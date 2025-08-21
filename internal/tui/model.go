package tui

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/config"
	"github.com/ericschmar/ldap-cli/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

// ViewMode represents the current view mode
type ViewMode int

const (
	ViewModeStart ViewMode = iota
	ViewModeTree
	ViewModeRecord
	ViewModeQuery
)

// Model represents the main TUI model
type Model struct {
	client      *ldap.Client
	startView   *StartView
	tree        *TreeView
	recordView  *RecordView
	queryView   *QueryView
	currentView ViewMode
	width       int
	height      int
	err         error
	statusMsg   string
	quitting    bool
}

// NewModel creates a new TUI model
func NewModel(client *ldap.Client) *Model {
	cfg := config.Default() // Use default config
	return &Model{
		client:      client,
		startView:   NewStartView(cfg),
		tree:        NewTreeView(client),
		recordView:  NewRecordView(),
		queryView:   NewQueryView(client),
		currentView: ViewModeStart,
	}
}

// NewModelWithPageSize creates a new TUI model with specified page size for queries
func NewModelWithPageSize(client *ldap.Client, cfg *config.Config) *Model {
	var tree *TreeView
	var queryView *QueryView

	if client != nil {
		tree = NewTreeView(client)
		queryView = NewQueryViewWithPageSize(client, cfg.Pagination.PageSize)
	}

	return &Model{
		client:      client,
		startView:   NewStartView(cfg),
		tree:        tree,
		recordView:  NewRecordView(),
		queryView:   queryView,
		currentView: ViewModeStart,
	}
}

// Init initializes the model
func (m *Model) Init() tea.Cmd {
	// Initialize bubblezone global manager
	zone.NewGlobal()

	var cmds []tea.Cmd

	if m.tree != nil {
		cmds = append(cmds, m.tree.Init())
	}

	cmds = append(cmds, tea.EnterAltScreen)

	return tea.Batch(cmds...)
}

// Update handles messages
func (m *Model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmds []tea.Cmd

	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width = msg.Width
		m.height = msg.Height

		// Update child views - reserve space for tab bar (3 lines), status bar (1 line) and help bar (1 line)
		contentHeight := msg.Height - 5
		m.startView.SetSize(msg.Width, contentHeight)
		if m.tree != nil {
			m.tree.SetSize(msg.Width, contentHeight)
		}
		m.recordView.SetSize(msg.Width, contentHeight)
		if m.queryView != nil {
			m.queryView.SetSize(msg.Width, contentHeight)
		}

	case tea.KeyMsg:
		switch msg.String() {
		case "ctrl+c", "q":
			m.quitting = true
			return m, tea.Quit
		case "tab":
			return m.switchView(), nil
		case "0":
			m.currentView = ViewModeStart
			return m, nil
		case "1", "2", "3":
			// Skip global navigation keys if we're in query view input mode
			if m.currentView == ViewModeQuery && m.queryView != nil && m.queryView.IsInputMode() {
				break // Let the query view handle the input
			}
			// Handle navigation keys for view switching
			switch msg.String() {
			case "1":
				m.currentView = ViewModeTree
			case "2":
				m.currentView = ViewModeRecord
			case "3":

				m.currentView = ViewModeQuery
			}
			return m, nil
		}

	case tea.MouseMsg:
		// Handle mouse clicks through bubblezone - this will generate zone messages
		if msg.Type == tea.MouseLeft {
			zone.AnyInBounds(m, msg)
		}

	case zone.MsgZoneInBounds:
		// Need to figure out which zone was clicked by checking coordinates
		return m.handleZoneMessage(msg)

	case ErrorMsg:
		m.err = msg.Err
		return m, nil

	case StatusMsg:
		m.statusMsg = msg.Message
		return m, nil

	case ShowRecordMsg:
		m.recordView.SetEntry(msg.Entry)
		m.currentView = ViewModeRecord
		return m, nil

	// Handle tree-specific messages regardless of current view
	// This ensures tree loading works even when user switches away before completion
	case RootNodeLoadedMsg, NodeChildrenLoadedMsg:
		if m.tree != nil {
			newModel, cmd := m.tree.Update(msg)
			m.tree = newModel.(*TreeView)
			cmds = append(cmds, cmd)
		}
		return m, tea.Batch(cmds...)
	}

	// Forward messages to current view
	switch m.currentView {
	case ViewModeStart:
		newModel, cmd := m.startView.Update(msg)
		m.startView = newModel.(*StartView)
		cmds = append(cmds, cmd)

	case ViewModeTree:
		if m.tree != nil {
			newModel, cmd := m.tree.Update(msg)
			m.tree = newModel.(*TreeView)
			cmds = append(cmds, cmd)
		}

	case ViewModeRecord:
		newModel, cmd := m.recordView.Update(msg)
		m.recordView = newModel.(*RecordView)
		cmds = append(cmds, cmd)

	case ViewModeQuery:
		if m.queryView != nil {
			newModel, cmd := m.queryView.Update(msg)
			m.queryView = newModel.(*QueryView)
			cmds = append(cmds, cmd)
		}
	}

	return m, tea.Batch(cmds...)
}

// View renders the model
func (m *Model) View() string {
	if m.quitting {
		return "Goodbye!\n"
	}

	// Reset bubblezone for this frame
	zone.Clear("")

	// Tab bar at the top
	tabBar := m.renderTabBar()

	var content string

	switch m.currentView {
	case ViewModeStart:
		content = m.startView.View()
	case ViewModeTree:
		if m.tree != nil {
			content = m.tree.View()
		} else {
			content = "Tree view not available without LDAP connection"
		}
	case ViewModeRecord:
		content = m.recordView.View()
	case ViewModeQuery:
		if m.queryView != nil {
			content = m.queryView.View()
		} else {
			content = "Query view not available without LDAP connection"
		}
	}

	// Status bar
	status := m.renderStatusBar()

	// Help bar
	help := m.renderHelpBar()

	finalView := tabBar + "\n" + content + "\n" + status + "\n" + help

	// Scan the final view with bubblezone
	return zone.Scan(finalView)
}

func (m *Model) switchView() *Model {
	switch m.currentView {
	case ViewModeStart:
		if m.tree != nil {
			m.currentView = ViewModeTree
		} else {
			m.currentView = ViewModeRecord
		}
	case ViewModeTree:
		m.currentView = ViewModeRecord
	case ViewModeRecord:
		if m.queryView != nil {
			m.currentView = ViewModeQuery
		} else {
			m.currentView = ViewModeStart
		}
	case ViewModeQuery:
		m.currentView = ViewModeStart
	}
	return m
}

func (m *Model) renderStatusBar() string {
	// Define emojis and colors for each view
	viewInfo := map[ViewMode]struct {
		name  string
		emoji string
		color string
	}{
		ViewModeStart:  {"Start Page", "🏠", "99"},       // Purple
		ViewModeTree:   {"Tree Explorer", "🌳", "40"},    // Green
		ViewModeRecord: {"Record View", "📄", "33"},      // Blue
		ViewModeQuery:  {"Query Interface", "🔍", "208"}, // Orange
	}

	info := viewInfo[m.currentView]

	// Create the main view indicator with emoji and styling
	viewStyle := lipgloss.NewStyle().
		Background(lipgloss.Color(info.color)).
		Foreground(lipgloss.Color("15")).
		Bold(true).
		Padding(0, 1).
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color(info.color))

	viewIndicator := viewStyle.Render(fmt.Sprintf("%s %s", info.emoji, info.name))

	var statusParts []string
	statusParts = append(statusParts, viewIndicator)

	// Add status message with its own styling if present
	if m.statusMsg != "" {
		statusStyle := lipgloss.NewStyle().
			Background(lipgloss.Color("28")). // Green background
			Foreground(lipgloss.Color("15")).
			Bold(true).
			Padding(0, 1).
			Border(lipgloss.RoundedBorder()).
			BorderForeground(lipgloss.Color("28"))

		statusParts = append(statusParts, statusStyle.Render("ℹ️  "+m.statusMsg))
	}

	// Add error with dramatic styling if present
	if m.err != nil {
		errorStyle := lipgloss.NewStyle().
			Background(lipgloss.Color("196")). // Red background
			Foreground(lipgloss.Color("15")).
			Bold(true).
			Italic(true).
			Padding(0, 1).
			Border(lipgloss.RoundedBorder()).
			BorderForeground(lipgloss.Color("196"))

		statusParts = append(statusParts, errorStyle.Render("⚠️  "+m.err.Error()))
	}

	// Join all parts with some spacing
	statusContent := lipgloss.JoinHorizontal(lipgloss.Center, statusParts...)

	// Create the container with a stylish background and border
	containerStyle := lipgloss.NewStyle().
		Width(m.width).
		Padding(0, 2).
		Background(lipgloss.Color("235")). // Dark gray background
		Border(lipgloss.NormalBorder()).
		BorderTop(true).
		BorderForeground(lipgloss.Color("241")). // Lighter gray border
		Align(lipgloss.Left)

	return containerStyle.Render(statusContent)
}

func (m *Model) renderTabBar() string {
	// Define fun colors for the tabs
	activeColors := map[ViewMode]string{
		ViewModeStart:  "99",  // Purple
		ViewModeTree:   "40",  // Green
		ViewModeRecord: "33",  // Blue
		ViewModeQuery:  "208", // Orange
	}

	// Tab names
	tabs := map[ViewMode]string{
		ViewModeStart:  "🏠 Start",
		ViewModeTree:   "🌳 Tree",
		ViewModeRecord: "📄 Record",
		ViewModeQuery:  "🔍 Query",
	}

	var tabButtons []string

	// Create each tab button
	for _, viewMode := range []ViewMode{ViewModeStart, ViewModeTree, ViewModeRecord, ViewModeQuery} {
		isActive := m.currentView == viewMode
		tabName := tabs[viewMode]
		isAvailable := true

		// Check availability
		if viewMode == ViewModeTree && m.tree == nil {
			isAvailable = false
			tabName = "🌳 Tree (N/A)"
		}
		if viewMode == ViewModeQuery && m.queryView == nil {
			isAvailable = false
			tabName = "🔍 Query (N/A)"
		}

		var style lipgloss.Style
		if isActive {
			// Active tab: bright colors, bold, underlined
			style = lipgloss.NewStyle().
				Background(lipgloss.Color(activeColors[viewMode])).
				Foreground(lipgloss.Color("15")).
				Bold(true).
				Underline(true).
				Padding(0, 2).
				Border(lipgloss.RoundedBorder()).
				BorderForeground(lipgloss.Color(activeColors[viewMode])).
				BorderTop(true).
				BorderBottom(false).
				BorderLeft(true).
				BorderRight(true)
		} else if !isAvailable {
			// Unavailable tab: grayed out
			style = lipgloss.NewStyle().
				Background(lipgloss.Color("233")).
				Foreground(lipgloss.Color("240")).
				Padding(0, 2).
				Border(lipgloss.RoundedBorder()).
				BorderForeground(lipgloss.Color("237"))
		} else {
			// Inactive but available tab: muted colors
			style = lipgloss.NewStyle().
				Background(lipgloss.Color("236")).
				Foreground(lipgloss.Color("252")).
				Padding(0, 2).
				Border(lipgloss.RoundedBorder()).
				BorderForeground(lipgloss.Color("240"))
		}

		renderedTab := style.Render(tabName)

		// Wrap tab with clickable zone if available
		if isAvailable {
			var zoneID string
			switch viewMode {
			case ViewModeStart:
				zoneID = "tab-start"
			case ViewModeTree:
				zoneID = "tab-tree"
			case ViewModeRecord:
				zoneID = "tab-record"
			case ViewModeQuery:
				zoneID = "tab-query"
			}
			renderedTab = zone.Mark(zoneID, renderedTab)
		}

		tabButtons = append(tabButtons, renderedTab)
	}

	// Join tabs with some spacing
	tabBar := lipgloss.JoinHorizontal(lipgloss.Top, tabButtons...)

	// Create the container for the tab bar with a fun gradient background
	tabContainer := lipgloss.NewStyle().
		Width(m.width).
		Padding(1, 2).
		Background(lipgloss.Color("235")).
		Border(lipgloss.NormalBorder()).
		BorderBottom(true).
		BorderForeground(lipgloss.Color("99")).
		Render(tabBar)

	return tabContainer
}

func (m *Model) renderHelpBar() string {
	help := "Keys: [q]uit • [tab] switch view • [0]start [1]tree [2]record [3]query"

	switch m.currentView {
	case ViewModeStart:
		help += " • [↑↓] navigate • [enter] edit • [esc] cancel"
	case ViewModeTree:
		help += " • [↑↓] navigate • [→] expand • [←] collapse • [enter] view record"
	case ViewModeRecord:
		help += " • [↑↓] scroll • [c] copy value"
	case ViewModeQuery:
		help += " • [enter] execute query • [esc] clear"
	}

	return lipgloss.NewStyle().
		Foreground(lipgloss.Color("241")).
		Width(m.width).
		Padding(0, 1).
		Render(help)
}

// handleZoneMessage handles zone click messages
func (m *Model) handleZoneMessage(msg zone.MsgZoneInBounds) (tea.Model, tea.Cmd) {
	// Check each possible zone to see which one was clicked
	tabZones := []string{"tab-start", "tab-tree", "tab-record", "tab-query"}

	for _, zoneID := range tabZones {
		if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
			return m.handleZoneClick(zoneID)
		}
	}

	// Check for start view config field zones
	if m.currentView == ViewModeStart {
		for i := 0; i < 8; i++ { // FieldCount is 8
			zoneID := fmt.Sprintf("config-field-%d", i)
			if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
				return m.handleStartViewClick(zoneID)
			}
		}
	}

	// Check for tree view item zones
	if m.currentView == ViewModeTree && m.tree != nil {
		for i := 0; i < len(m.tree.FlattenedTree); i++ {
			zoneID := fmt.Sprintf("tree-item-%d", i)
			if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
				return m.handleTreeViewClick(zoneID)
			}
		}
	}

	// Check for query view result zones
	if m.currentView == ViewModeQuery && m.queryView != nil {
		for i := 0; i < len(m.queryView.ResultLines); i++ {
			zoneID := fmt.Sprintf("query-result-%d", i)
			if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
				return m.handleQueryViewClick(zoneID)
			}
		}
	}

	// If no zones matched, let the current view handle it
	return m, nil
}
func (m *Model) handleZoneClick(zoneID string) (tea.Model, tea.Cmd) {
	switch zoneID {
	case "tab-start":
		m.currentView = ViewModeStart
		return m, nil
	case "tab-tree":
		if m.tree != nil {
			m.currentView = ViewModeTree
		}
		return m, nil
	case "tab-record":
		m.currentView = ViewModeRecord
		return m, nil
	case "tab-query":
		if m.queryView != nil {
			m.currentView = ViewModeQuery
		}
		return m, nil
	default:
		// Handle view-specific zone clicks by forwarding to current view
		switch m.currentView {
		case ViewModeStart:
			return m.handleStartViewClick(zoneID)
		case ViewModeTree:
			return m.handleTreeViewClick(zoneID)
		case ViewModeRecord:
			return m.handleRecordViewClick(zoneID)
		case ViewModeQuery:
			return m.handleQueryViewClick(zoneID)
		}
	}
	return m, nil
}

// handleStartViewClick handles clicks specific to start view
func (m *Model) handleStartViewClick(zoneID string) (tea.Model, tea.Cmd) {
	// Handle config field clicks
	if strings.HasPrefix(zoneID, "config-field-") {
		// Extract field number
		fieldStr := strings.TrimPrefix(zoneID, "config-field-")
		if fieldNum, err := strconv.Atoi(fieldStr); err == nil {
			// Simulate clicking on this field by setting cursor and entering edit mode
			m.startView.cursor = fieldNum
			m.startView.editing = true
			m.startView.editingField = fieldNum
			m.startView.inputValue = m.startView.getFieldValue(fieldNum)
			return m, nil
		}
	}
	return m, nil
}

// handleTreeViewClick handles clicks specific to tree view
func (m *Model) handleTreeViewClick(zoneID string) (tea.Model, tea.Cmd) {
	if m.tree == nil {
		return m, nil
	}

	// Handle tree item clicks
	if strings.HasPrefix(zoneID, "tree-item-") {
		// Extract item index
		itemStr := strings.TrimPrefix(zoneID, "tree-item-")
		if itemIndex, err := strconv.Atoi(itemStr); err == nil {
			// Set cursor to this item
			if itemIndex >= 0 && itemIndex < len(m.tree.FlattenedTree) {
				m.tree.cursor = itemIndex
				m.tree.adjustViewport()
				// Simulate Enter key press to expand/view
				return m.tree.Update(tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'\r'}})
			}
		}
	}
	return m, nil
}

// handleRecordViewClick handles clicks specific to record view
func (m *Model) handleRecordViewClick(zoneID string) (tea.Model, tea.Cmd) {
	// Handle attribute row clicks - will be implemented when we add zones to RecordView
	return m, nil
}

// handleQueryViewClick handles clicks specific to query view
func (m *Model) handleQueryViewClick(zoneID string) (tea.Model, tea.Cmd) {
	if m.queryView == nil {
		return m, nil
	}

	// Handle query result clicks
	if strings.HasPrefix(zoneID, "query-result-") {
		// Extract result index
		resultStr := strings.TrimPrefix(zoneID, "query-result-")
		if resultIndex, err := strconv.Atoi(resultStr); err == nil {
			// Set cursor to this result
			if resultIndex >= 0 && resultIndex < len(m.queryView.ResultLines) {
				m.queryView.cursor = resultIndex
				m.queryView.adjustViewport()
				// Simulate Enter key press to view the selected record
				return m.queryView.Update(tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'\r'}})
			}
		}
	}
	return m, nil
}

// Custom messages
type ErrorMsg struct {
	Err error
}

type StatusMsg struct {
	Message string
}

type ShowRecordMsg struct {
	Entry *ldap.Entry
}

// Helper functions for sending messages
func SendError(err error) tea.Cmd {
	return func() tea.Msg {
		return ErrorMsg{Err: err}
	}
}

func SendStatus(msg string) tea.Cmd {
	return func() tea.Msg {
		return StatusMsg{Message: msg}
	}
}

func ShowRecord(entry *ldap.Entry) tea.Cmd {
	return func() tea.Msg {
		return ShowRecordMsg{Entry: entry}
	}
}
