package config

import (
	"fmt"
	"os"
	"path/filepath"

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

	// Check home directory
	if homeDir, err := os.UserHomeDir(); err == nil {
		candidates = append(candidates,
			filepath.Join(homeDir, ".moribito.yaml"),                     // New format
			filepath.Join(homeDir, ".moribito.yml"),                      // New format
			filepath.Join(homeDir, ".config", "moribito", "config.yaml"), // New format
			filepath.Join(homeDir, ".config", "moribito", "config.yml"),  // New format
			filepath.Join(homeDir, ".ldap-cli.yaml"),                     // Legacy support
			filepath.Join(homeDir, ".ldap-cli.yml"),                      // Legacy support
			filepath.Join(homeDir, ".config", "ldap-cli", "config.yaml"), // Legacy support
			filepath.Join(homeDir, ".config", "ldap-cli", "config.yml"),  // Legacy support
		)
	}

	// Check /etc directory on Unix systems
	candidates = append(candidates,
		"/etc/moribito/config.yaml", // New format
		"/etc/moribito/config.yml",  // New format
		"/etc/ldap-cli/config.yaml", // Legacy support
		"/etc/ldap-cli/config.yml",  // Legacy support
	)

	for _, candidate := range candidates {
		if _, err := os.Stat(candidate); err == nil {
			return candidate
		}
	}

	return ""
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
