package tui

import (
	"fmt"
	"strings"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/textarea"
	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	zone "github.com/lrstanley/bubblezone"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

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
		hasMore:   false,
	}
}

// NewQueryViewWithPageSize creates a new query view with specified page size
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
		hasMore:   false,
	}
}

// Init initializes the query view
func (qv *QueryView) Init() tea.Cmd {
	return nil
}

// SetSize sets the size of the query view
func (qv *QueryView) SetSize(width, height int) {
	qv.width = width
	qv.height = height
	// Set textarea width to fit within the border with some padding
	qv.textarea.SetWidth(width - 4) // Account for border and padding
	// Allow the textarea to be multi-line but limit height reasonably
	qv.textarea.SetHeight(3) // Start with 3 lines, can expand
}

// IsInputMode returns true if the query view is in input mode
func (qv *QueryView) IsInputMode() bool {
	return qv.inputMode
}

// Update handles messages for the query view
func (qv *QueryView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
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

		qv.loading = false
		qv.loadingNextPage = false
		qv.error = nil
		qv.inputMode = false
		qv.textarea.Blur() // Blur the textarea when browsing results
		qv.hasMore = msg.Page.HasMore
		qv.currentCookie = msg.Page.Cookie
		qv.buildResultLines()

		statusMsg := fmt.Sprintf("Loaded %d results", len(qv.results))
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

	return qv, nil
}

// handleInputMode handles input when in query input mode
func (qv *QueryView) handleInputMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "ctrl+enter", "ctrl+j":
		// Execute query with Ctrl+Enter or Ctrl+J
		query := strings.TrimSpace(qv.textarea.Value())
		if query == "" {
			return qv, SendError(fmt.Errorf("query cannot be empty"))
		}
		return qv, qv.executeQuery()
	case "escape":
		qv.textarea.SetValue("")
		return qv, nil
	case "ctrl+u":
		qv.textarea.SetValue("")
		return qv, nil
	case "ctrl+v":
		// Handle paste from clipboard
		if clipboardText, err := clipboard.ReadAll(); err == nil {
			qv.textarea.InsertString(clipboardText)
		}
		return qv, nil
	case "ctrl+f":
		// Format the current query
		currentQuery := qv.textarea.Value()
		formattedQuery := qv.formatLdapQuery(currentQuery)
		qv.textarea.SetValue(formattedQuery)
		return qv, nil
	}

	// Let textarea handle the input
	var cmd tea.Cmd
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

			// Check if we need to load next page
			if cmd := qv.checkLoadNextPage(); cmd != nil {
				return qv, cmd
			}
		}
	case "page_up":
		qv.cursor -= qv.height - 8 // Account for header space
		if qv.cursor < 0 {
			qv.cursor = 0
		}
		qv.adjustViewport()
	case "page_down":
		qv.cursor += qv.height - 8
		if qv.cursor >= len(qv.ResultLines) {
			qv.cursor = len(qv.ResultLines) - 1
		}
		qv.adjustViewport()

		// Check if we need to load next page
		if cmd := qv.checkLoadNextPage(); cmd != nil {
			return qv, cmd
		}
	case "enter":
		return qv, qv.viewSelectedRecord()
	case "esc", "/":
		qv.inputMode = true
		qv.cursor = 0
		qv.viewport = 0
		qv.textarea.Focus() // Focus the textarea when returning to input mode
		return qv, nil
	}

	return qv, nil
}

// View renders the query view
func (qv *QueryView) View() string {
	var content strings.Builder

	// Header
	headerStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("12")).
		Bold(true)

	content.WriteString(headerStyle.Render("LDAP Query Interface"))
	content.WriteString("\n\n")

	// Query input area using textarea
	queryStyle := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		Padding(0, 1)

	if qv.inputMode {
		queryStyle = queryStyle.BorderForeground(lipgloss.Color("12"))
	}

	// Render the textarea
	textareaView := qv.textarea.View()
	content.WriteString(queryStyle.Render(textareaView))

	// Add instruction text
	if qv.inputMode {
		instructionStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("8")).
			Italic(true)
		content.WriteString("\n")
		content.WriteString(instructionStyle.Render("Press Ctrl+Enter to execute query, Ctrl+F to format, Escape to clear, / to return to search"))
	}
	content.WriteString("\n\n")

	// Status/Results area
	remainingHeight := qv.height - 8 // Account for header, query input, and instruction

	if qv.loading {
		statusContent := lipgloss.NewStyle().
			AlignHorizontal(lipgloss.Center).
			Render("Executing query...")
		content.WriteString(statusContent)
	} else if qv.error != nil {
		errorStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("9"))
		content.WriteString(errorStyle.Render(fmt.Sprintf("Error: %s", qv.error.Error())))
	} else if len(qv.results) == 0 && !qv.inputMode {
		content.WriteString("No results found")
	} else if len(qv.results) > 0 {
		content.WriteString(qv.renderResults(remainingHeight))
	} else if qv.inputMode {
		content.WriteString("Enter your LDAP query above and press Ctrl+Enter to execute")
	}

	return content.String()
}

// renderResults renders the query results
func (qv *QueryView) renderResults(height int) string {
	if len(qv.ResultLines) == 0 {
		return ""
	}

	var lines []string

	// Add results header with pagination info
	headerStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("10")).
		Bold(true)

	headerText := fmt.Sprintf("Results (%d entries", len(qv.results))
	if qv.hasMore {
		headerText += ", more available"
	}
	headerText += "):"

	if qv.loadingNextPage {
		headerText += " [Loading next page...]"
	}

	lines = append(lines, headerStyle.Render(headerText))
	lines = append(lines, "")

	// Add result lines
	visibleStart := qv.viewport
	visibleEnd := visibleStart + height - 2 // Account for header
	if visibleEnd > len(qv.ResultLines) {
		visibleEnd = len(qv.ResultLines)
	}

	for i := visibleStart; i < visibleEnd; i++ {
		line := qv.ResultLines[i]
		style := lipgloss.NewStyle()

		if i == qv.cursor {
			style = style.Background(lipgloss.Color("240")).Foreground(lipgloss.Color("15"))
		}

		// Truncate if too long
		if len(line) > qv.width-2 {
			line = line[:qv.width-5] + "..."
		}

		renderedLine := style.Render(line)
		
		// Wrap with clickable zone
		zoneID := fmt.Sprintf("query-result-%d", i)
		renderedLine = zone.Mark(zoneID, renderedLine)
		
		lines = append(lines, renderedLine)
	}

	return strings.Join(lines, "\n")
}

// executeQuery executes the LDAP query using pagination
func (qv *QueryView) executeQuery() tea.Cmd {
	qv.loading = true
	qv.hasMore = false
	qv.currentCookie = nil
	qv.results = nil
	qv.ResultLines = nil

	query := strings.TrimSpace(qv.textarea.Value())

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
	if !qv.hasMore || qv.loadingNextPage || qv.currentCookie == nil {
		return nil
	}

	qv.loadingNextPage = true
	query := strings.TrimSpace(qv.textarea.Value())

	return func() tea.Msg {
		page, err := qv.client.CustomSearchPaged(query, qv.pageSize, qv.currentCookie)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return QueryPageMsg{Page: page, IsFirstPage: false}
	}
}

// checkLoadNextPage checks if we need to load the next page based on cursor position
func (qv *QueryView) checkLoadNextPage() tea.Cmd {
	if !qv.hasMore || qv.loadingNextPage {
		return nil
	}

	// Load next page when we're within 5 entries of the end
	entriesFromEnd := len(qv.results) - (qv.cursor / 4) // Approximate cursor to entry mapping
	if entriesFromEnd <= 5 {
		return qv.loadNextPage()
	}

	return nil
}

// viewSelectedRecord shows the selected record
func (qv *QueryView) viewSelectedRecord() tea.Cmd {
	if qv.cursor >= len(qv.results) {
		return nil
	}

	entry := qv.results[qv.cursor]
	return ShowRecord(entry)
}

// buildResultLines builds display lines from the results
func (qv *QueryView) buildResultLines() {
	qv.ResultLines = nil

	for _, entry := range qv.results {
		// Add DN
		dnStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Bold(true)

		qv.ResultLines = append(qv.ResultLines, dnStyle.Render(entry.DN))

		// Add a few key attributes for preview
		previewAttrs := []string{"cn", "ou", "objectClass", "name", "displayName"}
		for _, attr := range previewAttrs {
			if values, exists := entry.Attributes[attr]; exists && len(values) > 0 {
				value := values[0]
				if len(values) > 1 {
					value += fmt.Sprintf(" (+%d more)", len(values)-1)
				}
				qv.ResultLines = append(qv.ResultLines, fmt.Sprintf("  %s: %s", attr, value))
			}
		}
		qv.ResultLines = append(qv.ResultLines, "")
	}

	// Remove trailing empty line
	if len(qv.ResultLines) > 0 && qv.ResultLines[len(qv.ResultLines)-1] == "" {
		qv.ResultLines = qv.ResultLines[:len(qv.ResultLines)-1]
	}
}

// adjustViewport adjusts the viewport to keep the cursor visible
func (qv *QueryView) adjustViewport() {
	visibleHeight := qv.height - 8 // Account for header, query input, and instruction text

	if qv.cursor < qv.viewport {
		qv.viewport = qv.cursor
	} else if qv.cursor >= qv.viewport+visibleHeight {
		qv.viewport = qv.cursor - visibleHeight + 1
	}

	if qv.viewport < 0 {
		qv.viewport = 0
	}
}

// formatLdapQuery formats an LDAP query with proper indentation
func (qv *QueryView) formatLdapQuery(query string) string {
	query = strings.TrimSpace(query)
	if query == "" {
		return query
	}

	return formatLdapFilter(query, 0)
}

// formatLdapFilter recursively formats LDAP filter expressions
func formatLdapFilter(filter string, indentLevel int) string {
	filter = strings.TrimSpace(filter)
	if filter == "" {
		return filter
	}

	// If it doesn't start with '(', it's not a valid LDAP filter
	if !strings.HasPrefix(filter, "(") || !strings.HasSuffix(filter, ")") {
		return filter
	}

	// Remove outer parentheses
	inner := filter[1 : len(filter)-1]

	// Check if this is a simple attribute=value filter
	if !strings.ContainsAny(inner, "&|!") {
		return filter // Return as-is for simple filters
	}

	// Handle complex filters with operators
	if len(inner) == 0 {
		return filter
	}

	operator := inner[0]
	if operator != '&' && operator != '|' && operator != '!' {
		return filter // Not a logical operator
	}

	// Parse the operands
	operands := parseFilterOperands(inner[1:])
	if len(operands) == 0 {
		return filter
	}

	indent := strings.Repeat("  ", indentLevel)
	childIndent := strings.Repeat("  ", indentLevel+1)

	var result strings.Builder
	result.WriteString("(" + string(operator))

	for _, operand := range operands {
		result.WriteString("\n")
		result.WriteString(childIndent)
		result.WriteString(formatLdapFilter(operand, indentLevel+1))
	}

	result.WriteString("\n")
	result.WriteString(indent)
	result.WriteString(")")

	return result.String()
}

// parseFilterOperands parses the operands of a logical filter
func parseFilterOperands(content string) []string {
	var operands []string
	var current strings.Builder
	depth := 0

	for i, char := range content {
		if char == '(' {
			if depth == 0 && current.Len() > 0 {
				// This shouldn't happen in valid filters, but handle gracefully
				operands = append(operands, strings.TrimSpace(current.String()))
				current.Reset()
			}
			current.WriteRune(char)
			depth++
		} else if char == ')' {
			current.WriteRune(char)
			depth--
			if depth == 0 {
				// Complete operand found
				operand := strings.TrimSpace(current.String())
				if operand != "" {
					operands = append(operands, operand)
				}
				current.Reset()
			}
		} else {
			current.WriteRune(char)
		}

		// Safety check for malformed filters
		if depth < 0 {
			// Malformed filter, return what we have
			remaining := content[i:]
			if remaining != "" {
				operands = append(operands, current.String()+remaining)
			}
			break
		}
	}

	// Handle any remaining content
	if current.Len() > 0 {
		remaining := strings.TrimSpace(current.String())
		if remaining != "" {
			operands = append(operands, remaining)
		}
	}

	return operands
}

// QueryResultsMsg represents query results
type QueryResultsMsg struct {
	Results []*ldap.Entry
}

// QueryPageMsg represents a page of query results
type QueryPageMsg struct {
	Page        *ldap.SearchPage
	IsFirstPage bool
}
