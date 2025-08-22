package tui

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/atotto/clipboard"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/config"
	zone "github.com/lrstanley/bubblezone"
)

// StartView provides the start page with configuration editing
type StartView struct {
	config       *config.Config
	width        int
	height       int
	cursor       int
	editing      bool
	editingField int
	inputValue   string
	container    *ViewContainer
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

// Field configuration
type fieldConfig struct {
	name        string
	placeholder string
	isBool      bool
	isPassword  bool
}

// Field configurations for display and editing
var fields = []fieldConfig{
	{name: "Host", placeholder: "ldap.example.com"},
	{name: "Port", placeholder: "389"},
	{name: "Base DN", placeholder: "dc=example,dc=com"},
	{name: "Use SSL", isBool: true},
	{name: "Use TLS", isBool: true},
	{name: "Bind User", placeholder: "cn=admin,dc=example,dc=com"},
	{name: "Bind Password", isPassword: true},
	{name: "Page Size", placeholder: "100"},
}

// Define consistent styles
var (
	titleStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("15")).
			Background(lipgloss.Color("12")).
			Bold(true).
			Align(lipgloss.Center).
			Padding(1, 2).
			Margin(0, 0, 1, 0)

	headerStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("14")).
			Bold(true).
			Margin(0, 0, 1, 0)

	fieldLabelStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("11")).
			Bold(true).
			Width(15).
			Align(lipgloss.Right)

	fieldValueStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("15")).
			Padding(0, 1)

	selectedFieldStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Background(lipgloss.Color(lipgloss.Color(GetGradientColor(0.5)))).
				Bold(true).
				Padding(0, 1)

	editingFieldStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("0")).
				Background(lipgloss.Color("11")).
				Bold(true).
				Padding(0, 1)

	placeholderStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("8")).
				Italic(true)

	instructionStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("8")).
				Italic(true).
				Margin(1, 0, 0, 0)

	containerStyle = lipgloss.NewStyle().
				Border(lipgloss.RoundedBorder()).
				BorderForeground(lipgloss.Color("6")).
				Padding(0, 2).
				Margin(0, 0)
)

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
	sv.container = NewViewContainer(width, height)
}

// Update handles input for the start view
func (sv *StartView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyMsg:
		if sv.editing {
			return sv.handleEditMode(msg)
		}

		switch msg.String() {
		case "up", "k":
			if sv.cursor > 0 {
				sv.cursor--
			}
		case "down", "j":
			if sv.cursor < FieldCount-1 {
				sv.cursor++
			}
		case "enter":
			sv.editing = true
			sv.editingField = sv.cursor
			sv.inputValue = sv.getFieldValue(sv.cursor)
		}
	}

	return sv, nil
}

// getFieldValue gets the current value for a field
func (sv *StartView) getFieldValue(field int) string {
	switch field {
	case FieldHost:
		return sv.config.LDAP.Host
	case FieldPort:
		return strconv.Itoa(sv.config.LDAP.Port)
	case FieldBaseDN:
		return sv.config.LDAP.BaseDN
	case FieldUseSSL:
		return strconv.FormatBool(sv.config.LDAP.UseSSL)
	case FieldUseTLS:
		return strconv.FormatBool(sv.config.LDAP.UseTLS)
	case FieldBindUser:
		return sv.config.LDAP.BindUser
	case FieldBindPass:
		return sv.config.LDAP.BindPass
	case FieldPageSize:
		return strconv.Itoa(int(sv.config.Pagination.PageSize))
	}
	return ""
}

// getDisplayValue gets the display value for a field
func (sv *StartView) getDisplayValue(field int) string {
	value := sv.getFieldValue(field)

	// Handle special display cases
	switch field {
	case FieldBindPass:
		if value == "" {
			return placeholderStyle.Render("[not set]")
		}
		return "********"
	case FieldHost, FieldBaseDN, FieldBindUser:
		if value == "" {
			return placeholderStyle.Render("[not set]")
		}
		return value
	default:
		return value
	}
}

// View renders the start view
func (sv *StartView) View() string {
	if sv.container == nil {
		sv.container = NewViewContainer(sv.width, sv.height)
	}

	contentWidth, _ := sv.container.GetContentDimensions()

	// For very narrow screens, show simplified view
	if contentWidth < 40 {
		return sv.renderNarrowView()
	}

	return sv.container.RenderWithPadding(sv.renderConfigPane(contentWidth))
}

// renderNarrowView renders a simplified view for narrow screens
func (sv *StartView) renderNarrowView() string {
	content := strings.Join([]string{
		"LDAP CLI",
		"",
		"Screen too narrow.",
		"Please resize terminal.",
		"",
		"Press [1-4] to switch views",
	}, "\n")

	style := lipgloss.NewStyle().
		Align(lipgloss.Center).
		Foreground(lipgloss.Color("15")).
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("8")).
		Padding(1, 2)

	return sv.container.RenderCentered(style.Render(content))
}

// renderConfigPane creates the main configuration interface
func (sv *StartView) renderConfigPane(maxWidth int) string {
	var sections []string

	// Header description
	header := headerStyle.Render("Configure your LDAP connection settings:")
	sections = append(sections, header)

	// Configuration fields
	fieldLines := sv.renderConfigFields()
	sections = append(sections, fieldLines)

	// Instructions
	instructions := sv.renderInstructions()
	sections = append(sections, instructions)

	// Join all sections
	content := strings.Join(sections, "\n")

	// Apply container styling
	return containerStyle.Width(maxWidth).Render(content)
}

// renderConfigFields renders all configuration fields
func (sv *StartView) renderConfigFields() string {
	var fieldLines []string

	for i := 0; i < FieldCount; i++ {
		fieldLine := sv.renderField(i)
		fieldLines = append(fieldLines, fieldLine)
	}

	return strings.Join(fieldLines, "\n")
}

// renderField renders a single configuration field
func (sv *StartView) renderField(field int) string {
	isSelected := field == sv.cursor
	isEditing := sv.editing && sv.editingField == field

	// Get field configuration
	fieldCfg := fields[field]

	// Render field label
	label := fieldLabelStyle.Render(fieldCfg.name + ":")

	// Render field value
	var valueContent string
	if isEditing {
		valueContent = sv.renderEditingField()
	} else {
		valueContent = sv.getDisplayValue(field)
	}

	// Apply appropriate styling to the value
	var styledValue string
	if isEditing {
		styledValue = editingFieldStyle.Render(valueContent)
	} else if isSelected {
		styledValue = selectedFieldStyle.Render(valueContent)
	} else {
		styledValue = fieldValueStyle.Render(valueContent)
	}

	// Create field line with proper spacing
	fieldLine := lipgloss.JoinHorizontal(lipgloss.Top, label, " ", styledValue)

	// Add clickable zone
	zoneID := fmt.Sprintf("config-field-%d", field)
	fieldLine = zone.Mark(zoneID, fieldLine)

	return fieldLine
}

// renderEditingField renders the field currently being edited
func (sv *StartView) renderEditingField() string {
	// Show input value with cursor
	return sv.inputValue + "█"
}

// renderInstructions renders the instruction text
func (sv *StartView) renderInstructions() string {
	var instructions string

	if sv.editing {
		instructions = "Press [Enter] to save • [Esc] to cancel • [Ctrl+V] to paste"
		if fields[sv.editingField].isBool {
			instructions += " • [Y/N] or [T/F] for boolean values"
		}
	} else {
		instructions = "Press [↑↓] or [j/k] to navigate • [Enter] to edit • [1-4] to switch views"
	}

	return instructionStyle.Render(instructions)
}

// handleEditMode handles input when editing a configuration value
func (sv *StartView) handleEditMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "enter":
		sv.saveValue()
		sv.editing = false
		sv.inputValue = ""
		return sv, nil

	case "esc":
		sv.editing = false
		sv.inputValue = ""
		return sv, nil

	case "backspace":
		if len(sv.inputValue) > 0 {
			sv.inputValue = sv.inputValue[:len(sv.inputValue)-1]
		}

	case "ctrl+v":
		if clipboardText, err := clipboard.ReadAll(); err == nil {
			sv.inputValue += clipboardText
		}

	default:
		// Handle boolean fields with shortcuts
		if fields[sv.editingField].isBool {
			switch strings.ToLower(msg.String()) {
			case "y", "t", "1":
				sv.inputValue = "true"
			case "n", "f", "0":
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

// saveValue saves the edited value to the config
func (sv *StartView) saveValue() {
	switch sv.editingField {
	case FieldHost:
		sv.config.LDAP.Host = sv.inputValue
	case FieldPort:
		if port, err := strconv.Atoi(sv.inputValue); err == nil && port > 0 && port < 65536 {
			sv.config.LDAP.Port = port
		}
	case FieldBaseDN:
		sv.config.LDAP.BaseDN = sv.inputValue
	case FieldUseSSL:
		if useSSL, err := strconv.ParseBool(sv.inputValue); err == nil {
			sv.config.LDAP.UseSSL = useSSL
		}
	case FieldUseTLS:
		if useTLS, err := strconv.ParseBool(sv.inputValue); err == nil {
			sv.config.LDAP.UseTLS = useTLS
		}
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
