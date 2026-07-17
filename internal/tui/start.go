package tui

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/debug"
	"github.com/ericschmar/moribito/internal/ldap"
	"github.com/ericschmar/moribito/internal/ssh"
	zone "github.com/lrstanley/bubblezone"
)

// StartView provides the start page with configuration editing
type StartView struct {
	config       *config.Config
	configPath   string // Path to config file for saving changes
	width        int
	height       int
	cursor       int
	editing      bool
	editingField int
	textInput    textinput.Model // Text input for editing fields
	container    *ViewContainer

	// Connection management state
	connectionCursor        int             // Which saved connection is highlighted
	showNewConnectionDialog bool            // Whether to show new connection name dialog
	newConnInput            textinput.Model // Text input for new connection name

	// SSH host key error dialog
	showHostKeyDialog bool
	hostKeyDialogHost string

	// Error tracking
	saveError     error     // Last save error
	saveErrorTime time.Time // When the error occurred

	// Config validation warnings
	configWarnings     []string  // Warnings from config validation
	configWarningsTime time.Time // When warnings were captured
}

// Field indices for editing
const (
	// Connection management fields
	FieldConnectionHeader = iota
	FieldConnectionList
	FieldSaveConnection
	FieldDeleteConnection
	FieldConnectionSeparator

	// LDAP configuration fields
	FieldHost
	FieldPort
	FieldBaseDN
	FieldUseSSL
	FieldUseTLS
	FieldBindUser
	FieldBindPass
	FieldPageSize

	// SSH Tunnel fields
	FieldSSHTunnelSeparator
	FieldSSHTunnelHeader
	FieldSSHTunnelEnabled
	FieldSSHHost
	FieldSSHPort
	FieldSSHUser
	FieldSSHAuthMethod
	FieldSSHPassword
	FieldSSHKeyFile
	FieldSSHKeyPassphrase
	FieldSSHIgnoreHostKey

	FieldConnect
	FieldCount
)

// Field configuration
type fieldConfig struct {
	name        string
	placeholder string
	isBool      bool
	isPassword  bool
	isHeader    bool // For section headers
	isAction    bool // For clickable actions
	isSeparator bool // For visual separators
}

// Field configurations for display and editing
var fields = []fieldConfig{
	{name: "Connection Management", isHeader: true},
	{name: "Saved Connections", placeholder: "Select connection"},
	{name: "Save", isAction: true},
	{name: "Delete", isAction: true},
	{name: "", isSeparator: true},
	{name: "Host", placeholder: "ldap.example.com"},
	{name: "Port", placeholder: "389"},
	{name: "Base DN", placeholder: "dc=example,dc=com"},
	{name: "Use SSL", isBool: true},
	{name: "Use TLS", isBool: true},
	{name: "Bind User", placeholder: "cn=admin,dc=example,dc=com"},
	{name: "Bind Password", isPassword: true},
	{name: "Page Size", placeholder: "100"},
	// SSH Tunnel section
	{name: "", isSeparator: true},
	{name: "SSH Tunnel", isHeader: true},
	{name: "SSH Enabled", isBool: true},
	{name: "SSH Host", placeholder: "bastion.example.com"},
	{name: "SSH Port", placeholder: "22"},
	{name: "SSH User", placeholder: "ops"},
	{name: "Auth Method", placeholder: "password|key|agent"},
	{name: "SSH Password", isPassword: true},
	{name: "Key File", placeholder: "~/.ssh/id_ed25519"},
	{name: "Key Passphrase", isPassword: true},
	{name: "Ignore Host Key", isBool: true},
	{name: "Connect", isAction: true},
}

// isFieldVisible returns whether a field should be rendered and navigable.
// SSH sub-fields are hidden when the tunnel is disabled; jump sub-fields
// are hidden when no jump host is configured.
func (sv *StartView) isFieldVisible(field int) bool {
	switch field {
	case FieldSSHHost, FieldSSHPort, FieldSSHUser, FieldSSHAuthMethod,
		FieldSSHPassword, FieldSSHKeyFile, FieldSSHKeyPassphrase,
		FieldSSHIgnoreHostKey:
		if !sv.config.LDAP.SSHTunnel.Enabled {
			return false
		}
	}
	return true
}

// Define consistent styles
var (
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

	// New styles for connection management
	actionStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("10")).
			Bold(true).
			Padding(0, 1)

	selectedActionStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Background(lipgloss.Color("10")).
				Bold(true).
				Padding(0, 1)

	headerStyle2 = lipgloss.NewStyle().
			Foreground(lipgloss.Color("13")).
			Bold(true).
			Underline(true).
			Margin(1, 0, 0, 0)

	separatorStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("8")).
			Margin(0, 0)

	selectedConnectionStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("15")).
				Background(lipgloss.Color("12")).
				Bold(true).
				Padding(0, 1)

	errorStyle = lipgloss.NewStyle().
			Foreground(lipgloss.Color("196")). // Bright red
			Bold(true).
			Margin(1, 0, 0, 0)
)

// NewStartView creates a new start view
// Deprecated: Use NewStartViewWithConfigPath instead to ensure config persistence
func NewStartView(cfg *config.Config) *StartView {
	// Try to get default config path to enable saving
	defaultPath := config.GetDefaultConfigPath()

	// Create text inputs
	ti := textinput.New()
	ti.Placeholder = ""
	ti.CharLimit = 256
	ti.Width = 50

	newConnInput := textinput.New()
	newConnInput.Placeholder = "Connection name"
	newConnInput.CharLimit = 64
	newConnInput.Width = 40

	sv := &StartView{
		config:       cfg,
		configPath:   defaultPath,
		cursor:       0,
		textInput:    ti,
		newConnInput: newConnInput,
	}

	return sv
}

// NewStartViewWithConfigPath creates a new start view with config path for saving
func NewStartViewWithConfigPath(cfg *config.Config, configPath string) *StartView {
	// Create text inputs
	ti := textinput.New()
	ti.Placeholder = ""
	ti.CharLimit = 256
	ti.Width = 50

	newConnInput := textinput.New()
	newConnInput.Placeholder = "Connection name"
	newConnInput.CharLimit = 64
	newConnInput.Width = 40

	sv := &StartView{
		config:       cfg,
		configPath:   configPath,
		cursor:       0,
		textInput:    ti,
		newConnInput: newConnInput,
	}

	// Validate config and capture any warnings
	if warnings := cfg.ValidateAndRepair(); len(warnings) > 0 {
		sv.configWarnings = warnings
		sv.configWarningsTime = time.Now()
	}

	return sv
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
	case SSHTunnelHostKeyMsg:
		sv.showHostKeyDialog = true
		sv.hostKeyDialogHost = msg.Host
		return sv, nil

	case tea.KeyMsg:
		if sv.showHostKeyDialog {
			sv.showHostKeyDialog = false
			return sv, nil
		}

		if sv.showNewConnectionDialog {
			return sv.handleNewConnectionDialog(msg)
		}

		if sv.editing {
			return sv.handleEditMode(msg)
		}

		switch msg.String() {
		case "up", "k":
			for next := sv.cursor - 1; next >= 0; next-- {
				if sv.isFieldVisible(next) {
					sv.cursor = next
					break
				}
			}
		case "down", "j":
			for next := sv.cursor + 1; next < FieldCount; next++ {
				if sv.isFieldVisible(next) {
					sv.cursor = next
					break
				}
			}
		case "left", "h":
			// Handle connection list navigation
			if sv.cursor == FieldConnectionList && len(sv.config.LDAP.SavedConnections) > 0 {
				if sv.connectionCursor > 0 {
					sv.connectionCursor--
				}
			}
		case "right", "l":
			// Handle connection list navigation
			if sv.cursor == FieldConnectionList && len(sv.config.LDAP.SavedConnections) > 0 {
				if sv.connectionCursor < len(sv.config.LDAP.SavedConnections)-1 {
					sv.connectionCursor++
				}
			}
		case "enter":
			debug.Log("tui/start: enter pressed, cursor=%d (%s), editing=%v", sv.cursor, fields[sv.cursor].name, sv.editing)
			return sv.handleFieldAction()
		}
	}

	return sv, nil
}

// getFieldValue gets the current value for a field
func (sv *StartView) getFieldValue(field int) string {
	switch field {
	case FieldConnectionHeader:
		return "Connection Management"
	case FieldConnectionList:
		if len(sv.config.LDAP.SavedConnections) == 0 {
			return "No saved connections"
		}
		activeConn := sv.config.GetActiveConnection()
		return fmt.Sprintf("Current: %s", activeConn.Name)
	case FieldSaveConnection:
		return "Save"
	case FieldDeleteConnection:
		return "Delete"
	case FieldConnectionSeparator:
		return "────────────────────────"
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
	case FieldSSHTunnelSeparator:
		return "────────────────────────"
	case FieldSSHTunnelHeader:
		return "SSH Tunnel"
	case FieldSSHTunnelEnabled:
		return strconv.FormatBool(sv.config.LDAP.SSHTunnel.Enabled)
	case FieldSSHHost:
		return sv.config.LDAP.SSHTunnel.Host
	case FieldSSHPort:
		port := sv.config.LDAP.SSHTunnel.Port
		if port == 0 {
			return ""
		}
		return strconv.Itoa(port)
	case FieldSSHUser:
		return sv.config.LDAP.SSHTunnel.User
	case FieldSSHAuthMethod:
		return sv.config.LDAP.SSHTunnel.AuthMethod
	case FieldSSHPassword:
		return sv.config.LDAP.SSHTunnel.Password
	case FieldSSHKeyFile:
		return sv.config.LDAP.SSHTunnel.KeyFile
	case FieldSSHKeyPassphrase:
		return sv.config.LDAP.SSHTunnel.KeyPassphrase
	case FieldSSHIgnoreHostKey:
		return strconv.FormatBool(sv.config.LDAP.SSHTunnel.InsecureIgnoreHostKey)
	case FieldConnect:
		return "Connect to LDAP"
	}
	return ""
}

// getDisplayValue gets the display value for a field
func (sv *StartView) getDisplayValue(field int) string {
	value := sv.getFieldValue(field)

	// Handle special display cases
	switch field {
	case FieldConnectionHeader:
		return value
	case FieldConnectionList:
		// Show current connection and list of saved connections
		if len(sv.config.LDAP.SavedConnections) == 0 {
			return placeholderStyle.Render("No saved connections (using default)")
		}
		return sv.renderConnectionList()
	case FieldSaveConnection, FieldDeleteConnection, FieldConnect:
		return value
	case FieldConnectionSeparator:
		return separatorStyle.Render(value)
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

	// Show host key error dialog if active
	if sv.showHostKeyDialog {
		return sv.renderHostKeyDialog()
	}

	// Show new connection dialog if active
	if sv.showNewConnectionDialog {
		return sv.renderNewConnectionDialog()
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
		if !sv.isFieldVisible(i) {
			continue
		}
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

	// Handle special field types
	if fieldCfg.isHeader {
		return sv.renderHeaderField(field)
	}
	if fieldCfg.isSeparator {
		return sv.renderSeparatorField(field)
	}
	if field == FieldConnectionList {
		return sv.renderConnectionListField(isSelected)
	}

	// Render field label (only for non-action fields)
	var fieldLine string
	if fieldCfg.isAction {
		// Action fields don't have labels, just the action text
		valueContent := sv.getDisplayValue(field)
		if isSelected {
			fieldLine = selectedActionStyle.Render(valueContent)
		} else {
			fieldLine = actionStyle.Render(valueContent)
		}
	} else {
		// Regular fields with labels
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
		fieldLine = lipgloss.JoinHorizontal(lipgloss.Top, label, " ", styledValue)
	}

	// Add clickable zone only for interactive fields
	if !fieldCfg.isHeader && !fieldCfg.isSeparator {
		zoneID := fmt.Sprintf("config-field-%d", field)
		fieldLine = zone.Mark(zoneID, fieldLine)
	}

	return fieldLine
}

// renderHeaderField renders a header field
func (sv *StartView) renderHeaderField(field int) string {
	value := sv.getFieldValue(field)
	headerText := headerStyle2.Render(value)

	// Add config path for connection management header
	if field == FieldConnectionHeader && sv.configPath != "" {
		configPathText := placeholderStyle.Render(fmt.Sprintf("  Config: %s", sv.configPath))
		return headerText + "\n" + configPathText
	} else if field == FieldConnectionHeader && sv.configPath == "" {
		warningText := errorStyle.Render("  ⚠ Config file not set - changes will not persist")
		return headerText + "\n" + warningText
	}

	return headerText
}

// renderSeparatorField renders a separator field
func (sv *StartView) renderSeparatorField(field int) string {
	value := sv.getFieldValue(field)
	return separatorStyle.Render(value)
}

// renderConnectionListField renders the connection list field
func (sv *StartView) renderConnectionListField(isSelected bool) string {
	content := sv.renderConnectionList()

	if isSelected {
		return selectedFieldStyle.Render(content)
	}
	return fieldValueStyle.Render(content)
}

// renderConnectionList renders the list of saved connections
func (sv *StartView) renderConnectionList() string {
	if len(sv.config.LDAP.SavedConnections) == 0 {
		return "No saved connections (using default)"
	}

	var lines []string
	activeConn := sv.config.GetActiveConnection()
	lines = append(lines, fmt.Sprintf("Current: %s (%s)", activeConn.Name, activeConn.Host))
	lines = append(lines, "")
	lines = append(lines, "Saved connections:")

	for i, conn := range sv.config.LDAP.SavedConnections {
		indicator := "  "
		if i == sv.connectionCursor && sv.cursor == FieldConnectionList {
			indicator = "▶ "
		} else if i == sv.config.LDAP.SelectedConnection {
			indicator = "● "
		}

		connLine := fmt.Sprintf("%s%s (%s)", indicator, conn.Name, conn.Host)
		if i == sv.connectionCursor && sv.cursor == FieldConnectionList {
			connLine = selectedConnectionStyle.Render(connLine)
		}
		lines = append(lines, connLine)
	}

	return strings.Join(lines, "\n")
}

// renderEditingField renders the field currently being edited
func (sv *StartView) renderEditingField() string {
	// For boolean fields, show toggle instructions instead of text input
	if fields[sv.editingField].isBool {
		currentValue := sv.getFieldValue(sv.editingField)
		return fmt.Sprintf("%s (press Space/Y/N to toggle)", currentValue)
	}
	// Use textinput view for regular fields
	return sv.textInput.View()
}

// renderInstructions renders the instruction text
func (sv *StartView) renderInstructions() string {
	var parts []string

	// Show error message if there is one and it's recent (within last 5 seconds)
	if sv.saveError != nil && time.Since(sv.saveErrorTime) < 5*time.Second {
		errorMsg := fmt.Sprintf("⚠ %s", sv.saveError.Error())
		parts = append(parts, errorStyle.Render(errorMsg))
	}

	// Show regular instructions
	var instructions string
	if sv.editing {
		if fields[sv.editingField].isBool {
			instructions = "Press [Space] to toggle • [Y/N] or [T/F] to set • [Enter] or [Esc] to finish"
		} else {
			instructions = "Press [Enter] to save • [Esc] to cancel • Arrow keys to navigate • Cmd+V to paste"
		}
	} else {
		instructions = "Press [↑↓] or [j/k] to navigate • [Enter] to edit/select • [←→] or [h/l] for connections • [1-4] to switch views"
	}
	parts = append(parts, instructionStyle.Render(instructions))

	return strings.Join(parts, "\n")
}

// renderNewConnectionDialog renders the dialog for creating a new connection
func (sv *StartView) renderNewConnectionDialog() string {
	content := strings.Join([]string{
		"New Connection",
		"",
		"Enter connection name:",
		sv.newConnInput.View(),
		"",
		"Press [Enter] to save • [Esc] to cancel",
	}, "\n")

	style := lipgloss.NewStyle().
		Align(lipgloss.Center).
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("0")).
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("12")).
		Padding(1, 2).
		Width(40)

	return sv.container.RenderCentered(style.Render(content))
}

// renderHostKeyDialog renders an error dialog when the SSH host key is unknown
func (sv *StartView) renderHostKeyDialog() string {
	content := strings.Join([]string{
		"SSH Host Key Unknown",
		"",
		fmt.Sprintf("The host key for '%s'", sv.hostKeyDialogHost),
		"is not in your known_hosts file.",
		"",
		"To add it, run:",
		"",
		fmt.Sprintf("  ssh-keyscan %s >> ~/.ssh/known_hosts", sv.hostKeyDialogHost),
		"",
		"Or enable 'Ignore Host Key' in the SSH",
		"tunnel settings to skip verification.",
		"",
		"Press any key to dismiss.",
	}, "\n")

	style := lipgloss.NewStyle().
		Align(lipgloss.Left).
		Foreground(lipgloss.Color("15")).
		Background(lipgloss.Color("0")).
		Border(lipgloss.RoundedBorder()).
		BorderForeground(lipgloss.Color("9")).
		Padding(1, 2).
		Width(52)

	return sv.container.RenderCentered(style.Render(content))
}

// IsEditing returns true if the start view is currently in editing mode
func (sv *StartView) IsEditing() bool {
	return sv.editing || sv.showNewConnectionDialog || sv.showHostKeyDialog
}

// handleEditMode handles input when editing a configuration value
func (sv *StartView) handleEditMode(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	// Handle boolean fields differently - use toggles instead of text input
	if fields[sv.editingField].isBool {
		switch msg.String() {
		case "enter", "esc":
			sv.editing = false
			return sv, nil
		case " ", "y", "n", "t", "f", "1", "0":
			// Toggle or set boolean value
			currentValue := sv.getFieldValue(sv.editingField)
			var newValue bool
			switch strings.ToLower(msg.String()) {
			case " ":
				// Toggle current value
				newValue = currentValue != "true"
			case "y", "t", "1":
				newValue = true
			case "n", "f", "0":
				newValue = false
			}

			// Update the config value directly
			switch sv.editingField {
			case FieldUseSSL:
				sv.config.LDAP.UseSSL = newValue
			case FieldUseTLS:
				sv.config.LDAP.UseTLS = newValue
			case FieldSSHTunnelEnabled:
				sv.config.LDAP.SSHTunnel.Enabled = newValue
			case FieldSSHIgnoreHostKey:
				sv.config.LDAP.SSHTunnel.InsecureIgnoreHostKey = newValue
			}

			// Save the configuration to disk
			sv.saveConfigToDisk()
		}
		return sv, nil
	}

	// Handle regular fields with textinput
	switch msg.String() {
	case "enter":
		sv.saveValue()
		sv.editing = false
		return sv, nil

	case "esc":
		sv.editing = false
		return sv, nil

	default:
		// Delegate to textinput for all other key handling
		var cmd tea.Cmd
		sv.textInput, cmd = sv.textInput.Update(msg)
		return sv, cmd
	}
}

// saveValue saves the edited value to the config
func (sv *StartView) saveValue() {
	inputValue := sv.textInput.Value()

	switch sv.editingField {
	case FieldHost:
		sv.config.LDAP.Host = inputValue
	case FieldPort:
		if port, err := strconv.Atoi(inputValue); err == nil && port > 0 && port < 65536 {
			sv.config.LDAP.Port = port
		}
	case FieldBaseDN:
		sv.config.LDAP.BaseDN = inputValue
	case FieldUseSSL:
		if useSSL, err := strconv.ParseBool(inputValue); err == nil {
			sv.config.LDAP.UseSSL = useSSL
		}
	case FieldUseTLS:
		if useTLS, err := strconv.ParseBool(inputValue); err == nil {
			sv.config.LDAP.UseTLS = useTLS
		}
	case FieldBindUser:
		sv.config.LDAP.BindUser = inputValue
	case FieldBindPass:
		sv.config.LDAP.BindPass = inputValue
	case FieldPageSize:
		if pageSize, err := strconv.Atoi(inputValue); err == nil && pageSize > 0 {
			sv.config.Pagination.PageSize = uint32(pageSize)
		}
	case FieldSSHTunnelEnabled:
		if enabled, err := strconv.ParseBool(inputValue); err == nil {
			sv.config.LDAP.SSHTunnel.Enabled = enabled
		}
	case FieldSSHHost:
		sv.config.LDAP.SSHTunnel.Host = inputValue
	case FieldSSHPort:
		if port, err := strconv.Atoi(inputValue); err == nil && port > 0 && port < 65536 {
			sv.config.LDAP.SSHTunnel.Port = port
		}
	case FieldSSHUser:
		sv.config.LDAP.SSHTunnel.User = inputValue
	case FieldSSHAuthMethod:
		sv.config.LDAP.SSHTunnel.AuthMethod = inputValue
	case FieldSSHPassword:
		sv.config.LDAP.SSHTunnel.Password = inputValue
	case FieldSSHKeyFile:
		sv.config.LDAP.SSHTunnel.KeyFile = inputValue
	case FieldSSHKeyPassphrase:
		sv.config.LDAP.SSHTunnel.KeyPassphrase = inputValue
	case FieldSSHIgnoreHostKey:
		if v, err := strconv.ParseBool(inputValue); err == nil {
			sv.config.LDAP.SSHTunnel.InsecureIgnoreHostKey = v
		}
	}

	// Save the configuration to disk
	sv.saveConfigToDisk()
}

// saveConfigToDisk saves the current configuration to the config file
func (sv *StartView) saveConfigToDisk() {
	if sv.configPath == "" {
		sv.saveError = fmt.Errorf("no config file path set - changes will not persist")
		sv.saveErrorTime = time.Now()
		return
	}

	if err := sv.config.Save(sv.configPath); err != nil {
		sv.saveError = fmt.Errorf("failed to save config: %w", err)
		sv.saveErrorTime = time.Now()
	} else {
		// Clear any previous errors on successful save
		sv.saveError = nil
		sv.saveErrorTime = time.Time{}
	}
}

// handleFieldAction handles enter key press on different field types
func (sv *StartView) handleFieldAction() (tea.Model, tea.Cmd) {
	fieldCfg := fields[sv.cursor]
	debug.Log("tui/start: handleFieldAction cursor=%d name=%q isAction=%v isHeader=%v isSeparator=%v", sv.cursor, fieldCfg.name, fieldCfg.isAction, fieldCfg.isHeader, fieldCfg.isSeparator)

	switch sv.cursor {
	case FieldConnectionList:
		// Select the highlighted connection
		if len(sv.config.LDAP.SavedConnections) > 0 && sv.connectionCursor < len(sv.config.LDAP.SavedConnections) {
			sv.config.SetActiveConnection(sv.connectionCursor)
			sv.saveConfigToDisk()
		}
		return sv, nil

	case FieldSaveConnection:
		// Save current settings to the currently selected connection
		if len(sv.config.LDAP.SavedConnections) > 0 && sv.config.LDAP.SelectedConnection >= 0 && sv.config.LDAP.SelectedConnection < len(sv.config.LDAP.SavedConnections) {
			// Update the currently selected saved connection with current settings
			updated := config.SavedConnection{
				Name:      sv.config.LDAP.SavedConnections[sv.config.LDAP.SelectedConnection].Name,
				Host:      sv.config.LDAP.Host,
				Port:      sv.config.LDAP.Port,
				BaseDN:    sv.config.LDAP.BaseDN,
				UseSSL:    sv.config.LDAP.UseSSL,
				UseTLS:    sv.config.LDAP.UseTLS,
				BindUser:  sv.config.LDAP.BindUser,
				BindPass:  sv.config.LDAP.BindPass,
				SSHTunnel: sv.config.LDAP.SSHTunnel,
			}
			sv.config.UpdateSavedConnection(sv.config.LDAP.SelectedConnection, updated)
			sv.saveConfigToDisk()
		} else {
			// No saved connection selected, create a new one
			sv.showNewConnectionDialog = true
			sv.newConnInput.SetValue("")
			sv.newConnInput.Focus()
		}
		return sv, nil

	case FieldDeleteConnection:
		// Delete the currently selected saved connection
		if len(sv.config.LDAP.SavedConnections) > 0 && sv.connectionCursor < len(sv.config.LDAP.SavedConnections) {
			sv.config.RemoveSavedConnection(sv.connectionCursor)
			if sv.connectionCursor >= len(sv.config.LDAP.SavedConnections) && len(sv.config.LDAP.SavedConnections) > 0 {
				sv.connectionCursor = len(sv.config.LDAP.SavedConnections) - 1
			}
			sv.saveConfigToDisk()
		}
		return sv, nil

	case FieldConnect:
		debug.Log("tui/start: FieldConnect action triggered")
		sv.saveConfigToDisk()
		return sv.handleConnect()

	default:
		debug.Log("tui/start: handleFieldAction default branch — starting edit for cursor=%d", sv.cursor)
		if !fieldCfg.isHeader && !fieldCfg.isSeparator && !fieldCfg.isAction {
			sv.editing = true
			sv.editingField = sv.cursor

			// Initialize textinput with current value
			sv.textInput.SetValue(sv.getFieldValue(sv.cursor))

			// Configure textinput for password fields
			if fieldCfg.isPassword {
				sv.textInput.EchoMode = textinput.EchoPassword
				sv.textInput.EchoCharacter = '*'
			} else {
				sv.textInput.EchoMode = textinput.EchoNormal
			}

			// Set placeholder for the field
			sv.textInput.Placeholder = fieldCfg.placeholder

			// Focus the textinput
			sv.textInput.Focus()
		}
		return sv, nil
	}
}

// handleNewConnectionDialog handles input for the new connection name dialog
func (sv *StartView) handleNewConnectionDialog(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "enter":
		connName := sv.newConnInput.Value()
		if connName != "" {
			// Create new connection from current settings
			newConn := config.SavedConnection{
				Name:      connName,
				Host:      sv.config.LDAP.Host,
				Port:      sv.config.LDAP.Port,
				BaseDN:    sv.config.LDAP.BaseDN,
				UseSSL:    sv.config.LDAP.UseSSL,
				UseTLS:    sv.config.LDAP.UseTLS,
				BindUser:  sv.config.LDAP.BindUser,
				BindPass:  sv.config.LDAP.BindPass,
				SSHTunnel: sv.config.LDAP.SSHTunnel,
			}
			sv.config.AddSavedConnection(newConn)

			// Set as active connection
			sv.config.SetActiveConnection(len(sv.config.LDAP.SavedConnections) - 1)
			sv.connectionCursor = len(sv.config.LDAP.SavedConnections) - 1

			// Save the configuration to disk
			sv.saveConfigToDisk()
		}
		sv.showNewConnectionDialog = false
		return sv, nil

	case "esc":
		sv.showNewConnectionDialog = false
		return sv, nil

	default:
		// Delegate to textinput for all other key handling
		var cmd tea.Cmd
		sv.newConnInput, cmd = sv.newConnInput.Update(msg)
		return sv, cmd
	}
}

// handleConnect attempts to create an LDAP connection with current settings
func (sv *StartView) handleConnect() (tea.Model, tea.Cmd) {
	activeConn := sv.config.GetActiveConnection()

	// Validate required fields
	if activeConn.Host == "" {
		return sv, func() tea.Msg {
			return StatusMsg{Message: "Error: LDAP host is required"}
		}
	}
	if activeConn.BaseDN == "" {
		return sv, func() tea.Msg {
			return StatusMsg{Message: "Error: Base DN is required"}
		}
	}

	debug.Log("tui/connect: host=%s port=%d baseDN=%s user=%s ssl=%v tls=%v sshEnabled=%v",
		activeConn.Host, activeConn.Port, activeConn.BaseDN, activeConn.BindUser,
		activeConn.UseSSL, activeConn.UseTLS, activeConn.SSHTunnel.Enabled)

	// Return command that will attempt connection in background
	return sv, func() tea.Msg {
		var tunnel *ssh.Tunnel
		ldapHost := activeConn.Host
		ldapPort := activeConn.Port

		// Establish SSH tunnel if enabled
		if activeConn.SSHTunnel.Enabled {
			debug.Log("tui/connect: starting SSH tunnel to %s:%d (auth=%s key=%q)",
				activeConn.SSHTunnel.Host, activeConn.SSHTunnel.Port,
				activeConn.SSHTunnel.AuthMethod, activeConn.SSHTunnel.KeyFile)
			tunnelCfg := ssh.TunnelConfig{
				SSHHost:               activeConn.SSHTunnel.Host,
				SSHPort:               activeConn.SSHTunnel.Port,
				SSHUser:               activeConn.SSHTunnel.User,
				AuthMethod:            activeConn.SSHTunnel.AuthMethod,
				Password:              activeConn.SSHTunnel.Password,
				KeyFile:               activeConn.SSHTunnel.KeyFile,
				KeyPassphrase:         activeConn.SSHTunnel.KeyPassphrase,
				InsecureIgnoreHostKey: activeConn.SSHTunnel.InsecureIgnoreHostKey,
				RemoteHost:            activeConn.Host,
				RemotePort:            activeConn.Port,
			}

			var err error
			tunnel, err = ssh.NewTunnel(tunnelCfg)
			if err != nil {
				debug.Log("tui/connect: SSH tunnel failed: %v", err)
				var hkErr *ssh.HostKeyUnknownError
				if errors.As(err, &hkErr) {
					return SSHTunnelHostKeyMsg{Host: hkErr.Host}
				}
				return StatusMsg{Message: fmt.Sprintf("SSH tunnel failed: %v", err)}
			}
			ldapHost = "127.0.0.1"
			ldapPort = tunnel.LocalPort()
			debug.Log("tui/connect: SSH tunnel ready, local addr %s:%d", ldapHost, ldapPort)
		}

		// Create LDAP configuration
		ldapConfig := ldap.Config{
			Host:           ldapHost,
			Port:           ldapPort,
			BaseDN:         activeConn.BaseDN,
			UseSSL:         activeConn.UseSSL,
			UseTLS:         activeConn.UseTLS,
			BindUser:       activeConn.BindUser,
			BindPass:       activeConn.BindPass,
			RetryEnabled:   sv.config.Retry.Enabled,
			MaxRetries:     sv.config.Retry.MaxAttempts,
			InitialDelayMs: sv.config.Retry.InitialDelayMs,
			MaxDelayMs:     sv.config.Retry.MaxDelayMs,
		}
		debug.Log("tui/connect: dialing LDAP %s:%d", ldapConfig.Host, ldapConfig.Port)

		// Create channel to receive result or timeout
		resultChan := make(chan struct {
			client *ldap.Client
			err    error
		}, 1)

		// Start connection attempt in goroutine
		go func() {
			client, err := ldap.NewClient(ldapConfig)
			debug.Log("tui/connect: ldap.NewClient returned err=%v", err)
			resultChan <- struct {
				client *ldap.Client
				err    error
			}{client, err}
		}()

		timeout := time.Duration(sv.config.Retry.ConnectTimeoutSeconds) * time.Second
		ctx, cancel := context.WithTimeout(context.Background(), timeout)
		defer cancel()

		select {
		case result := <-resultChan:
			if result.err != nil {
				if tunnel != nil {
					tunnel.Close() //nolint:errcheck
				}
				debug.Log("tui/connect: connection failed: %v", result.err)
				return StatusMsg{Message: fmt.Sprintf("Connection failed: %v", result.err)}
			}
			debug.Log("tui/connect: connected successfully")
			return ConnectMsg{
				Client: result.client,
				Config: sv.config,
				Tunnel: tunnel,
			}
		case <-ctx.Done():
			if tunnel != nil {
				tunnel.Close() //nolint:errcheck
			}
			debug.Log("tui/connect: timed out after 5 seconds")
			return StatusMsg{Message: fmt.Sprintf("Connection timeout after %d seconds", sv.config.Retry.ConnectTimeoutSeconds)}
		}
	}
}
