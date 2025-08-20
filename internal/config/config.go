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

	return &config, nil
}

// findConfigFile looks for configuration files in standard locations
func findConfigFile() string {
	// Check current directory first
	candidates := []string{
		"./config.yaml",
		"./config.yml",
		"./ldap-cli.yaml",
		"./ldap-cli.yml",
	}

	// Check home directory
	if homeDir, err := os.UserHomeDir(); err == nil {
		candidates = append(candidates,
			filepath.Join(homeDir, ".ldap-cli.yaml"),
			filepath.Join(homeDir, ".ldap-cli.yml"),
			filepath.Join(homeDir, ".config", "ldap-cli", "config.yaml"),
			filepath.Join(homeDir, ".config", "ldap-cli", "config.yml"),
		)
	}

	// Check /etc directory on Unix systems
	candidates = append(candidates,
		"/etc/ldap-cli/config.yaml",
		"/etc/ldap-cli/config.yml",
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
	}
}
