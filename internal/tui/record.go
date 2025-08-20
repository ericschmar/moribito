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

// buildLines builds the display lines from the entry
func (rv *RecordView) buildLines() {
	if rv.entry == nil {
		rv.lines = []string{}
		return
	}

	rv.lines = []string{}

	// Add DN header
	rv.lines = append(rv.lines, fmt.Sprintf("DN: %s", rv.entry.DN))
	rv.lines = append(rv.lines, "")

	// Sort attributes for consistent display
	var attrNames []string
	for name := range rv.entry.Attributes {
		attrNames = append(attrNames, name)
	}
	sort.Strings(attrNames)

	// Add attributes
	for _, name := range attrNames {
		values := rv.entry.Attributes[name]

		// Style attribute names
		attributeStyle := lipgloss.NewStyle().
			Foreground(lipgloss.Color("12")).
			Bold(true)

		if len(values) == 1 {
			rv.lines = append(rv.lines, fmt.Sprintf("%s: %s",
				attributeStyle.Render(name), values[0]))
		} else {
			rv.lines = append(rv.lines, fmt.Sprintf("%s:", attributeStyle.Render(name)))
			for i, value := range values {
				prefix := "├─"
				if i == len(values)-1 {
					prefix = "└─"
				}
				rv.lines = append(rv.lines, fmt.Sprintf("  %s %s", prefix, value))
			}
		}
		rv.lines = append(rv.lines, "")
	}

	// Remove trailing empty line
	if len(rv.lines) > 0 && rv.lines[len(rv.lines)-1] == "" {
		rv.lines = rv.lines[:len(rv.lines)-1]
	}
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
