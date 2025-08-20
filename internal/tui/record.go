package tui

import (
	"fmt"
	"sort"
	"strings"

	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// RecordView displays detailed information about an LDAP entry
type RecordView struct {
	entry    *ldap.Entry
	lines    []string
	cursor   int
	viewport int
	width    int
	height   int
}

// NewRecordView creates a new record view
func NewRecordView() *RecordView {
	return &RecordView{
		cursor: 0,
	}
}

// Init initializes the record view
func (rv *RecordView) Init() tea.Cmd {
	return nil
}

// SetSize sets the size of the record view
func (rv *RecordView) SetSize(width, height int) {
	rv.width = width
	rv.height = height
}

// SetEntry sets the entry to display
func (rv *RecordView) SetEntry(entry *ldap.Entry) {
	rv.entry = entry
	rv.cursor = 0
	rv.viewport = 0
	rv.buildLines()
}

// Update handles messages for the record view
func (rv *RecordView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "up", "k":
			if rv.cursor > 0 {
				rv.cursor--
				rv.adjustViewport()
			}
		case "down", "j":
			if rv.cursor < len(rv.lines)-1 {
				rv.cursor++
				rv.adjustViewport()
			}
		case "page_up":
			rv.cursor -= rv.height
			if rv.cursor < 0 {
				rv.cursor = 0
			}
			rv.adjustViewport()
		case "page_down":
			rv.cursor += rv.height
			if rv.cursor >= len(rv.lines) {
				rv.cursor = len(rv.lines) - 1
			}
			rv.adjustViewport()
		case "home":
			rv.cursor = 0
			rv.adjustViewport()
		case "end":
			rv.cursor = len(rv.lines) - 1
			rv.adjustViewport()
		}
	}

	return rv, nil
}

// View renders the record view
func (rv *RecordView) View() string {
	if rv.entry == nil {
		return lipgloss.NewStyle().
			Width(rv.width).
			Height(rv.height).
			AlignHorizontal(lipgloss.Center).
			AlignVertical(lipgloss.Center).
			Render("No record selected")
	}

	if len(rv.lines) == 0 {
		return lipgloss.NewStyle().
			Width(rv.width).
			Height(rv.height).
			AlignHorizontal(lipgloss.Center).
			AlignVertical(lipgloss.Center).
			Render("Record is empty")
	}

	var displayLines []string
	visibleStart := rv.viewport
	visibleEnd := visibleStart + rv.height
	if visibleEnd > len(rv.lines) {
		visibleEnd = len(rv.lines)
	}

	for i := visibleStart; i < visibleEnd; i++ {
		line := rv.lines[i]
		style := lipgloss.NewStyle()

		if i == rv.cursor {
			style = style.Background(lipgloss.Color("240")).Foreground(lipgloss.Color("15"))
		}

		// Truncate if too long
		if len(line) > rv.width-2 {
			line = line[:rv.width-5] + "..."
		}

		displayLines = append(displayLines, style.Width(rv.width).Render(line))
	}

	// Fill remaining space
	for len(displayLines) < rv.height {
		displayLines = append(displayLines, "")
	}

	return strings.Join(displayLines, "\n")
}

// buildLines builds the display lines from the entry in table format
func (rv *RecordView) buildLines() {
	if rv.entry == nil {
		rv.lines = []string{}
		return
	}

	rv.lines = []string{}

	// Add DN header with styling
	dnStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("14")).
		Bold(true).
		Background(lipgloss.Color("238")).
		Padding(0, 1).
		Width(rv.width - 2)

	rv.lines = append(rv.lines, dnStyle.Render(fmt.Sprintf("DN: %s", rv.entry.DN)))
	rv.lines = append(rv.lines, "")

	// Calculate column widths for better responsiveness
	totalWidth := rv.width
	if totalWidth < 60 {
		totalWidth = 60 // minimum table width
	}

	nameWidth := totalWidth / 3              // ~33% for attribute names
	valueWidth := totalWidth - nameWidth - 4 // remaining width minus separators

	// Ensure minimum widths
	if nameWidth < 15 {
		nameWidth = 15
		valueWidth = totalWidth - nameWidth - 4
	}
	if valueWidth < 20 {
		valueWidth = 20
		nameWidth = totalWidth - valueWidth - 4
	}

	// Header styles
	nameHeaderStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("240")).
		Bold(true).
		Width(nameWidth).
		Align(lipgloss.Center).
		Padding(0, 1)

	valueHeaderStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("240")).
		Bold(true).
		Width(valueWidth).
		Align(lipgloss.Center).
		Padding(0, 1)

	// Create table header with borders
	headerRow := fmt.Sprintf("│%s│%s│",
		nameHeaderStyle.Render("Attribute"),
		valueHeaderStyle.Render("Value(s)"))

	// Add top border
	topBorder := "┌" + strings.Repeat("─", nameWidth+2) + "┬" + strings.Repeat("─", valueWidth+2) + "┐"
	rv.lines = append(rv.lines, topBorder)
	rv.lines = append(rv.lines, headerRow)

	// Add separator after header
	separator := "├" + strings.Repeat("─", nameWidth+2) + "┼" + strings.Repeat("─", valueWidth+2) + "┤"
	rv.lines = append(rv.lines, separator)

	// Sort attributes for consistent display
	var attrNames []string
	for name := range rv.entry.Attributes {
		attrNames = append(attrNames, name)
	}
	sort.Strings(attrNames)

	// Add attribute rows
	for i, name := range attrNames {
		values := rv.entry.Attributes[name]

		// Create value display
		var valueText string
		if len(values) == 1 {
			valueText = values[0]
		} else {
			// For multiple values, join with bullet points
			valueText = "• " + strings.Join(values, " • ")
		}

		// Wrap text instead of truncating
		wrappedLines := wrapText(valueText, valueWidth)

		// Style cells
		nameStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("12")).
			Bold(true).
			Width(nameWidth).
			Padding(0, 1)

		valueStyle := lipgloss.NewStyle().
			Width(valueWidth).
			Padding(0, 1)

		// Apply alternating row colors for better readability
		if i%2 == 1 {
			nameStyle = nameStyle.Background(lipgloss.Color("234"))
			valueStyle = valueStyle.Background(lipgloss.Color("234"))
		}

		// Create rows for wrapped content
		for lineIdx, wrappedLine := range wrappedLines {
			var nameText string
			if lineIdx == 0 {
				// First line shows the attribute name
				nameText = name
			} else {
				// Continuation lines have empty attribute cell
				nameText = ""
			}

			// Create the row with borders
			row := fmt.Sprintf("│%s│%s│",
				nameStyle.Render(nameText),
				valueStyle.Render(wrappedLine))
			rv.lines = append(rv.lines, row)
		}
	}

	// Add bottom border
	bottomBorder := "└" + strings.Repeat("─", nameWidth+2) + "┴" + strings.Repeat("─", valueWidth+2) + "┘"
	rv.lines = append(rv.lines, bottomBorder)
}

// wrapText wraps text to fit within the specified width
func wrapText(text string, width int) []string {
	if width <= 0 {
		return []string{text}
	}

	if len(text) <= width {
		return []string{text}
	}

	var lines []string
	remaining := text

	for len(remaining) > 0 {
		if len(remaining) <= width {
			lines = append(lines, remaining)
			break
		}

		// Find the best place to break the line
		breakPoint := width

		// Try to break at whitespace if possible
		for i := width - 1; i >= 0 && i >= width-20; i-- {
			if remaining[i] == ' ' || remaining[i] == '\t' || remaining[i] == '\n' {
				breakPoint = i
				break
			}
		}

		lines = append(lines, remaining[:breakPoint])
		remaining = strings.TrimLeft(remaining[breakPoint:], " \t\n")
	}

	return lines
}

// adjustViewport adjusts the viewport to keep the cursor visible
func (rv *RecordView) adjustViewport() {
	if rv.cursor < rv.viewport {
		rv.viewport = rv.cursor
	} else if rv.cursor >= rv.viewport+rv.height {
		rv.viewport = rv.cursor - rv.height + 1
	}

	if rv.viewport < 0 {
		rv.viewport = 0
	}
}
