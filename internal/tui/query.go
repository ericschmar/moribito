package tui

import (
	"fmt"
	"strings"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/textarea"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
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
	cursor      int
	results     []*ldap.Entry
	ResultLines []string
	viewport    int
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

	return &QueryView{
		client:    client,
		textarea:  ta,
		inputMode: true,
		cursor:    0,
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

	return &QueryView{
		client:    client,
		textarea:  ta,
		inputMode: true,
		cursor:    0,
		pageSize:  pageSize,
	}
}

// IsInputMode returns whether the query view is in input mode
func (qv *QueryView) IsInputMode() bool {
	return qv.inputMode
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
	contentWidth, _ := qv.container.GetContentDimensions()

	// Set textarea width to fit within the content area
	qv.textarea.SetWidth(contentWidth - 4) // Account for border and padding
	// Allow the textarea to be multi-line but limit height reasonably
	qv.textarea.SetHeight(3)
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
		qv.hasMore = false
		qv.currentCookie = nil
		qv.buildResultLines()
		return qv, SendStatus(fmt.Sprintf("Found %d results", len(qv.results)))

	case QueryPageMsg:
		// Handle paginated results
		if msg.IsFirstPage {
			// First page - replace existing results
			qv.results = msg.Page.Entries
			qv.cursor = 0
			qv.viewport = 0
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

		qv.buildResultLines()
		qv.adjustViewport()

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
		qv.cursor = 0
		qv.viewport = 0
		qv.hasMore = false
		qv.currentCookie = nil
		qv.inputMode = true
		qv.textarea.Focus()
		return qv, nil

	case "ctrl+c":
		return qv, tea.Quit

	case "tab":
		// Switch to browse mode if we have results
		if len(qv.results) > 0 {
			qv.inputMode = false
			qv.textarea.Blur()
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
	switch msg.String() {
	case "up", "k":
		if qv.cursor > 0 {
			qv.cursor--
			qv.adjustViewport()
		}
	case "down", "j":
		if qv.cursor < len(qv.ResultLines)-1 {
			qv.cursor++
			qv.adjustViewport()
		}
	case "page_up":
		_, contentHeight := qv.container.GetContentDimensions()
		if qv.container == nil {
			contentHeight = qv.height
		}
		qv.cursor -= contentHeight - 8 // Account for header space
		if qv.cursor < 0 {
			qv.cursor = 0
		}
		qv.adjustViewport()
	case "page_down":
		_, contentHeight := qv.container.GetContentDimensions()
		if qv.container == nil {
			contentHeight = qv.height
		}
		qv.cursor += contentHeight - 8
		if qv.cursor >= len(qv.ResultLines) {
			qv.cursor = len(qv.ResultLines) - 1
		}
		qv.adjustViewport()
	case "home":
		qv.cursor = 0
		qv.adjustViewport()
	case "end":
		qv.cursor = len(qv.ResultLines) - 1
		qv.adjustViewport()
	case "enter", " ":
		// Show record details for selected entry
		if qv.cursor < len(qv.results) {
			return qv, ShowRecord(qv.results[qv.cursor])
		}
	case "esc":
		// Return to input mode
		qv.inputMode = true
		qv.textarea.Focus()
		return qv, nil
	case "n":
		// Load next page if available
		if qv.hasMore && !qv.loadingNextPage {
			qv.loadingNextPage = true
			return qv, qv.loadNextPage()
		}
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
			resultsContent := qv.renderResults(remainingHeight)
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

// renderResults renders the results list
func (qv *QueryView) renderResults(maxHeight int) string {
	if len(qv.ResultLines) == 0 {
		return "No results"
	}

	// Reserve space for pagination info if needed
	availableHeight := maxHeight
	if qv.hasMore {
		availableHeight = maxHeight - 1 // Reserve 1 line for pagination info
	}
	
	// Ensure we have at least 1 line for results
	if availableHeight < 1 {
		availableHeight = 1
	}

	// Calculate visible range
	visibleStart := qv.viewport
	visibleEnd := visibleStart + availableHeight
	if visibleEnd > len(qv.ResultLines) {
		visibleEnd = len(qv.ResultLines)
	}

	var lines []string

	// Get content dimensions for consistent width handling
	contentWidth, _ := qv.container.GetContentDimensions()

	for i := visibleStart; i < visibleEnd; i++ {
		line := qv.ResultLines[i]
		isSelected := i == qv.cursor

		var renderedLine string
		if isSelected {
			style := lipgloss.NewStyle().
				Foreground(lipgloss.Color("0")).
				Background(lipgloss.Color("4")).
				Bold(true).
				Width(contentWidth - 4) // Account for padding
			renderedLine = style.Render(line)
		} else {
			style := lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Width(contentWidth - 4)
			renderedLine = style.Render(line)
		}

		// Wrap with clickable zone
		zoneID := fmt.Sprintf("query-result-%d", i)
		renderedLine = zone.Mark(zoneID, renderedLine)

		lines = append(lines, renderedLine)
	}

	result := strings.Join(lines, "\n")

	// Add pagination info if applicable and if we reserved space for it
	if qv.hasMore && maxHeight > 1 {
		paginationInfo := lipgloss.NewStyle().
			Foreground(lipgloss.Color("8")).
			Italic(true).
			Render(fmt.Sprintf("Page info: Showing %d-%d of %d+ results", visibleStart+1, visibleEnd, len(qv.ResultLines)))
		result += "\n" + paginationInfo
	}

	return result
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

// buildResultLines builds the display lines from results
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

// adjustViewport adjusts the viewport to keep the cursor visible
func (qv *QueryView) adjustViewport() {
	// Get content height and calculate actual space available for results
	_, contentHeight := qv.container.GetContentDimensions()
	if qv.container == nil {
		contentHeight = qv.height
	}
	
	// Calculate the same way as in View() - count actual UI elements
	// This ensures consistency between rendering and viewport calculations
	
	// Estimated fixed elements (more conservative than hardcoded 8):
	// - Title: ~2 lines (with margin)
	// - Query header: 1 line  
	// - Textarea: ~3 lines (with border)
	// - Status: ~1 line (when present)
	// - Results header: ~2 lines (with margin)
	// - Instructions: ~2 lines (with margin)
	// Total: ~11 lines, so use 12 to be safe
	fixedUILines := 12
	visibleHeight := contentHeight - fixedUILines
	
	// Ensure we have at least 1 line for results
	if visibleHeight < 1 {
		visibleHeight = 1
	}

	if qv.cursor < qv.viewport {
		qv.viewport = qv.cursor
	} else if qv.cursor >= qv.viewport+visibleHeight {
		qv.viewport = qv.cursor - visibleHeight + 1
	}

	if qv.viewport < 0 {
		qv.viewport = 0
	}
}
