package config

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"gopkg.in/yaml.v3"
)

func TestPaginationConfig(t *testing.T) {
	// Test default configuration includes pagination
	cfg := Default()

	if cfg.Pagination.PageSize != 50 {
		t.Errorf("Expected default page size to be 50, got %d", cfg.Pagination.PageSize)
	}
}

func TestRetryConfig(t *testing.T) {
	// Test default configuration includes retry settings
	cfg := Default()

	if !cfg.Retry.Enabled {
		t.Errorf("Expected retry to be enabled by default")
	}

	if cfg.Retry.MaxAttempts != 3 {
		t.Errorf("Expected default max attempts to be 3, got %d", cfg.Retry.MaxAttempts)
	}

	if cfg.Retry.InitialDelayMs != 500 {
		t.Errorf("Expected default initial delay to be 500ms, got %d", cfg.Retry.InitialDelayMs)
	}

	if cfg.Retry.MaxDelayMs != 5000 {
		t.Errorf("Expected default max delay to be 5000ms, got %d", cfg.Retry.MaxDelayMs)
	}
}

func TestConfigLoadWithPagination(t *testing.T) {
	// Create a temporary config file
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
  use_ssl: false
  use_tls: false
pagination:
  page_size: 25
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check pagination settings
	if cfg.Pagination.PageSize != 25 {
		t.Errorf("Expected page size to be 25, got %d", cfg.Pagination.PageSize)
	}

	// Check LDAP settings
	if cfg.LDAP.Host != "test.example.com" {
		t.Errorf("Expected host to be test.example.com, got %s", cfg.LDAP.Host)
	}
}

func TestConfigLoadWithoutPagination(t *testing.T) {
	// Create a temporary config file without pagination section
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check that default pagination is applied
	if cfg.Pagination.PageSize != 50 {
		t.Errorf("Expected default page size to be 50, got %d", cfg.Pagination.PageSize)
	}
}

func TestConfigLoadWithRetry(t *testing.T) {
	// Create a temporary config file with retry settings
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
  use_ssl: false
  use_tls: false
pagination:
  page_size: 25
retry:
  enabled: true
  max_attempts: 5
  initial_delay_ms: 1000
  max_delay_ms: 10000
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check retry settings
	if !cfg.Retry.Enabled {
		t.Errorf("Expected retry to be enabled")
	}

	if cfg.Retry.MaxAttempts != 5 {
		t.Errorf("Expected max attempts to be 5, got %d", cfg.Retry.MaxAttempts)
	}

	if cfg.Retry.InitialDelayMs != 1000 {
		t.Errorf("Expected initial delay to be 1000ms, got %d", cfg.Retry.InitialDelayMs)
	}

	if cfg.Retry.MaxDelayMs != 10000 {
		t.Errorf("Expected max delay to be 10000ms, got %d", cfg.Retry.MaxDelayMs)
	}

	// Check pagination settings
	if cfg.Pagination.PageSize != 25 {
		t.Errorf("Expected page size to be 25, got %d", cfg.Pagination.PageSize)
	}

	// Check LDAP settings
	if cfg.LDAP.Host != "test.example.com" {
		t.Errorf("Expected host to be test.example.com, got %s", cfg.LDAP.Host)
	}
}

func TestConfigLoadWithoutRetry(t *testing.T) {
	// Create a temporary config file without retry section
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
pagination:
  page_size: 30
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check that default retry settings are applied
	if !cfg.Retry.Enabled {
		t.Errorf("Expected retry to be enabled by default")
	}

	if cfg.Retry.MaxAttempts != 3 {
		t.Errorf("Expected default max attempts to be 3, got %d", cfg.Retry.MaxAttempts)
	}

	if cfg.Retry.InitialDelayMs != 500 {
		t.Errorf("Expected default initial delay to be 500ms, got %d", cfg.Retry.InitialDelayMs)
	}

	if cfg.Retry.MaxDelayMs != 5000 {
		t.Errorf("Expected default max delay to be 5000ms, got %d", cfg.Retry.MaxDelayMs)
	}
}

func TestGetDefaultConfigPath(t *testing.T) {
// Test that GetDefaultConfigPath returns a non-empty path
path := GetDefaultConfigPath()
if path == "" {
t.Error("Expected non-empty default config path")
}

// Test that the path is OS-appropriate
switch runtime.GOOS {
case "windows":
if !filepath.IsAbs(path) {
t.Error("Expected absolute path on Windows")
}
case "darwin":
if !strings.Contains(path, ".moribito") {
t.Error("Expected macOS path to contain .moribito")
}
default:
if !strings.Contains(path, ".config") {
t.Error("Expected Unix path to contain .config")
}
}
}

func TestGetOSSpecificConfigPaths(t *testing.T) {
paths := getOSSpecificConfigPaths()

if len(paths) == 0 {
t.Error("Expected non-empty list of OS-specific config paths")
}

// Check that paths contain moribito
foundMoribito := false
for _, path := range paths {
if strings.Contains(path, "moribito") {
foundMoribito = true
break
}
}
if !foundMoribito {
t.Error("Expected at least one path to contain 'moribito'")
}
}

func TestCreateDefaultConfigCore(t *testing.T) {
// Create a temporary directory for testing
tempDir, err := os.MkdirTemp("", "moribito-config-test")
if err != nil {
t.Fatalf("Failed to create temp dir: %v", err)
}
defer os.RemoveAll(tempDir)

// Test creating a config in a specific directory
testConfigPath := filepath.Join(tempDir, "test-config.yaml")

// Create directory
if err := os.MkdirAll(filepath.Dir(testConfigPath), 0755); err != nil {
t.Fatalf("Failed to create config directory: %v", err)
}

// Create default config content
config := Default()
data, err := yaml.Marshal(config)
if err != nil {
t.Fatalf("Failed to marshal default config: %v", err)
}

// Write config file
header := fmt.Sprintf("# Moribito Configuration\n# Created at: %s\n# Edit this file with your LDAP server details\n\n", testConfigPath)
configContent := header + string(data)

if err := os.WriteFile(testConfigPath, []byte(configContent), 0644); err != nil {
t.Fatalf("Failed to write config file: %v", err)
}

// Verify the file was created and can be loaded
if _, err := os.Stat(testConfigPath); err != nil {
t.Errorf("Config file was not created: %v", err)
}

// Try to load the created config
cfg, err := Load(testConfigPath)
if err != nil {
t.Errorf("Failed to load created config: %v", err)
}

if cfg == nil {
t.Error("Loaded config is nil")
}
}
