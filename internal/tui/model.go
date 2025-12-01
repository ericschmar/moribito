package tui

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
	"github.com/ericschmar/moribito/internal/updater"
	"github.com/ericschmar/moribito/internal/version"
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

// Update-related message types
type (
	updateCheckMsg struct {
		available bool
		version   string
		url       string
		err       error
	}

	// ConnectMsg is sent when the start view successfully connects to LDAP
	ConnectMsg struct {
		Client *ldap.Client
		Config *config.Config
	}
)

// checkForUpdatesCmd creates a command to check for updates
func checkForUpdatesCmd() tea.Cmd {
	return func() tea.Msg {
		// Add a small delay to allow UI to initialize first
		// This prevents blocking the startup while still running the check in background
		time.Sleep(1500 * time.Millisecond)

		checker := updater.New("ericschmar", "moribito")
		ctx := context.Background()

		currentVersion := version.Get().Version
		release, err := checker.CheckForUpdate(ctx, currentVersion)

		if err != nil {
			return updateCheckMsg{err: err}
		}

		if release != nil {
			return updateCheckMsg{
				available: true,
				version:   release.TagName,
				url:       release.URL,
			}
		}

		return updateCheckMsg{available: false}
	}
}

// Model represents the main TUI model
type Model struct {
	client       *ldap.Client
	startView    *StartView
	tree         *TreeView
	recordView   *RecordView
	queryView    *QueryView
	currentView  ViewMode
	width        int
	height       int
	err          error
	statusMsg    string
	quitting     bool
	checkUpdates bool
	updateStatus string
}

// NewModel creates a new model
func NewModel(client *ldap.Client, cfg *config.Config) *Model {
	model := &Model{
		client:      client,
		startView:   NewStartView(cfg),
		recordView:  NewRecordView(),
		currentView: ViewModeStart,
	}

	// Initialize tree and query views if client is available
	if client != nil {
		model.tree = NewTreeView(client)
		model.queryView = NewQueryViewWithPageSize(client, cfg.Pagination.PageSize)
	}

	return model
}

// NewModelWithPageSize creates a new model with page size configuration
func NewModelWithPageSize(client *ldap.Client, cfg *config.Config) *Model {
	return NewModelWithUpdateCheck(client, cfg, false)
}

// NewModelWithUpdateCheck creates a new model with page size configuration and update checking
func NewModelWithUpdateCheck(client *ldap.Client, cfg *config.Config, checkUpdates bool) *Model {
	model := &Model{
		client:       client,
		startView:    NewStartView(cfg),
		recordView:   NewRecordView(),
		currentView:  ViewModeStart,
		checkUpdates: checkUpdates,
	}

	// Initialize tree and query views if client is available
	if client != nil {
		model.tree = NewTreeView(client)
		model.queryView = NewQueryViewWithPageSize(client, cfg.Pagination.PageSize)
	}

	return model
}

// NewModelWithUpdateCheckAndConfigPath creates a new model with page size configuration, update checking, and config path
func NewModelWithUpdateCheckAndConfigPath(client *ldap.Client, cfg *config.Config, checkUpdates bool, configPath string) *Model {
	model := &Model{
		client:       client,
		startView:    NewStartViewWithConfigPath(cfg, configPath),
		recordView:   NewRecordView(),
		currentView:  ViewModeStart,
		checkUpdates: checkUpdates,
	}

	// Initialize tree and query views if client is available
	if client != nil {
		model.tree = NewTreeView(client)
		model.queryView = NewQueryViewWithPageSize(client, cfg.Pagination.PageSize)
	}

	return model
}

// Init initializes the model
func (m *Model) Init() tea.Cmd {
	// Initialize bubblezone manager to prevent panics
	zone.NewGlobal()

	var cmds []tea.Cmd

	// Initialize child views
	if cmd := m.startView.Init(); cmd != nil {
		cmds = append(cmds, cmd)
	}

	if m.tree != nil {
		if cmd := m.tree.Init(); cmd != nil {
			cmds = append(cmds, cmd)
		}
	}

	if cmd := m.recordView.Init(); cmd != nil {
		cmds = append(cmds, cmd)
	}

	if m.queryView != nil {
		if cmd := m.queryView.Init(); cmd != nil {
			cmds = append(cmds, cmd)
		}
	}

	// Start update checking if enabled
	if m.checkUpdates {
		cmds = append(cmds, checkForUpdatesCmd())
	}

	return tea.Batch(cmds...)
}

func (m *Model) SetSize(width, height int) {
	m.width = width
	m.height = height

	// Calculate content height (reserve space for tab bar, status bar, and help bar)
	// Tab bar: 3 lines, Status bar: 1 line, Help bar: 1 line = 5 lines total
	contentHeight := height - 5
	if contentHeight < 1 {
		contentHeight = 1
	}

	// Update all view sizes with content height (not full height)
	m.startView.SetSize(width, contentHeight)
	if m.tree != nil {
		m.tree.SetSize(width, contentHeight)
	}
	m.recordView.SetSize(width, contentHeight)
	if m.queryView != nil {
		m.queryView.SetSize(width, contentHeight)
	}
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
		case "ctrl+c":
			m.quitting = true
			return m, tea.Quit
		case "q":
			// Skip global quit key if we're in an input mode
			if m.currentView == ViewModeQuery && m.queryView != nil && m.queryView.IsInputMode() {
				break // Let the query view handle the input
			}
			if m.currentView == ViewModeStart && m.startView != nil && m.startView.IsEditing() {
				break // Let the start view handle the input
			}
			m.quitting = true
			return m, tea.Quit
		case "tab":
			return m.switchView(), nil
		case "1", "2", "3", "4":
			// Skip global navigation keys if we're in an input mode
			if m.currentView == ViewModeQuery && m.queryView != nil && m.queryView.IsInputMode() {
				break // Let the query view handle the input
			}
			if m.currentView == ViewModeStart && m.startView != nil && m.startView.IsEditing() {
				break // Let the start view handle the input
			}
			// Handle navigation keys for view switching
			switch msg.String() {
			case "1":
				m.currentView = ViewModeStart
			case "2":
				m.currentView = ViewModeTree
			case "3":
				m.currentView = ViewModeRecord
			case "4":
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
		// Handle zone clicks by examining the zone info and event
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

	case updateCheckMsg:
		if msg.err != nil {
			// Silently ignore update check errors - don't disturb user experience
			return m, nil
		}
		if msg.available {
			m.updateStatus = fmt.Sprintf("🔄 Update available: %s", msg.version)
		} else {
			m.updateStatus = ""
		}
		return m, nil

	case ConnectMsg:
		// Handle successful LDAP connection from start view
		if m.client != nil {
			m.client.Close() // Close existing connection if any
		}

		m.client = msg.Client

		// Initialize tree and query views with new client
		m.tree = NewTreeView(msg.Client)
		m.queryView = NewQueryViewWithPageSize(msg.Client, msg.Config.Pagination.PageSize)

		// Set sizes for the new views (reserve space for tab bar, status bar, and help bar)
		contentHeight := m.height - 5
		m.tree.SetSize(m.width, contentHeight)
		m.queryView.SetSize(m.width, contentHeight)

		// Switch to tree view
		m.currentView = ViewModeTree
		m.statusMsg = "Successfully connected to LDAP server"

		// Initialize the tree view to start loading the tree
		treeInitCmd := m.tree.Init()

		return m, treeInitCmd

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

// View renders the main view
func (m *Model) View() string {
	if m.quitting {
		return "Goodbye!\n"
	}

	// Check for minimum terminal size
	minWidth := 60
	minHeight := 15
	if m.width < minWidth || m.height < minHeight {
		warningStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Bold(true).
			Align(lipgloss.Center).
			Width(m.width)

		message := fmt.Sprintf(
			"Terminal too small!\n\n"+
				"Current: %dx%d\n"+
				"Minimum: %dx%d\n\n"+
				"Please resize your terminal.",
			m.width, m.height, minWidth, minHeight)

		return zone.Scan(warningStyle.Render(message))
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

	// CRITICAL: Strictly enforce height limits to prevent UI chrome from being pushed off screen
	// Reserve: Tab bar (3 lines) + Status bar (1 line) + Help bar (1 line) = 5 lines
	// Content gets the remainder
	contentMaxLines := m.height - 5
	if contentMaxLines < 1 {
		contentMaxLines = 1
	}

	// Enforce content height limit
	contentLines := strings.Split(content, "\n")
	if len(contentLines) > contentMaxLines {
		contentLines = contentLines[:contentMaxLines]
		// Add truncation indicator
		if contentMaxLines > 0 {
			contentLines[contentMaxLines-1] = lipgloss.NewStyle().
				Foreground(lipgloss.Color("8")).
				Render("... (content truncated, resize terminal)")
		}
	}
	content = strings.Join(contentLines, "\n")

	// Build the layout with strictly controlled heights
	// Tab bar (3 lines) + content (contentMaxLines) + status (1 line) + help (1 line)
	// Note: tabBar already ends with "\n", so don't add extra newline
	mainContent := tabBar + content + "\n" + status

	// Calculate how much vertical space we have and position help bar at the bottom
	mainContentLines := strings.Split(mainContent, "\n")
	totalContentHeight := len(mainContentLines)

	var finalView string
	// If we have room, add padding to push help bar to bottom
	if totalContentHeight < m.height-1 { // -1 for help bar itself
		paddingNeeded := m.height - totalContentHeight - 1
		if paddingNeeded > 0 {
			padding := strings.Repeat("\n", paddingNeeded)
			finalView = mainContent + padding + help
		} else {
			finalView = mainContent + "\n" + help
		}
	} else {
		// Should not happen with our strict enforcement, but handle gracefully
		finalView = mainContent + "\n" + help
	}

	// Scan the final view with bubblezone
	return zone.Scan(finalView)
}

// switchView switches to the next view
func (m *Model) switchView() *Model {
	switch m.currentView {
	case ViewModeStart:
		if m.client != nil {
			m.currentView = ViewModeTree
		} else {
			m.currentView = ViewModeRecord
		}
	case ViewModeTree:
		m.currentView = ViewModeRecord
	case ViewModeRecord:
		if m.client != nil {
			m.currentView = ViewModeQuery
		} else {
			m.currentView = ViewModeStart
		}
	case ViewModeQuery:
		m.currentView = ViewModeStart
	}
	return m
}

// renderStatusBar creates the status bar
func (m *Model) renderStatusBar() string {
	// Create right side with connection status
	var rightContent string
	if m.client != nil {
		connStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("0")).
			Background(lipgloss.Color("10")).
			Bold(true).
			Padding(0, 1)
		rightContent = connStyle.Render("🔗 Connected")
	} else {
		connStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("0")).
			Background(lipgloss.Color("9")).
			Bold(true).
			Padding(0, 1)
		rightContent = connStyle.Render("❌ Disconnected")
	}

	// Create status message in the middle - prioritize update notifications
	var statusContent string
	if m.updateStatus != "" {
		// Show update notification with special styling
		updateStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("0")).
			Background(lipgloss.Color("11")). // Bright yellow background
			Bold(true).
			Padding(0, 1)
		statusContent = updateStyle.Render(m.updateStatus)
	} else if m.statusMsg != "" {
		statusStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("15")).
			Padding(0, 1)
		statusContent = statusStyle.Render(m.statusMsg)
	}

	// Calculate spacing for center alignment
	totalWidth := m.width
	rightWidth := lipgloss.Width(rightContent)
	statusWidth := lipgloss.Width(statusContent)

	remainingWidth := totalWidth - rightWidth
	if remainingWidth < 0 {
		remainingWidth = 0
	}

	var middleContent string
	if statusWidth > 0 && remainingWidth >= statusWidth {
		paddingNeeded := remainingWidth - statusWidth
		leftPadding := paddingNeeded / 2
		middleContent = strings.Repeat(" ", leftPadding) + statusContent
	} else {
		middleContent = strings.Repeat(" ", remainingWidth)
	}

	return middleContent + rightContent
}

// renderTabBar creates the tab navigation bar
func (m *Model) renderTabBar() string {
	tabs := []struct {
		name     string
		emoji    string
		key      string
		viewMode ViewMode
		enabled  bool
		color    string
	}{
		{"Start", "🏠", "1", ViewModeStart, true, "12"},
		{"Tree", "🌲", "2", ViewModeTree, m.client != nil, "10"},
		{"Record", "📄", "3", ViewModeRecord, true, "11"},
		{"Query", "🔍", "4", ViewModeQuery, m.client != nil, "13"},
	}

	var tabButtons []string
	for _, tab := range tabs {
		var style lipgloss.Style

		if tab.viewMode == m.currentView {
			// Active tab style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color("0")).
				Background(lipgloss.Color(tab.color)).
				Bold(true).
				Padding(0, 2).
				Border(lipgloss.ThickBorder(), false, false, true, false).
				BorderForeground(lipgloss.Color("12"))
		} else if tab.enabled {
			// Available tab style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color(tab.color)).
				Background(lipgloss.Color("8")).
				Padding(0, 2).
				Border(lipgloss.ThickBorder(), false, false, true, false).
				BorderForeground(lipgloss.Color("8"))
		} else {
			// Disabled tab style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color("8")).
				Background(lipgloss.Color("0")).
				Padding(0, 2).
				Border(lipgloss.ThickBorder(), false, false, true, false).
				BorderForeground(lipgloss.Color("8"))
		}

		tabText := fmt.Sprintf("[%s] %s %s", tab.key, tab.emoji, tab.name)
		renderedTab := style.Render(tabText)

		// Add clickable zone for enabled tabs
		if tab.enabled {
			zoneID := fmt.Sprintf("tab-%s", tab.key)
			renderedTab = zone.Mark(zoneID, renderedTab)
		}

		tabButtons = append(tabButtons, renderedTab)
	}

	// Join tabs with small spacing
	tabRow := lipgloss.JoinHorizontal(lipgloss.Top, tabButtons...)

	// Add some spacing and instructions
	instructions := lipgloss.NewStyle().
		Foreground(lipgloss.Color("8")).
		Italic(true).
		Padding(0, 1).
		Render("Use [Tab] to cycle views • [Ctrl+C] or [Q] to quit")

	return tabRow + "\n" + instructions + "\n"
}

// renderHelpBar creates the help bar at the bottom
func (m *Model) renderHelpBar() string {
	var helpText string

	switch m.currentView {
	case ViewModeStart:
		helpText = "Configure LDAP settings • [↑↓] navigate • [Enter] edit"
	case ViewModeTree:
		if m.tree != nil {
			helpText = "Browse LDAP tree • [↑↓] navigate • [Enter] expand • [Space] view record"
		} else {
			helpText = "Tree view requires LDAP connection"
		}
	case ViewModeRecord:
		helpText = "View LDAP record details • [↑↓] navigate attributes • [C] copy value"
	}

	style := lipgloss.NewStyle().
		Foreground(lipgloss.Color("8")).
		Background(lipgloss.Color("0")).
		Padding(0, 1).
		Width(m.width)

	return style.Render(helpText)
}

// handleZoneMessage handles bubblezone click messages
func (m *Model) handleZoneMessage(msg zone.MsgZoneInBounds) (tea.Model, tea.Cmd) {
	// Check if this is a tab click by checking each tab zone
	tabKeys := []string{"1", "2", "3", "4"}
	viewModes := []ViewMode{ViewModeStart, ViewModeTree, ViewModeRecord, ViewModeQuery}

	for i, key := range tabKeys {
		zoneID := fmt.Sprintf("tab-%s", key)
		if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
			// Navigate to the clicked tab
			m.currentView = viewModes[i]
			return m, nil
		}
	}

	// Forward to current view's zone handler based on view mode
	switch m.currentView {
	case ViewModeStart:
		// Check for config field clicks
		for i := 0; i < 10; i++ { // reasonable upper bound for config fields
			zoneID := fmt.Sprintf("config-field-%d", i)
			if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
				return m.handleStartViewClick(zoneID)
			}
		}

	case ViewModeTree:
		if m.tree != nil {
			// Check for tree item clicks
			for i := 0; i < len(m.tree.FlattenedTree); i++ {
				zoneID := fmt.Sprintf("tree-item-%d", i)
				if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
					return m.handleTreeViewClick(zoneID)
				}
			}
		}

	case ViewModeRecord:
		// Check for record row clicks
		for i := 0; i < 100; i++ { // reasonable upper bound for attributes
			zoneID := fmt.Sprintf("record-row-%d", i)
			if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
				return m.handleRecordViewClick(zoneID)
			}
		}

	case ViewModeQuery:
		if m.queryView != nil {
			// Check for query result clicks
			for i := 0; i < 1000; i++ { // reasonable upper bound for query results
				zoneID := fmt.Sprintf("query-result-%d", i)
				if zoneInfo := zone.Get(zoneID); zoneInfo != nil && zoneInfo.InBounds(msg.Event) {
					return m.handleQueryViewClick(zoneID)
				}
			}
		}
	}

	return m, nil
}

// handleZoneClick is a legacy method that forwards to handleZoneMessage
func (m *Model) handleZoneClick(zoneID string) (tea.Model, tea.Cmd) {
	// This method can be removed if not used elsewhere
	return m, nil
}

// handleStartViewClick handles clicks in the start view
func (m *Model) handleStartViewClick(zoneID string) (tea.Model, tea.Cmd) {
	// Handle config field clicks
	if len(zoneID) > 13 && zoneID[:13] == "config-field-" {
		if fieldIndex, err := strconv.Atoi(zoneID[13:]); err == nil {
			// Set cursor to clicked field
			m.startView.cursor = fieldIndex

			// Use the field action handler to properly initialize editing
			// This will handle textinput initialization for regular fields
			// and boolean toggles appropriately
			return m.startView.handleFieldAction()
		}
	}
	return m, nil
}

// handleTreeViewClick handles clicks in the tree view
func (m *Model) handleTreeViewClick(zoneID string) (tea.Model, tea.Cmd) {
	if m.tree == nil {
		return m, nil
	}

	// Handle tree item clicks - match the zone ID format used in tree.go
	if len(zoneID) > 10 && zoneID[:10] == "tree-item-" {
		if nodeIndex, err := strconv.Atoi(zoneID[10:]); err == nil && nodeIndex < len(m.tree.FlattenedTree) {
			m.tree.cursor = nodeIndex
			m.tree.adjustViewport() // Ensure the cursor is visible
			// Simulate enter key press to expand/view node
			newTreeModel, cmd := m.tree.Update(tea.KeyMsg{Type: tea.KeyEnter})
			m.tree = newTreeModel.(*TreeView) // Update the tree in the model
			return m, cmd
		}
	}
	return m, nil
}

// handleRecordViewClick handles clicks in the record view
func (m *Model) handleRecordViewClick(zoneID string) (tea.Model, tea.Cmd) {
	// Handle record row clicks - match the zone ID format used in record.go
	if len(zoneID) > 11 && zoneID[:11] == "record-row-" {
		if attrIndex, err := strconv.Atoi(zoneID[11:]); err == nil {
			// Set table cursor to clicked attribute
			if attrIndex < len(m.recordView.renderedRows) {
				m.recordView.table.SetCursor(attrIndex)
			}
		}
	}
	return m, nil
}

// handleQueryViewClick handles clicks in the query view
func (m *Model) handleQueryViewClick(zoneID string) (tea.Model, tea.Cmd) {
	if m.queryView == nil {
		return m, nil
	}

	// Handle result line clicks - now using table cursor
	if len(zoneID) > 13 && zoneID[:13] == "query-result-" {
		if lineIndex, err := strconv.Atoi(zoneID[13:]); err == nil {
			// Since we're using a table now, we need to set the table cursor
			// The lineIndex should correspond to the row index
			if lineIndex < len(m.queryView.results) {
				// Set the table cursor to the clicked row
				// Note: We can't directly set table cursor, but we can trigger selection
				// For now, let's just show the record directly
				return m, ShowRecord(m.queryView.results[lineIndex])
			}
		}
	}
	return m, nil
}

// ErrorMsg represents an error message
type ErrorMsg struct {
	Err error
}

// StatusMsg represents a status message
type StatusMsg struct {
	Message string
}

// ShowRecordMsg represents a message to show a record
type ShowRecordMsg struct {
	Entry *ldap.Entry
}

// SendError sends an error message
func SendError(err error) tea.Cmd {
	return func() tea.Msg {
		return ErrorMsg{Err: err}
	}
}

// SendStatus sends a status message
func SendStatus(message string) tea.Cmd {
	return func() tea.Msg {
		return StatusMsg{Message: message}
	}
}

// ShowRecord sends a message to show a record
func ShowRecord(entry *ldap.Entry) tea.Cmd {
	return func() tea.Msg {
		return ShowRecordMsg{Entry: entry}
	}
}
