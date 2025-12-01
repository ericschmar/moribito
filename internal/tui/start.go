package tui

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/atotto/clipboard"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

// StartView provides the start page with configuration editing
type StartView struct {
	config     *config.Config
	configPath string // Path to config file for saving changes
	width      int
	height     int
	cursor     int
	editing    bool
	editingField int
	inputValue string
	container  *ViewContainer

	// Connection management state
	connectionCursor        int    // Which saved connection is highlighted
	showNewConnectionDialog bool   // Whether to show new connection name dialog
	newConnectionName       string // Name for new connection being created

	// Error tracking
	saveError     error     // Last save error
	saveErrorTime time.Time // When the error occurred

	// Config warnings
	configWarnings     []string  // Config validation warnings
	configWarningsTime time.Time // When warnings were generated
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
	{name: "Connect", isAction: true},
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

	connectionListStyle = lipgloss.NewStyle().
				Foreground(lipgloss.Color("7")).
				Padding(0, 2)

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

	sv := &StartView{
		config:     cfg,
		configPath: defaultPath,
		cursor:     0,
	}

	// Validate config and store warnings
	sv.configWarnings = cfg.ValidateAndRepair()
	if len(sv.configWarnings) > 0 {
		sv.configWarningsTime = time.Now()
	}

	return sv
}

// NewStartViewWithConfigPath creates a new start view with config path for saving
func NewStartViewWithConfigPath(cfg *config.Config, configPath string) *StartView {
	sv := &StartView{
		config:     cfg,
		configPath: configPath,
		cursor:     0,
	}

	// Validate config and store warnings
	sv.configWarnings = cfg.ValidateAndRepair()
	if len(sv.configWarnings) > 0 {
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
	case tea.KeyMsg:
		if sv.showNewConnectionDialog {
			return sv.handleNewConnectionDialog(msg)
		}

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
	return headerStyle2.Render(value)
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
	// Show input value with cursor
	return sv.inputValue + "█"
}

// renderInstructions renders the instruction text
func (sv *StartView) renderInstructions() string {
	var parts []string

	// Show config warnings if they exist and are recent (within last 10 seconds)
	if len(sv.configWarnings) > 0 && time.Since(sv.configWarningsTime) < 10*time.Second {
		for _, warning := range sv.configWarnings {
			warningMsg := fmt.Sprintf("⚠ Config: %s", warning)
			parts = append(parts, errorStyle.Render(warningMsg))
		}
	}

	// Show error message if there is one and it's recent (within last 5 seconds)
	if sv.saveError != nil && time.Since(sv.saveErrorTime) < 5*time.Second {
		errorMsg := fmt.Sprintf("⚠ %s", sv.saveError.Error())
		parts = append(parts, errorStyle.Render(errorMsg))
	}

	// Show regular instructions
	var instructions string
	if sv.editing {
		instructions = "Press [Enter] to save • [Esc] to cancel • [Ctrl+V/Cmd+V] to paste"
		if fields[sv.editingField].isBool {
			instructions += " • [Y/N] or [T/F] for boolean values"
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
		sv.newConnectionName + "█",
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

// IsEditing returns true if the start view is currently in editing mode
func (sv *StartView) IsEditing() bool {
	return sv.editing || sv.showNewConnectionDialog
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

	case "ctrl+v", "cmd+v", "shift+insert", "insert":
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
				Name:     sv.config.LDAP.SavedConnections[sv.config.LDAP.SelectedConnection].Name,
				Host:     sv.config.LDAP.Host,
				Port:     sv.config.LDAP.Port,
				BaseDN:   sv.config.LDAP.BaseDN,
				UseSSL:   sv.config.LDAP.UseSSL,
				UseTLS:   sv.config.LDAP.UseTLS,
				BindUser: sv.config.LDAP.BindUser,
				BindPass: sv.config.LDAP.BindPass,
			}
			sv.config.UpdateSavedConnection(sv.config.LDAP.SelectedConnection, updated)
			sv.saveConfigToDisk()
		} else {
			// No saved connection selected, create a new one
			sv.showNewConnectionDialog = true
			sv.newConnectionName = ""
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
		// Save config before connecting to LDAP
		sv.saveConfigToDisk()
		// Attempt to connect to LDAP
		return sv.handleConnect()

	default:
		// For regular fields, start editing
		if !fieldCfg.isHeader && !fieldCfg.isSeparator && !fieldCfg.isAction {
			sv.editing = true
			sv.editingField = sv.cursor
			sv.inputValue = sv.getFieldValue(sv.cursor)
		}
		return sv, nil
	}
}

// handleNewConnectionDialog handles input for the new connection name dialog
func (sv *StartView) handleNewConnectionDialog(msg tea.KeyMsg) (tea.Model, tea.Cmd) {
	switch msg.String() {
	case "enter":
		if sv.newConnectionName != "" {
			// Create new connection from current settings
			newConn := config.SavedConnection{
				Name:     sv.newConnectionName,
				Host:     sv.config.LDAP.Host,
				Port:     sv.config.LDAP.Port,
				BaseDN:   sv.config.LDAP.BaseDN,
				UseSSL:   sv.config.LDAP.UseSSL,
				UseTLS:   sv.config.LDAP.UseTLS,
				BindUser: sv.config.LDAP.BindUser,
				BindPass: sv.config.LDAP.BindPass,
			}
			sv.config.AddSavedConnection(newConn)

			// Set as active connection
			sv.config.SetActiveConnection(len(sv.config.LDAP.SavedConnections) - 1)
			sv.connectionCursor = len(sv.config.LDAP.SavedConnections) - 1
			
			// Save the configuration to disk
			sv.saveConfigToDisk()
		}
		sv.showNewConnectionDialog = false
		sv.newConnectionName = ""
		return sv, nil

	case "esc":
		sv.showNewConnectionDialog = false
		sv.newConnectionName = ""
		return sv, nil

	case "backspace":
		if len(sv.newConnectionName) > 0 {
			sv.newConnectionName = sv.newConnectionName[:len(sv.newConnectionName)-1]
		}

	case "ctrl+v", "cmd+v", "shift+insert", "insert":
		if clipboardText, err := clipboard.ReadAll(); err == nil {
			sv.newConnectionName += clipboardText
		}

	default:
		// Handle regular character input
		if len(msg.String()) == 1 && msg.String() >= " " {
			sv.newConnectionName += msg.String()
		}
	}
	return sv, nil
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

	// Return command that will attempt connection in background
	return sv, func() tea.Msg {
		// Create LDAP configuration
		ldapConfig := ldap.Config{
			Host:           activeConn.Host,
			Port:           activeConn.Port,
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

		// Create channel to receive result or timeout
		resultChan := make(chan struct {
			client *ldap.Client
			err    error
		}, 1)

		// Start connection attempt in goroutine
		go func() {
			client, err := ldap.NewClient(ldapConfig)
			resultChan <- struct {
				client *ldap.Client
				err    error
			}{client, err}
		}()

		// Wait for result or timeout (5 seconds)
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		select {
		case result := <-resultChan:
			if result.err != nil {
				return StatusMsg{Message: fmt.Sprintf("Connection failed: %v", result.err)}
			}
			return ConnectMsg{
				Client: result.client,
				Config: sv.config,
			}
		case <-ctx.Done():
			return StatusMsg{Message: "Connection timeout after 5 seconds"}
		}
	}
}
