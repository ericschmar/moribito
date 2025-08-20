package tui

import (
	"fmt"
	"strings"

	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// QueryView provides an interface for LDAP queries
type QueryView struct {
	client      *ldap.Client
	query       string
	cursor      int
	results     []*ldap.Entry
	resultLines []string
	viewport    int
	width       int
	height      int
	inputMode   bool
	loading     bool
	error       error
}

// NewQueryView creates a new query view
func NewQueryView(client *ldap.Client) *QueryView {
	return &QueryView{
		client:    client,
		query:     "(objectClass=*)",
		inputMode: true,
		cursor:    0,
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
		qv.results = msg.Results
		qv.loading = false
		qv.error = nil
		qv.inputMode = false
		qv.buildResultLines()
		return qv, SendStatus(fmt.Sprintf("Found %d results", len(qv.results)))

	case ErrorMsg:
		qv.loading = false
		qv.error = msg.Err
		return qv, nil
	}

	return qv, nil
}

// handleInputMode handles input when in query input mode
func (qv *QueryView) handleInputMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "enter":
		if strings.TrimSpace(qv.query) == "" {
			return qv, SendError(fmt.Errorf("query cannot be empty"))
		}
		return qv, qv.executeQuery()
	case "escape":
		qv.query = ""
		return qv, nil
	case "backspace":
		if len(qv.query) > 0 {
			qv.query = qv.query[:len(qv.query)-1]
		}
		return qv, nil
	case "ctrl+u":
		qv.query = ""
		return qv, nil
	default:
		// Handle regular character input
		if len(msg.String()) == 1 && msg.String() >= " " {
			qv.query += msg.String()
		}
	}

	return qv, nil
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
		if qv.cursor < len(qv.resultLines)-1 {
			qv.cursor++
			qv.adjustViewport()
		}
	case "page_up":
		qv.cursor -= qv.height - 4 // Account for header space
		if qv.cursor < 0 {
			qv.cursor = 0
		}
		qv.adjustViewport()
	case "page_down":
		qv.cursor += qv.height - 4
		if qv.cursor >= len(qv.resultLines) {
			qv.cursor = len(qv.resultLines) - 1
		}
		qv.adjustViewport()
	case "enter":
		return qv, qv.viewSelectedRecord()
	case "escape", "/":
		qv.inputMode = true
		qv.cursor = 0
		qv.viewport = 0
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

	// Query input
	queryStyle := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		Padding(0, 1)

	if qv.inputMode {
		queryStyle = queryStyle.BorderForeground(lipgloss.Color("12"))
	}

	queryContent := fmt.Sprintf("Query: %s", qv.query)
	if qv.inputMode {
		queryContent += "█" // Cursor
	}

	content.WriteString(queryStyle.Render(queryContent))
	content.WriteString("\n\n")

	// Status/Results area
	remainingHeight := qv.height - 6 // Account for header and query input

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
		content.WriteString("Enter your LDAP query above and press Enter to execute")
	}

	return content.String()
}

// renderResults renders the query results
func (qv *QueryView) renderResults(height int) string {
	if len(qv.resultLines) == 0 {
		return ""
	}

	var lines []string

	// Add results header
	headerStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("10")).
		Bold(true)
	lines = append(lines, headerStyle.Render(fmt.Sprintf("Results (%d entries):", len(qv.results))))
	lines = append(lines, "")

	// Add result lines
	visibleStart := qv.viewport
	visibleEnd := visibleStart + height - 2 // Account for header
	if visibleEnd > len(qv.resultLines) {
		visibleEnd = len(qv.resultLines)
	}

	for i := visibleStart; i < visibleEnd; i++ {
		line := qv.resultLines[i]
		style := lipgloss.NewStyle()

		if i == qv.cursor {
			style = style.Background(lipgloss.Color("240")).Foreground(lipgloss.Color("15"))
		}

		// Truncate if too long
		if len(line) > qv.width-2 {
			line = line[:qv.width-5] + "..."
		}

		lines = append(lines, style.Render(line))
	}

	return strings.Join(lines, "\n")
}

// executeQuery executes the LDAP query
func (qv *QueryView) executeQuery() tea.Cmd {
	qv.loading = true
	return func() tea.Msg {
		results, err := qv.client.CustomSearch(qv.query)
		if err != nil {
			return ErrorMsg{Err: err}
		}
		return QueryResultsMsg{Results: results}
	}
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
	qv.resultLines = nil

	for _, entry := range qv.results {
		// Add DN
		dnStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Bold(true)

		qv.resultLines = append(qv.resultLines, dnStyle.Render(entry.DN))

		// Add a few key attributes for preview
		previewAttrs := []string{"cn", "ou", "objectClass", "name", "displayName"}
		for _, attr := range previewAttrs {
			if values, exists := entry.Attributes[attr]; exists && len(values) > 0 {
				value := values[0]
				if len(values) > 1 {
					value += fmt.Sprintf(" (+%d more)", len(values)-1)
				}
				qv.resultLines = append(qv.resultLines, fmt.Sprintf("  %s: %s", attr, value))
			}
		}
		qv.resultLines = append(qv.resultLines, "")
	}

	// Remove trailing empty line
	if len(qv.resultLines) > 0 && qv.resultLines[len(qv.resultLines)-1] == "" {
		qv.resultLines = qv.resultLines[:len(qv.resultLines)-1]
	}
}

// adjustViewport adjusts the viewport to keep the cursor visible
func (qv *QueryView) adjustViewport() {
	visibleHeight := qv.height - 6 // Account for header space

	if qv.cursor < qv.viewport {
		qv.viewport = qv.cursor
	} else if qv.cursor >= qv.viewport+visibleHeight {
		qv.viewport = qv.cursor - visibleHeight + 1
	}

	if qv.viewport < 0 {
		qv.viewport = 0
	}
}

// QueryResultsMsg represents query results
type QueryResultsMsg struct {
	Results []*ldap.Entry
}
