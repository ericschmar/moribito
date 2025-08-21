package tui

import (
	"fmt"
	"strings"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	zone "github.com/lrstanley/bubblezone"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// TreeView represents the LDAP tree view
type TreeView struct {
	client        *ldap.Client
	root          *ldap.TreeNode
	FlattenedTree []*TreeItem
	cursor        int
	viewport      int
	width         int
	height        int
	loading       bool
	container     *ViewContainer
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
		viewport: 0,
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
	tv.container = NewViewContainer(width, height)
}

// Update handles messages for the tree view
func (tv *TreeView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "up", "k":
			if tv.cursor > 0 {
				tv.cursor--
				tv.adjustViewport()
			}
		case "down", "j":
			if tv.cursor < len(tv.FlattenedTree)-1 {
				tv.cursor++
				tv.adjustViewport()
			}
		case "page_up":
			_, contentHeight := tv.container.GetContentDimensions()
			if tv.container == nil {
				contentHeight = tv.height
			}
			tv.cursor -= contentHeight
			if tv.cursor < 0 {
				tv.cursor = 0
			}
			tv.adjustViewport()
		case "page_down":
			_, contentHeight := tv.container.GetContentDimensions()
			if tv.container == nil {
				contentHeight = tv.height
			}
			tv.cursor += contentHeight
			if tv.cursor >= len(tv.FlattenedTree) {
				tv.cursor = len(tv.FlattenedTree) - 1
			}
			tv.adjustViewport()
		case "home":
			tv.cursor = 0
			tv.adjustViewport()
		case "end":
			tv.cursor = len(tv.FlattenedTree) - 1
			tv.adjustViewport()
		case "right", "l":
			return tv, tv.expandNode()
		case "left", "h":
			return tv, tv.collapseNode()
		case "enter":
			return tv, tv.viewRecord()
		}

	case RootNodeLoadedMsg:
		tv.root = msg.Node
		tv.loading = false
		tv.rebuildFlattenedTree()
		return tv, SendStatus("Tree loaded")

	case NodeChildrenLoadedMsg:
		tv.rebuildFlattenedTree()
		tv.loading = false
		return tv, SendStatus(fmt.Sprintf("Loaded children for %s", msg.Node.Name))

	case tea.Msg:
		// Handle other message types
	}

	return tv, nil
}

// View renders the tree view
func (tv *TreeView) View() string {
	if tv.container == nil {
		tv.container = NewViewContainer(tv.width, tv.height)
	}

	if tv.loading {
		return tv.container.RenderCentered("Loading LDAP tree...")
	}

	if len(tv.FlattenedTree) == 0 {
		return lipgloss.NewStyle().
			Width(tv.width).
			Height(tv.height).
			AlignHorizontal(lipgloss.Center).
			AlignVertical(lipgloss.Center).
			Render("No entries found")
	}

	// Get content dimensions
	contentWidth, contentHeight := tv.container.GetContentDimensions()

	var lines []string
	visibleStart := tv.viewport
	visibleEnd := visibleStart + contentHeight
	if visibleEnd > len(tv.FlattenedTree) {
		visibleEnd = len(tv.FlattenedTree)
	}

	for i := visibleStart; i < visibleEnd; i++ {
		item := tv.FlattenedTree[i]
		line := tv.renderTreeItem(item, i == tv.cursor, contentWidth)
		
		// Wrap with clickable zone
		zoneID := fmt.Sprintf("tree-item-%d", i)
		line = zone.Mark(zoneID, line)
		
		lines = append(lines, line)
	}

	// Fill remaining space
	for len(lines) < contentHeight {
		lines = append(lines, "")
	}

	content := strings.Join(lines, "\n")
	return tv.container.RenderWithPadding(content)
}

// renderTreeItem renders a single tree item
func (tv *TreeView) renderTreeItem(item *TreeItem, isCursor bool, contentWidth int) string {
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
	if contentWidth > 5 && len(content) > contentWidth-2 {
		content = content[:contentWidth-5] + "..."
	}

	return style.Width(contentWidth).Render(content)
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
	if tv.cursor >= len(tv.FlattenedTree) {
		return nil
	}

	item := tv.FlattenedTree[tv.cursor]
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
	if tv.cursor >= len(tv.FlattenedTree) {
		return nil
	}

	item := tv.FlattenedTree[tv.cursor]
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
	if tv.cursor >= len(tv.FlattenedTree) {
		return nil
	}

	item := tv.FlattenedTree[tv.cursor]
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
	tv.FlattenedTree = nil
	if tv.root != nil {
		tv.flattenTreeNode(tv.root, 0, true)
	}
}

// adjustViewport adjusts the viewport to keep the cursor visible
func (tv *TreeView) adjustViewport() {
	// Use content height for viewport calculations
	_, contentHeight := tv.container.GetContentDimensions()
	if tv.container == nil {
		contentHeight = tv.height
	}

	if tv.cursor < tv.viewport {
		tv.viewport = tv.cursor
	} else if tv.cursor >= tv.viewport+contentHeight {
		tv.viewport = tv.cursor - contentHeight + 1
	}

	if tv.viewport < 0 {
		tv.viewport = 0
	}
}

// flattenTreeNode recursively flattens the tree structure
func (tv *TreeView) flattenTreeNode(node *ldap.TreeNode, level int, isLast bool) {
	item := &TreeItem{
		Node:   node,
		Level:  level,
		IsLast: isLast,
	}
	tv.FlattenedTree = append(tv.FlattenedTree, item)

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
