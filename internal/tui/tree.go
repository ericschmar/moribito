package tui

import (
	"fmt"
	"strings"

	"github.com/charmbracelet/bubbles/viewport"
	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// TreeView represents the LDAP tree view
type TreeView struct {
	client        *ldap.Client
	root          *ldap.TreeNode
	flattenedTree []*TreeItem
	cursor        int
	width         int
	height        int
	viewport      viewport.Model
	loading       bool
}

// TreeItem represents a flattened tree item for display
type TreeItem struct {
	Node   *ldap.TreeNode
	Level  int
	IsLast bool
}

// NewTreeView creates a new tree view
func NewTreeView(client *ldap.Client) *TreeView {
	return &TreeView{
		client:   client,
		cursor:   0,
		viewport: viewport.New(0, 0),
	}
}

// Init initializes the tree view
func (tv *TreeView) Init() tea.Cmd {
	return tv.loadRootNode()
}

// SetSize sets the size of the tree view
func (tv *TreeView) SetSize(width, height int) {
	tv.width = width
	tv.height = height
	tv.viewport.Width = width
	tv.viewport.Height = height
}

// Update handles messages for the tree view
func (tv *TreeView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "up", "k":
			if tv.cursor > 0 {
				tv.cursor--
				tv.updateViewportForCursor()
			}
		case "down", "j":
			if tv.cursor < len(tv.flattenedTree)-1 {
				tv.cursor++
				tv.updateViewportForCursor()
			}
		case "page_up":
			tv.cursor -= tv.height
			if tv.cursor < 0 {
				tv.cursor = 0
			}
			tv.updateViewportForCursor()
		case "page_down":
			tv.cursor += tv.height
			if tv.cursor >= len(tv.flattenedTree) {
				tv.cursor = len(tv.flattenedTree) - 1
			}
			tv.updateViewportForCursor()
		case "home":
			tv.cursor = 0
			tv.updateViewportForCursor()
		case "end":
			tv.cursor = len(tv.flattenedTree) - 1
			tv.updateViewportForCursor()
		case "right", "l":
			return tv, tv.expandNode()
		case "left", "h":
			return tv, tv.collapseNode()
		case "enter":
			return tv, tv.viewRecord()
		default:
			// Let viewport handle other keys (like mouse wheel)
			tv.viewport, cmd = tv.viewport.Update(msg)
			return tv, cmd
		}

	case RootNodeLoadedMsg:
		tv.root = msg.Node
		tv.loading = false
		tv.rebuildFlattenedTree()
		tv.updateViewportContent()
		return tv, SendStatus("Tree loaded")

	case NodeChildrenLoadedMsg:
		tv.rebuildFlattenedTree()
		tv.loading = false
		tv.updateViewportContent()
		return tv, SendStatus(fmt.Sprintf("Loaded children for %s", msg.Node.Name))

	case tea.Msg:
		// Handle other message types
	}

	return tv, nil
}

// View renders the tree view
func (tv *TreeView) View() string {
	if tv.loading {
		return lipgloss.NewStyle().
			Width(tv.width).
			Height(tv.height).
			AlignHorizontal(lipgloss.Center).
			AlignVertical(lipgloss.Center).
			Render("Loading LDAP tree...")
	}

	if len(tv.flattenedTree) == 0 {
		return lipgloss.NewStyle().
			Width(tv.width).
			Height(tv.height).
			AlignHorizontal(lipgloss.Center).
			AlignVertical(lipgloss.Center).
			Render("No entries found")
	}

	return tv.viewport.View()
}

// renderTreeItem renders a single tree item
func (tv *TreeView) renderTreeItem(item *TreeItem, isCursor bool) string {
	indent := strings.Repeat("  ", item.Level)

	var prefix string
	if item.Node.Children != nil {
		if len(item.Node.Children) > 0 {
			prefix = "[-] "
		} else if item.Node.IsLoaded {
			prefix = "[·] "
		} else {
			prefix = "[+] "
		}
	} else {
		prefix = "[+] "
	}

	name := item.Node.Name
	if name == "" {
		name = item.Node.DN
	}

	content := indent + prefix + name

	style := lipgloss.NewStyle()
	if isCursor {
		style = style.Background(lipgloss.Color("240")).Foreground(lipgloss.Color("15"))
	}

	// Truncate if too long
	if tv.width > 5 && len(content) > tv.width-2 {
		content = content[:tv.width-5] + "..."
	}

	return style.Width(tv.width).Render(content)
}

// loadRootNode loads the root node of the tree
func (tv *TreeView) loadRootNode() tea.Cmd {
	tv.loading = true
	return func() tea.Msg {
		root, err := tv.client.BuildTree()
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return RootNodeLoadedMsg{Node: root}
	}
}

// expandNode expands the current node
func (tv *TreeView) expandNode() tea.Cmd {
	if tv.cursor >= len(tv.flattenedTree) {
		return nil
	}

	item := tv.flattenedTree[tv.cursor]
	node := item.Node

	if node.IsLoaded && len(node.Children) == 0 {
		return SendStatus("No children to expand")
	}

	if node.IsLoaded {
		return SendStatus("Node already expanded")
	}

	tv.loading = true
	return func() tea.Msg {
		err := tv.client.LoadChildren(node)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return NodeChildrenLoadedMsg{Node: node}
	}
}

// collapseNode collapses the current node
func (tv *TreeView) collapseNode() tea.Cmd {
	if tv.cursor >= len(tv.flattenedTree) {
		return nil
	}

	item := tv.flattenedTree[tv.cursor]
	node := item.Node

	if !node.IsLoaded || len(node.Children) == 0 {
		return SendStatus("No children to collapse")
	}

	// Mark as not loaded to collapse
	node.IsLoaded = false
	tv.rebuildFlattenedTree()

	return SendStatus("Node collapsed")
}

// viewRecord shows the record for the current node
func (tv *TreeView) viewRecord() tea.Cmd {
	if tv.cursor >= len(tv.flattenedTree) {
		return nil
	}

	item := tv.flattenedTree[tv.cursor]
	node := item.Node

	return func() tea.Msg {
		entry, err := tv.client.GetEntry(node.DN)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return ShowRecordMsg{Entry: entry}
	}
}

// rebuildFlattenedTree rebuilds the flattened tree for display
func (tv *TreeView) rebuildFlattenedTree() {
	tv.flattenedTree = nil
	if tv.root != nil {
		tv.flattenTreeNode(tv.root, 0, true)
	}
	tv.updateViewportContent()
}

// updateViewportContent updates the viewport with current tree content
func (tv *TreeView) updateViewportContent() {
	if len(tv.flattenedTree) == 0 {
		tv.viewport.SetContent("")
		return
	}

	var lines []string
	for i, item := range tv.flattenedTree {
		line := tv.renderTreeItem(item, i == tv.cursor)
		lines = append(lines, line)
	}

	content := strings.Join(lines, "\n")
	tv.viewport.SetContent(content)
}

// updateViewportForCursor updates viewport content and ensures cursor is visible
func (tv *TreeView) updateViewportForCursor() {
	tv.updateViewportContent()

	// Ensure cursor is visible by scrolling viewport if needed
	if tv.cursor < tv.viewport.YOffset {
		tv.viewport.YOffset = tv.cursor
	} else if tv.cursor >= tv.viewport.YOffset+tv.viewport.Height {
		tv.viewport.YOffset = tv.cursor - tv.viewport.Height + 1
	}

	if tv.viewport.YOffset < 0 {
		tv.viewport.YOffset = 0
	}
}

// flattenTreeNode recursively flattens the tree structure
func (tv *TreeView) flattenTreeNode(node *ldap.TreeNode, level int, isLast bool) {
	item := &TreeItem{
		Node:   node,
		Level:  level,
		IsLast: isLast,
	}
	tv.flattenedTree = append(tv.flattenedTree, item)

	if node.IsLoaded && node.Children != nil {
		for i, child := range node.Children {
			isLastChild := i == len(node.Children)-1
			tv.flattenTreeNode(child, level+1, isLastChild)
		}
	}
}

// Custom messages for tree view
type RootNodeLoadedMsg struct {
	Node *ldap.TreeNode
}

type NodeChildrenLoadedMsg struct {
	Node *ldap.TreeNode
}
