package tui

import (
	"fmt"

	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/config"
	"github.com/ericschmar/ldap-cli/internal/ldap"
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

		// Update child views
		m.startView.SetSize(msg.Width, msg.Height-3) // Reserve space for status and help
		if m.tree != nil {
			m.tree.SetSize(msg.Width, msg.Height-3)
		}
		m.recordView.SetSize(msg.Width, msg.Height-3)
		if m.queryView != nil {
			m.queryView.SetSize(msg.Width, msg.Height-3)
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
		case "1":
			if m.tree != nil {
				m.currentView = ViewModeTree
			}
			return m, nil
		case "2":
			m.currentView = ViewModeRecord
			return m, nil
		case "3":
			if m.queryView != nil {
				m.currentView = ViewModeQuery
			}
			return m, nil
		}

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

	return content + "\n" + status + "\n" + help
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
	var viewName string
	switch m.currentView {
	case ViewModeStart:
		viewName = "Start Page"
	case ViewModeTree:
		viewName = "Tree Explorer"
	case ViewModeRecord:
		viewName = "Record View"
	case ViewModeQuery:
		viewName = "Query Interface"
	}

	status := fmt.Sprintf("View: %s", viewName)
	if m.statusMsg != "" {
		status += fmt.Sprintf(" | %s", m.statusMsg)
	}
	if m.err != nil {
		status += fmt.Sprintf(" | Error: %s", m.err.Error())
	}

	return lipgloss.NewStyle().
		Background(lipgloss.Color("240")).
		Foreground(lipgloss.Color("15")).
		Width(m.width).
		Padding(0, 1).
		Render(status)
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
