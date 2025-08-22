package tui

import (
	"fmt"
	"strings"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/table"
	"github.com/charmbracelet/bubbles/textarea"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// Message types for query results
type QueryResultsMsg struct {
	Results []*ldap.Entry
}

type QueryPageMsg struct {
	Page        *ldap.SearchPage
	IsFirstPage bool
}

// QueryView provides an interface for LDAP queries
type QueryView struct {
	client      *ldap.Client
	textarea    textarea.Model
	results     []*ldap.Entry
	table       table.Model
	ResultLines []string // Keep for backward compatibility during transition
	width       int
	height      int
	inputMode   bool
	loading     bool
	error       error
	container   *ViewContainer

	// Pagination state
	pageSize        uint32
	hasMore         bool
	currentCookie   []byte
	loadingNextPage bool
}

// NewQueryView creates a new query view
func NewQueryView(client *ldap.Client) *QueryView {
	ta := textarea.New()
	ta.SetValue("(objectClass=*)")
	ta.Placeholder = "Enter your LDAP query..."
	ta.ShowLineNumbers = false
	ta.Focus()

	// Create table with columns for query results
	columns := []table.Column{
		{Title: "DN", Width: 40},
		{Title: "Summary", Width: 60},
	}

	t := table.New(
		table.WithColumns(columns),
		table.WithRows([]table.Row{}),
		table.WithFocused(false), // Start unfocused since we start in input mode
		table.WithHeight(10),
	)

	// Style the table
	s := table.DefaultStyles()
	s.Header = s.Header.
		BorderStyle(lipgloss.NormalBorder()).
		BorderForeground(lipgloss.Color("240")).
		BorderBottom(true).
		Bold(false)
	s.Selected = s.Selected.
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color(GetGradientColor(0.3))).
		Bold(false)
	t.SetStyles(s)

	return &QueryView{
		client:    client,
		textarea:  ta,
		table:     t,
		inputMode: true,
		pageSize:  50, // Default page size
	}
}

// NewQueryViewWithPageSize creates a new query view with custom page size
func NewQueryViewWithPageSize(client *ldap.Client, pageSize uint32) *QueryView {
	ta := textarea.New()
	ta.SetValue("(objectClass=*)")
	ta.Placeholder = "Enter your LDAP query..."
	ta.ShowLineNumbers = false
	ta.Focus()

	// Create table with columns for query results
	columns := []table.Column{
		{Title: "DN", Width: 40},
		{Title: "Summary", Width: 60},
	}

	t := table.New(
		table.WithColumns(columns),
		table.WithRows([]table.Row{}),
		table.WithFocused(false), // Start unfocused since we start in input mode
		table.WithHeight(10),
	)

	// Style the table
	s := table.DefaultStyles()
	s.Header = s.Header.
		BorderStyle(lipgloss.NormalBorder()).
		BorderForeground(lipgloss.Color("240")).
		BorderBottom(true).
		Bold(false)
	s.Selected = s.Selected.
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color(GetGradientColor(0.3))).
		Bold(false)
	t.SetStyles(s)

	return &QueryView{
		client:    client,
		textarea:  ta,
		table:     t,
		inputMode: true,
		pageSize:  pageSize,
	}
}

// IsInputMode returns whether the query view is in input mode
func (qv *QueryView) IsInputMode() bool {
	return qv.inputMode
}

// SetResults sets the results for testing purposes
func (qv *QueryView) SetResults(entries []*ldap.Entry) {
	qv.results = entries
	qv.buildTableRows()
}

// Init initializes the query view
func (qv *QueryView) Init() tea.Cmd {
	return nil
}

// SetSize sets the size of the query view
func (qv *QueryView) SetSize(width, height int) {
	qv.width = width
	qv.height = height
	qv.container = NewViewContainer(width, height)

	// Get content dimensions for textarea sizing
	contentWidth, contentHeight := qv.container.GetContentDimensions()

	// Set textarea width to fit within the content area
	qv.textarea.SetWidth(contentWidth - 4) // Account for border and padding
	// Allow the textarea to be multi-line but limit height reasonably
	qv.textarea.SetHeight(3)

	// Configure table dimensions
	// Reserve space for title, query, status, instructions (rough estimate ~10 lines)
	tableHeight := contentHeight - 12
	if tableHeight < 3 {
		tableHeight = 3
	}

	// Calculate column widths based on available content space
	dnWidth := contentWidth / 3
	if dnWidth < 20 {
		dnWidth = 20
	}
	summaryWidth := contentWidth - dnWidth - 4 // Account for borders and spacing
	if summaryWidth < 30 {
		summaryWidth = 30
		dnWidth = contentWidth - summaryWidth - 4
	}

	// Update table dimensions and columns
	columns := []table.Column{
		{Title: "DN", Width: dnWidth},
		{Title: "Summary", Width: summaryWidth},
	}
	qv.table.SetColumns(columns)
	qv.table.SetHeight(tableHeight)
	qv.table.SetWidth(contentWidth)
}

// Update handles input for the query view
func (qv *QueryView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	switch msg := msg.(type) {
	case tea.KeyMsg:
		if qv.inputMode {
			return qv.handleInputMode(msg)
		} else {
			return qv.handleBrowseMode(msg)
		}

	case QueryResultsMsg:
		// Legacy non-paginated results (fallback)
		qv.results = msg.Results
		qv.loading = false
		qv.error = nil
		qv.inputMode = false
		qv.textarea.Blur() // Blur the textarea when browsing results
		qv.table.Focus()   // Focus the table when browsing results
		qv.hasMore = false
		qv.currentCookie = nil
		qv.buildTableRows()
		return qv, SendStatus(fmt.Sprintf("Found %d results", len(qv.results)))

	case QueryPageMsg:
		// Handle paginated results
		if msg.IsFirstPage {
			// First page - replace existing results
			qv.results = msg.Page.Entries
		} else {
			// Subsequent page - append to existing results
			qv.results = append(qv.results, msg.Page.Entries...)
		}

		// Update pagination state
		qv.hasMore = msg.Page.HasMore
		qv.currentCookie = msg.Page.Cookie
		qv.loading = false
		qv.loadingNextPage = false
		qv.error = nil
		qv.inputMode = false
		qv.textarea.Blur()
		qv.table.Focus()

		qv.buildTableRows()

		totalResults := len(qv.results)
		statusMsg := fmt.Sprintf("Found %d results", totalResults)
		if qv.hasMore {
			statusMsg += " (more available)"
		}
		return qv, SendStatus(statusMsg)

	case ErrorMsg:
		qv.loading = false
		qv.loadingNextPage = false
		qv.error = msg.Err
		return qv, nil
	}

	return qv, cmd
}

// handleInputMode handles input when the textarea is focused
func (qv *QueryView) handleInputMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	switch msg.String() {
	case "enter":
		if !qv.loading {
			qv.loading = true
			qv.error = nil
			return qv, qv.executeQuery()
		}
		return qv, nil

	case "esc":
		if qv.loading {
			qv.loading = false
			return qv, nil
		}
		// Clear results and reset to input mode
		qv.results = nil
		qv.ResultLines = nil
		qv.table.SetRows([]table.Row{})
		qv.hasMore = false
		qv.currentCookie = nil
		qv.inputMode = true
		qv.table.Blur()
		qv.textarea.Focus()
		return qv, nil

	case "ctrl+c":
		return qv, tea.Quit

	case "tab":
		// Switch to browse mode if we have results
		if len(qv.results) > 0 {
			qv.inputMode = false
			qv.textarea.Blur()
			qv.table.Focus()
		}
		return qv, nil

	case "ctrl+v":
		// Handle paste
		if clipboardText, err := clipboard.ReadAll(); err == nil {
			qv.textarea.SetValue(qv.textarea.Value() + clipboardText)
		}
		return qv, nil
	}

	// Forward to textarea
	qv.textarea, cmd = qv.textarea.Update(msg)
	return qv, cmd
}

// handleBrowseMode handles input when browsing results
func (qv *QueryView) handleBrowseMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	switch msg.String() {
	case "enter", " ":
		// Show record details for selected entry
		selectedRow := qv.table.Cursor()
		if selectedRow < len(qv.results) {
			return qv, ShowRecord(qv.results[selectedRow])
		}
	case "esc":
		// Return to input mode
		qv.inputMode = true
		qv.table.Blur()
		qv.textarea.Focus()
		return qv, nil
	case "n":
		// Load next page if available
		if qv.hasMore && !qv.loadingNextPage {
			qv.loadingNextPage = true
			return qv, qv.loadNextPage()
		}
	default:
		// Forward navigation keys to the table
		qv.table, cmd = qv.table.Update(msg)
		return qv, cmd
	}

	return qv, nil
}

// View renders the query view
func (qv *QueryView) View() string {
	if qv.container == nil {
		qv.container = NewViewContainer(qv.width, qv.height)
	}

	var sections []string

	// Title
	titleStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("13")).
		Bold(true).
		Padding(1, 2).
		Margin(0, 0, 1, 0)

	title := titleStyle.Render("🔍 LDAP Query Interface")
	sections = append(sections, title)

	// Query input area
	queryHeader := lipgloss.NewStyle().
		Foreground(lipgloss.Color("14")).
		Bold(true).
		Render("Query:")

	sections = append(sections, queryHeader)

	// Textarea with border
	textareaStyle := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("6")).
		Padding(0, 1)

	textareaContent := textareaStyle.Render(qv.textarea.View())
	sections = append(sections, textareaContent)

	// Status/loading information
	if qv.loading {
		loadingStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Italic(true)
		sections = append(sections, loadingStyle.Render("⏳ Executing query..."))
	} else if qv.loadingNextPage {
		loadingStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Italic(true)
		sections = append(sections, loadingStyle.Render("⏳ Loading next page..."))
	} else if qv.error != nil {
		errorStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("9")).
			Bold(true)
		sections = append(sections, errorStyle.Render(fmt.Sprintf("❌ Error: %s", qv.error.Error())))
	}

	// Results area
	if len(qv.results) > 0 {
		resultsHeader := lipgloss.NewStyle().
			Foreground(lipgloss.Color("14")).
			Bold(true).
			Margin(1, 0, 0, 0).
			Render("Results:")
		sections = append(sections, resultsHeader)

		// Calculate remaining height dynamically based on content built so far
		_, contentHeight := qv.container.GetContentDimensions()

		// Count lines in sections built so far
		currentContent := strings.Join(sections, "\n")
		currentLines := strings.Split(currentContent, "\n")
		usedLines := len(currentLines)

		// Reserve space for instructions at the bottom (2 lines: margin + instruction text)
		instructionLines := 2

		// Calculate remaining space for results
		remainingHeight := contentHeight - usedLines - instructionLines
		if remainingHeight < 0 {
			remainingHeight = 0
		}

		if remainingHeight > 0 {
			resultsContent := qv.renderTable()
			sections = append(sections, resultsContent)
		}
	}

	// Instructions
	var instructions string
	if qv.inputMode {
		instructions = "Press [Enter] to execute • [Esc] to clear • [Tab] to browse results"
		if len(qv.results) > 0 {
			instructions += " • [Ctrl+V] to paste"
		}
	} else {
		instructions = "Press [↑↓] to navigate • [Enter/Space] to view record • [Esc] to edit query"
		if qv.hasMore {
			instructions += " • [N] for next page"
		}
	}

	instructionStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("8")).
		Italic(true).
		Margin(1, 0, 0, 0)
	sections = append(sections, instructionStyle.Render(instructions))

	content := strings.Join(sections, "\n")

	return qv.container.RenderWithPadding(content)
}

// executeQuery executes the current query
func (qv *QueryView) executeQuery() tea.Cmd {
	query := strings.TrimSpace(qv.textarea.Value())
	if query == "" {
		return SendError(fmt.Errorf("query cannot be empty"))
	}

	return func() tea.Msg {
		page, err := qv.client.CustomSearchPaged(query, qv.pageSize, nil)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return QueryPageMsg{Page: page, IsFirstPage: true}
	}
}

// loadNextPage loads the next page of results
func (qv *QueryView) loadNextPage() tea.Cmd {
	query := strings.TrimSpace(qv.textarea.Value())
	if query == "" {
		return SendError(fmt.Errorf("query cannot be empty"))
	}

	return func() tea.Msg {
		page, err := qv.client.CustomSearchPaged(query, qv.pageSize, qv.currentCookie)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return QueryPageMsg{Page: page, IsFirstPage: false}
	}
}

// buildTableRows builds the table rows from results
func (qv *QueryView) buildTableRows() {
	if len(qv.results) == 0 {
		qv.table.SetRows([]table.Row{})
		return
	}

	var rows []table.Row

	for _, entry := range qv.results {
		// Create DN column
		dn := entry.DN

		// Create summary column with key attributes
		var summaryParts []string
		for attrName, attrValues := range entry.Attributes {
			if len(attrValues) > 0 {
				summary := fmt.Sprintf("%s: %s", attrName, attrValues[0])
				if len(attrValues) > 1 {
					summary += fmt.Sprintf(" (+%d more)", len(attrValues)-1)
				}
				summaryParts = append(summaryParts, summary)
				// Limit to first few attributes to keep summary concise
				if len(summaryParts) >= 3 {
					break
				}
			}
		}

		summary := strings.Join(summaryParts, " | ")
		if summary == "" {
			summary = "(no attributes)"
		}

		rows = append(rows, table.Row{dn, summary})
	}

	qv.table.SetRows(rows)

	// Also keep the legacy ResultLines for backward compatibility during transition
	qv.buildResultLines()
}

// renderTable renders the table with proper styling and pagination info
func (qv *QueryView) renderTable() string {
	if len(qv.results) == 0 {
		return "No results"
	}

	result := qv.table.View()

	// Add pagination info if applicable
	if qv.hasMore {
		paginationInfo := lipgloss.NewStyle().
			Foreground(lipgloss.Color("8")).
			Italic(true).
			Render(fmt.Sprintf("Showing %d of %d+ results • Press [N] for next page",
				len(qv.results), len(qv.results)))
		result += "\n" + paginationInfo
	}

	return result
}

// buildResultLines builds the display lines from results (kept for backward compatibility)
func (qv *QueryView) buildResultLines() {
	qv.ResultLines = qv.ResultLines[:0] // Clear but keep capacity

	for _, entry := range qv.results {
		// Format entry for display
		line := fmt.Sprintf("DN: %s", entry.DN)
		qv.ResultLines = append(qv.ResultLines, line)

		// Add a few key attributes for preview
		for attrName, attrValues := range entry.Attributes {
			if len(attrValues) > 0 {
				line = fmt.Sprintf("  %s: %s", attrName, attrValues[0])
				if len(attrValues) > 1 {
					line += fmt.Sprintf(" (+%d more)", len(attrValues)-1)
				}
				qv.ResultLines = append(qv.ResultLines, line)
			}
		}
		qv.ResultLines = append(qv.ResultLines, "") // Empty line between entries
	}

	// Remove trailing empty line
	if len(qv.ResultLines) > 0 && qv.ResultLines[len(qv.ResultLines)-1] == "" {
		qv.ResultLines = qv.ResultLines[:len(qv.ResultLines)-1]
	}
}
