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

// LDAPConfig contains LDAP connection settings
type LDAPConfig struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	BaseDN   string `yaml:"base_dn"`
	UseSSL   bool   `yaml:"use_ssl"`
	UseTLS   bool   `yaml:"use_tls"`
	BindUser string `yaml:"bind_user"`
	BindPass string `yaml:"bind_pass"`
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

// Load loads configuration from a YAML file
func Load(configPath string) (*Config, error) {
	// If no config path provided, look for default locations
	if configPath == "" {
		configPath = findConfigFile()
	}

	data, err := os.ReadFile(configPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read config file %s: %w", configPath, err)
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse config file %s: %w", configPath, err)
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

	return &config, nil
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
