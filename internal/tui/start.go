package tui

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/config"
)

// StartView provides the start page with ASCII art and config editing
type StartView struct {
	config       *config.Config
	width        int
	height       int
	cursor       int
	editing      bool
	editingField int
	inputValue   string
}

// Field indices for editing
const (
	FieldHost = iota
	FieldPort
	FieldBaseDN
	FieldUseSSL
	FieldUseTLS
	FieldBindUser
	FieldBindPass
	FieldPageSize
	FieldCount
)

// Field names for display
var fieldNames = []string{
	"Host",
	"Port",
	"Base DN",
	"Use SSL",
	"Use TLS",
	"Bind User",
	"Bind Password",
	"Page Size",
}

// NewStartView creates a new start view
func NewStartView(cfg *config.Config) *StartView {
	return &StartView{
		config: cfg,
		cursor: 0,
	}
}

// Init initializes the start view
func (sv *StartView) Init() tea.Cmd {
	return nil
}

// SetSize sets the size of the start view
func (sv *StartView) SetSize(width, height int) {
	sv.width = width
	sv.height = height
}

// Update handles messages for the start view
func (sv *StartView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		if sv.editing {
			return sv.handleEditMode(msg)
		} else {
			return sv.handleBrowseMode(msg)
		}
	}
	return sv, nil
}

// handleBrowseMode handles input when browsing configuration options
func (sv *StartView) handleBrowseMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "up", "k":
		if sv.cursor > 0 {
			sv.cursor--
		}
	case "down", "j":
		if sv.cursor < FieldCount-1 {
			sv.cursor++
		}
	case "enter", " ":
		sv.editing = true
		sv.editingField = sv.cursor
		sv.inputValue = sv.getCurrentFieldValue()
		return sv, nil
	}
	return sv, nil
}

// handleEditMode handles input when editing a configuration value
func (sv *StartView) handleEditMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "enter":
		sv.saveCurrentField()
		sv.editing = false
		return sv, nil
	case "escape":
		sv.editing = false
		sv.inputValue = ""
		return sv, nil
	case "backspace":
		if len(sv.inputValue) > 0 {
			sv.inputValue = sv.inputValue[:len(sv.inputValue)-1]
		}
	case "ctrl+u":
		sv.inputValue = ""
	default:
		// Handle special keys for boolean fields
		if sv.editingField == FieldUseSSL || sv.editingField == FieldUseTLS {
			if msg.String() == "y" || msg.String() == "Y" || msg.String() == "t" || msg.String() == "T" {
				sv.inputValue = "true"
			} else if msg.String() == "n" || msg.String() == "N" || msg.String() == "f" || msg.String() == "F" {
				sv.inputValue = "false"
			}
		} else {
			// Handle regular character input
			if len(msg.String()) == 1 && msg.String() >= " " {
				sv.inputValue += msg.String()
			}
		}
	}
	return sv, nil
}

// getCurrentFieldValue returns the current value of the selected field
func (sv *StartView) getCurrentFieldValue() string {
	switch sv.editingField {
	case FieldHost:
		return sv.config.LDAP.Host
	case FieldPort:
		return strconv.Itoa(sv.config.LDAP.Port)
	case FieldBaseDN:
		return sv.config.LDAP.BaseDN
	case FieldUseSSL:
		return fmt.Sprintf("%t", sv.config.LDAP.UseSSL)
	case FieldUseTLS:
		return fmt.Sprintf("%t", sv.config.LDAP.UseTLS)
	case FieldBindUser:
		return sv.config.LDAP.BindUser
	case FieldBindPass:
		return sv.config.LDAP.BindPass
	case FieldPageSize:
		return strconv.Itoa(int(sv.config.Pagination.PageSize))
	}
	return ""
}

// saveCurrentField saves the edited value back to config
func (sv *StartView) saveCurrentField() {
	switch sv.editingField {
	case FieldHost:
		sv.config.LDAP.Host = sv.inputValue
	case FieldPort:
		if port, err := strconv.Atoi(sv.inputValue); err == nil {
			sv.config.LDAP.Port = port
		}
	case FieldBaseDN:
		sv.config.LDAP.BaseDN = sv.inputValue
	case FieldUseSSL:
		sv.config.LDAP.UseSSL = strings.ToLower(sv.inputValue) == "true"
	case FieldUseTLS:
		sv.config.LDAP.UseTLS = strings.ToLower(sv.inputValue) == "true"
	case FieldBindUser:
		sv.config.LDAP.BindUser = sv.inputValue
	case FieldBindPass:
		sv.config.LDAP.BindPass = sv.inputValue
	case FieldPageSize:
		if pageSize, err := strconv.Atoi(sv.inputValue); err == nil && pageSize > 0 {
			sv.config.Pagination.PageSize = uint32(pageSize)
		}
	}
}

// View renders the start view
func (sv *StartView) View() string {
	if sv.width < 80 {
		// For narrow screens, show a simple message
		return sv.renderNarrowView()
	}

	leftWidth := sv.width / 2
	rightWidth := sv.width - leftWidth

	// Create the ASCII art on the left
	leftContent := sv.renderParthenon(leftWidth)

	// Create the config editor on the right
	rightContent := sv.renderConfigEditor(rightWidth)

	// Combine left and right panels
	leftLines := strings.Split(leftContent, "\n")
	rightLines := strings.Split(rightContent, "\n")

	// Make sure both sides have the same number of lines
	maxLines := len(leftLines)
	if len(rightLines) > maxLines {
		maxLines = len(rightLines)
	}

	// Pad shorter side with empty lines
	for len(leftLines) < maxLines {
		leftLines = append(leftLines, "")
	}
	for len(rightLines) < maxLines {
		rightLines = append(rightLines, "")
	}

	// Create the combined view
	var result strings.Builder
	for i := 0; i < maxLines && i < sv.height; i++ {
		leftLine := leftLines[i]
		if len(leftLine) > leftWidth-2 {
			leftLine = leftLine[:leftWidth-2]
		}
		leftLine = fmt.Sprintf("%-*s", leftWidth-1, leftLine)

		rightLine := rightLines[i]
		if len(rightLine) > rightWidth-2 {
			rightLine = rightLine[:rightWidth-2]
		}

		result.WriteString(leftLine + "│" + rightLine)
		if i < maxLines-1 {
			result.WriteString("\n")
		}
	}

	return result.String()
}

// renderNarrowView renders a simplified view for narrow screens
func (sv *StartView) renderNarrowView() string {
	style := lipgloss.NewStyle().
		Foreground(lipgloss.Color("14")).
		Bold(true).
		Padding(1, 2).
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("12"))

	content := "LDAP CLI Start Page\n\n"
	content += "Screen too narrow for split view.\n"
	content += "Please resize your terminal or switch to another view.\n\n"
	content += "Press [1] for Tree View, [2] for Record View, [3] for Query View"

	return style.Render(content)
}

// renderParthenon creates ASCII art of the Parthenon
func (sv *StartView) renderParthenon(width int) string {
	// Parthenon ASCII art with colors
	art := `
    ████████████████████████████████
   ███                            ███
  ██                                ██
 ██  █  █  █  █  █  █  █  █  █  █    ██
 ██                                  ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██  ║  ║  ║  ║  ║  ║  ║  ║  ║  ║   ██
 ██                                  ██
 ██████████████████████████████████████
████████████████████████████████████████
`

	// Style the art with colors
	artStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("11")). // Bright yellow
		Bold(true).
		Padding(1, 2)

	titleStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("13")). // Bright magenta
		Bold(true).
		Align(lipgloss.Center).
		Width(width-4).
		Padding(1, 0)

	borderStyle := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("10")). // Bright green
		Padding(0, 1)

	title := titleStyle.Render("🏛️  THE PARTHENON  🏛️")
	styledArt := artStyle.Render(strings.TrimSpace(art))

	content := title + "\n\n" + styledArt + "\n\n"
	content += lipgloss.NewStyle().
		Foreground(lipgloss.Color("14")).
		Italic(true).
		Align(lipgloss.Center).
		Width(width - 4).
		Render("\"Excellence is never an accident. It is always the result of high intention,\nsincere effort, and intelligent execution; it represents the wise choice\nof many alternatives.\" - Aristotle")

	return borderStyle.Render(content)
}

// renderConfigEditor creates the configuration editing interface
func (sv *StartView) renderConfigEditor(width int) string {
	var content strings.Builder

	// Title
	titleStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("12")).
		Bold(true).
		Align(lipgloss.Center).
		Width(width-4).
		Padding(1, 0)

	content.WriteString(titleStyle.Render("🔧 LDAP Configuration 🔧"))
	content.WriteString("\n\n")

	// Configuration fields
	for i := 0; i < FieldCount; i++ {
		isSelected := i == sv.cursor
		isEditing := sv.editing && sv.editingField == i

		fieldValue := sv.getDisplayValue(i)

		var style lipgloss.Style
		if isEditing {
			// Editing style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color("0")).
				Background(lipgloss.Color("11")).
				Bold(true).
				Padding(0, 1)
			fieldValue = sv.inputValue + "█" // Show cursor
		} else if isSelected {
			// Selected but not editing style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Background(lipgloss.Color("4")).
				Bold(true).
				Padding(0, 1)
		} else {
			// Normal style
			style = lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Padding(0, 1)
		}

		// Format field
		fieldName := fmt.Sprintf("%-12s:", fieldNames[i])
		fieldLine := fmt.Sprintf("%s %s", fieldName, fieldValue)

		content.WriteString(style.Render(fieldLine))
		content.WriteString("\n")
	}

	// Instructions
	content.WriteString("\n")
	instructionStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("8")).
		Italic(true)

	if sv.editing {
		content.WriteString(instructionStyle.Render("Press [Enter] to save, [Esc] to cancel"))
	} else {
		content.WriteString(instructionStyle.Render("Press [↑↓] to navigate, [Enter] to edit"))
	}

	// Add border
	borderStyle := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("9")). // Bright red
		Padding(1, 1).
		Height(sv.height - 2)

	return borderStyle.Render(content.String())
}

// getDisplayValue returns the display value for a field
func (sv *StartView) getDisplayValue(fieldIndex int) string {
	switch fieldIndex {
	case FieldHost:
		return sv.config.LDAP.Host
	case FieldPort:
		return strconv.Itoa(sv.config.LDAP.Port)
	case FieldBaseDN:
		return sv.config.LDAP.BaseDN
	case FieldUseSSL:
		return fmt.Sprintf("%t", sv.config.LDAP.UseSSL)
	case FieldUseTLS:
		return fmt.Sprintf("%t", sv.config.LDAP.UseTLS)
	case FieldBindUser:
		if sv.config.LDAP.BindUser == "" {
			return "[empty]"
		}
		return sv.config.LDAP.BindUser
	case FieldBindPass:
		if sv.config.LDAP.BindPass == "" {
			return "[empty]"
		}
		return strings.Repeat("*", len(sv.config.LDAP.BindPass))
	case FieldPageSize:
		return strconv.Itoa(int(sv.config.Pagination.PageSize))
	}
	return ""
}
