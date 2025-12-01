package config

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"

	"gopkg.in/yaml.v3"
)

// Config represents the LDAP CLI configuration
type Config struct {
	LDAP       LDAPConfig       `yaml:"ldap"`
	Pagination PaginationConfig `yaml:"pagination"`
	Retry      RetryConfig      `yaml:"retry"`
}

// SavedConnection represents a single saved LDAP connection profile
type SavedConnection struct {
	Name     string `yaml:"name"`
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	BaseDN   string `yaml:"base_dn"`
	UseSSL   bool   `yaml:"use_ssl"`
	UseTLS   bool   `yaml:"use_tls"`
	BindUser string `yaml:"bind_user"`
	BindPass string `yaml:"bind_pass"`
}

// LDAPConfig contains LDAP connection settings
type LDAPConfig struct {
	// Current/default connection settings (for backward compatibility)
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	BaseDN   string `yaml:"base_dn"`
	UseSSL   bool   `yaml:"use_ssl"`
	UseTLS   bool   `yaml:"use_tls"`
	BindUser string `yaml:"bind_user"`
	BindPass string `yaml:"bind_pass"`

	// Multiple saved connections (new feature)
	SavedConnections   []SavedConnection `yaml:"saved_connections,omitempty"`
	SelectedConnection int               `yaml:"selected_connection,omitempty"` // Index into SavedConnections, -1 means use default
}

// PaginationConfig contains pagination settings
type PaginationConfig struct {
	PageSize uint32 `yaml:"page_size"`
}

// RetryConfig contains retry settings for LDAP operations
type RetryConfig struct {
	MaxAttempts    int  `yaml:"max_attempts"`     // Maximum number of retry attempts
	InitialDelayMs int  `yaml:"initial_delay_ms"` // Initial delay in milliseconds
	MaxDelayMs     int  `yaml:"max_delay_ms"`     // Maximum delay in milliseconds
	Enabled        bool `yaml:"enabled"`          // Whether retries are enabled
}

// Load loads configuration from a YAML file and returns the config and actual path used
func Load(configPath string) (*Config, string, error) {
	// If no config path provided, look for default locations
	if configPath == "" {
		configPath = findConfigFile()
	}

	data, err := os.ReadFile(configPath)
	if err != nil {
		return nil, "", fmt.Errorf("failed to read config file %s: %w", configPath, err)
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, "", fmt.Errorf("failed to parse config file %s: %w", configPath, err)
	}

	// Set defaults
	if config.LDAP.Port == 0 {
		if config.LDAP.UseSSL {
			config.LDAP.Port = 636
		} else {
			config.LDAP.Port = 389
		}
	}

	// Set pagination defaults
	if config.Pagination.PageSize == 0 {
		config.Pagination.PageSize = 50
	}

	// Set retry defaults
	if !config.Retry.Enabled && config.Retry.MaxAttempts == 0 && config.Retry.InitialDelayMs == 0 {
		// If retry section is completely unset, enable with defaults
		config.Retry.Enabled = true
		config.Retry.MaxAttempts = 3
		config.Retry.InitialDelayMs = 500
		config.Retry.MaxDelayMs = 5000
	}
	if config.Retry.MaxAttempts <= 0 {
		config.Retry.MaxAttempts = 3
	}
	if config.Retry.InitialDelayMs <= 0 {
		config.Retry.InitialDelayMs = 500
	}
	if config.Retry.MaxDelayMs <= 0 {
		config.Retry.MaxDelayMs = 5000
	}

	return &config, configPath, nil
}

// GetActiveConnection returns the currently active LDAP connection settings
func (c *Config) GetActiveConnection() LDAPConnection {
	// If no saved connections or selected connection is -1, use default
	if len(c.LDAP.SavedConnections) == 0 || c.LDAP.SelectedConnection < 0 {
		return LDAPConnection{
			Name:     "Default",
			Host:     c.LDAP.Host,
			Port:     c.LDAP.Port,
			BaseDN:   c.LDAP.BaseDN,
			UseSSL:   c.LDAP.UseSSL,
			UseTLS:   c.LDAP.UseTLS,
			BindUser: c.LDAP.BindUser,
			BindPass: c.LDAP.BindPass,
		}
	}

	// Validate selected connection index
	if c.LDAP.SelectedConnection >= len(c.LDAP.SavedConnections) {
		c.LDAP.SelectedConnection = 0
	}

	saved := c.LDAP.SavedConnections[c.LDAP.SelectedConnection]
	return LDAPConnection{
		Name:     saved.Name,
		Host:     saved.Host,
		Port:     saved.Port,
		BaseDN:   saved.BaseDN,
		UseSSL:   saved.UseSSL,
		UseTLS:   saved.UseTLS,
		BindUser: saved.BindUser,
		BindPass: saved.BindPass,
	}
}

// LDAPConnection represents the active connection settings
type LDAPConnection struct {
	Name     string
	Host     string
	Port     int
	BaseDN   string
	UseSSL   bool
	UseTLS   bool
	BindUser string
	BindPass string
}

// SetActiveConnection updates the current connection settings from a saved connection
func (c *Config) SetActiveConnection(index int) {
	if index < 0 || index >= len(c.LDAP.SavedConnections) {
		c.LDAP.SelectedConnection = -1 // Use default
		return
	}

	c.LDAP.SelectedConnection = index
	saved := c.LDAP.SavedConnections[index]

	// Update the default fields to match the selected connection
	c.LDAP.Host = saved.Host
	c.LDAP.Port = saved.Port
	c.LDAP.BaseDN = saved.BaseDN
	c.LDAP.UseSSL = saved.UseSSL
	c.LDAP.UseTLS = saved.UseTLS
	c.LDAP.BindUser = saved.BindUser
	c.LDAP.BindPass = saved.BindPass
}

// AddSavedConnection adds a new saved connection
func (c *Config) AddSavedConnection(conn SavedConnection) {
	c.LDAP.SavedConnections = append(c.LDAP.SavedConnections, conn)
}

// RemoveSavedConnection removes a saved connection by index
func (c *Config) RemoveSavedConnection(index int) {
	if index < 0 || index >= len(c.LDAP.SavedConnections) {
		return
	}

	// If we're removing the currently selected connection, reset to default
	if c.LDAP.SelectedConnection == index {
		c.LDAP.SelectedConnection = -1
	} else if c.LDAP.SelectedConnection > index {
		// Adjust index if we removed a connection before the selected one
		c.LDAP.SelectedConnection--
	}

	// Remove the connection
	c.LDAP.SavedConnections = append(
		c.LDAP.SavedConnections[:index],
		c.LDAP.SavedConnections[index+1:]...,
	)
}

// UpdateSavedConnection updates a saved connection by index
func (c *Config) UpdateSavedConnection(index int, conn SavedConnection) {
	if index < 0 || index >= len(c.LDAP.SavedConnections) {
		return
	}

	c.LDAP.SavedConnections[index] = conn

	// If this is the currently selected connection, update the active settings
	if c.LDAP.SelectedConnection == index {
		c.SetActiveConnection(index)
	}
}

// ValidateAndRepair checks the config for issues and repairs them, returning warnings
func (c *Config) ValidateAndRepair() []string {
	var warnings []string

	// Check if selected connection index is out of bounds
	if len(c.LDAP.SavedConnections) > 0 && c.LDAP.SelectedConnection >= len(c.LDAP.SavedConnections) {
		oldIndex := c.LDAP.SelectedConnection
		c.LDAP.SelectedConnection = 0
		warnings = append(warnings, fmt.Sprintf("Selected connection index %d was invalid (only %d connections exist). Reset to first connection.", oldIndex, len(c.LDAP.SavedConnections)))
	}

	return warnings
}

// findConfigFile looks for configuration files in standard locations
func findConfigFile() string {
	// Check current directory first
	candidates := []string{
		"./config.yaml",
		"./config.yml",
		"./moribito.yaml", // New format
		"./moribito.yml",  // New format
		"./ldap-cli.yaml", // Legacy support
		"./ldap-cli.yml",  // Legacy support
	}

	// Add OS-specific and home directory paths
	candidates = append(candidates, getOSSpecificConfigPaths()...)

	for _, candidate := range candidates {
		if _, err := os.Stat(candidate); err == nil {
			return candidate
		}
	}

	return ""
}

// getOSSpecificConfigPaths returns configuration file paths based on OS conventions
func getOSSpecificConfigPaths() []string {
	var candidates []string

	homeDir, err := os.UserHomeDir()
	if err != nil {
		return candidates
	}

	switch runtime.GOOS {
	case "windows":
		// Windows: Use APPDATA for user-specific config
		if appData := os.Getenv("APPDATA"); appData != "" {
			candidates = append(candidates,
				filepath.Join(appData, "moribito", "config.yaml"),
				filepath.Join(appData, "moribito", "config.yml"),
			)
		}
		// Fallback to user profile directory
		candidates = append(candidates,
			filepath.Join(homeDir, ".moribito.yaml"),
			filepath.Join(homeDir, ".moribito.yml"),
		)

	case "darwin":
		// macOS: Prefer ~/.moribito/ as requested in the issue, with fallbacks
		candidates = append(candidates,
			filepath.Join(homeDir, ".moribito", "config.yaml"), // Primary choice as per issue
			filepath.Join(homeDir, ".moribito", "config.yml"),
			filepath.Join(homeDir, "Library", "Application Support", "moribito", "config.yaml"), // macOS standard
			filepath.Join(homeDir, "Library", "Application Support", "moribito", "config.yml"),
			filepath.Join(homeDir, ".moribito.yaml"), // Fallback
			filepath.Join(homeDir, ".moribito.yml"),
			filepath.Join(homeDir, ".config", "moribito", "config.yaml"), // XDG fallback
			filepath.Join(homeDir, ".config", "moribito", "config.yml"),
		)

	default:
		// Linux and other Unix-like systems: XDG Base Directory Specification
		xdgConfigHome := os.Getenv("XDG_CONFIG_HOME")
		if xdgConfigHome == "" {
			xdgConfigHome = filepath.Join(homeDir, ".config")
		}

		candidates = append(candidates,
			filepath.Join(xdgConfigHome, "moribito", "config.yaml"), // XDG standard
			filepath.Join(xdgConfigHome, "moribito", "config.yml"),
			filepath.Join(homeDir, ".moribito", "config.yaml"), // Also support directory approach
			filepath.Join(homeDir, ".moribito", "config.yml"),
			filepath.Join(homeDir, ".moribito.yaml"), // Fallback
			filepath.Join(homeDir, ".moribito.yml"),
		)

		// System-wide config for Unix systems
		candidates = append(candidates,
			"/etc/moribito/config.yaml",
			"/etc/moribito/config.yml",
		)
	}

	// Add legacy support for all platforms
	candidates = append(candidates,
		filepath.Join(homeDir, ".ldap-cli.yaml"),
		filepath.Join(homeDir, ".ldap-cli.yml"),
	)

	// Add legacy XDG support for Unix-like systems
	if runtime.GOOS != "windows" {
		xdgConfigHome := os.Getenv("XDG_CONFIG_HOME")
		if xdgConfigHome == "" {
			xdgConfigHome = filepath.Join(homeDir, ".config")
		}
		candidates = append(candidates,
			filepath.Join(xdgConfigHome, "ldap-cli", "config.yaml"),
			filepath.Join(xdgConfigHome, "ldap-cli", "config.yml"),
		)

		if runtime.GOOS != "darwin" {
			// System-wide legacy support for Linux
			candidates = append(candidates,
				"/etc/ldap-cli/config.yaml",
				"/etc/ldap-cli/config.yml",
			)
		}
	}

	return candidates
}

// GetDefaultConfigPath returns the preferred config file path for the current OS
func GetDefaultConfigPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return "./config.yaml" // Fallback to current directory
	}

	switch runtime.GOOS {
	case "windows":
		if appData := os.Getenv("APPDATA"); appData != "" {
			return filepath.Join(appData, "moribito", "config.yaml")
		}
		return filepath.Join(homeDir, ".moribito.yaml")
	case "darwin":
		return filepath.Join(homeDir, ".moribito", "config.yaml")
	default:
		xdgConfigHome := os.Getenv("XDG_CONFIG_HOME")
		if xdgConfigHome == "" {
			xdgConfigHome = filepath.Join(homeDir, ".config")
		}
		return filepath.Join(xdgConfigHome, "moribito", "config.yaml")
	}
}

// CreateDefaultConfig creates a default configuration file at the OS-appropriate location
func CreateDefaultConfig() error {
	configPath := GetDefaultConfigPath()

	// Create directory if it doesn't exist
	configDir := filepath.Dir(configPath)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory %s: %w", configDir, err)
	}

	// Check if config file already exists
	if _, err := os.Stat(configPath); err == nil {
		return fmt.Errorf("configuration file already exists at %s", configPath)
	}

	// Create sample configuration
	config := Default()
	data, err := yaml.Marshal(config)
	if err != nil {
		return fmt.Errorf("failed to marshal default config: %w", err)
	}

	// Add header comment
	header := fmt.Sprintf("# Moribito Configuration\n# Created at: %s\n# Edit this file with your LDAP server details\n\n", configPath)
	configContent := header + string(data)

	if err := os.WriteFile(configPath, []byte(configContent), 0644); err != nil {
		return fmt.Errorf("failed to write config file %s: %w", configPath, err)
	}

	return nil
}

// Save saves the configuration to a file
func (c *Config) Save(configPath string) error {
	// If no config path provided, use the default location
	if configPath == "" {
		configPath = GetDefaultConfigPath()
	}

	// Create directory if it doesn't exist
	configDir := filepath.Dir(configPath)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory %s: %w", configDir, err)
	}

	// Marshal the config to YAML
	data, err := yaml.Marshal(c)
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	// Add header comment
	header := fmt.Sprintf("# Moribito Configuration\n# Last updated: %s\n\n", filepath.Base(configPath))
	configContent := header + string(data)

	if err := os.WriteFile(configPath, []byte(configContent), 0644); err != nil {
		return fmt.Errorf("failed to write config file %s: %w", configPath, err)
	}

	return nil
}

// Default returns a default configuration
func Default() *Config {
	return &Config{
		LDAP: LDAPConfig{
			Host:   "localhost",
			Port:   389,
			BaseDN: "dc=example,dc=com",
			UseSSL: false,
			UseTLS: false,
		},
		Pagination: PaginationConfig{
			PageSize: 50,
		},
		Retry: RetryConfig{
			Enabled:        true,
			MaxAttempts:    3,
			InitialDelayMs: 500,
			MaxDelayMs:     5000,
		},
	}
}
