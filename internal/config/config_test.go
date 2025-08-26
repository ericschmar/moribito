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

func TestSavedConnections(t *testing.T) {
	cfg := Default()
	
	// Test initial state - no saved connections
	activeConn := cfg.GetActiveConnection()
	if activeConn.Name != "Default" {
		t.Errorf("Expected default connection name to be 'Default', got %s", activeConn.Name)
	}
	if activeConn.Host != "localhost" {
		t.Errorf("Expected default host to be 'localhost', got %s", activeConn.Host)
	}
	
	// Add a saved connection
	savedConn := SavedConnection{
		Name:     "Production",
		Host:     "ldap.prod.com",
		Port:     636,
		BaseDN:   "dc=prod,dc=com",
		UseSSL:   true,
		UseTLS:   false,
		BindUser: "cn=prodadmin,dc=prod,dc=com",
		BindPass: "prodpass",
	}
	cfg.AddSavedConnection(savedConn)
	
	if len(cfg.LDAP.SavedConnections) != 1 {
		t.Errorf("Expected 1 saved connection, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	// Test selecting the saved connection
	cfg.SetActiveConnection(0)
	activeConn = cfg.GetActiveConnection()
	if activeConn.Name != "Production" {
		t.Errorf("Expected active connection name to be 'Production', got %s", activeConn.Name)
	}
	if activeConn.Host != "ldap.prod.com" {
		t.Errorf("Expected active host to be 'ldap.prod.com', got %s", activeConn.Host)
	}
	if activeConn.Port != 636 {
		t.Errorf("Expected active port to be 636, got %d", activeConn.Port)
	}
	
	// Test that the default connection fields are updated
	if cfg.LDAP.Host != "ldap.prod.com" {
		t.Errorf("Expected LDAP.Host to be updated to 'ldap.prod.com', got %s", cfg.LDAP.Host)
	}
	
	// Test removing saved connection
	cfg.RemoveSavedConnection(0)
	if len(cfg.LDAP.SavedConnections) != 0 {
		t.Errorf("Expected 0 saved connections after removal, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	// Should fall back to default connection
	activeConn = cfg.GetActiveConnection()
	if activeConn.Name != "Default" {
		t.Errorf("Expected fallback to default connection, got %s", activeConn.Name)
	}
}

func TestMultipleSavedConnections(t *testing.T) {
	cfg := Default()
	
	// Add multiple connections
	conn1 := SavedConnection{
		Name:   "Development",
		Host:   "ldap.dev.com",
		Port:   389,
		BaseDN: "dc=dev,dc=com",
	}
	conn2 := SavedConnection{
		Name:   "Staging", 
		Host:   "ldap.staging.com",
		Port:   389,
		BaseDN: "dc=staging,dc=com",
	}
	conn3 := SavedConnection{
		Name:   "Production",
		Host:   "ldap.prod.com", 
		Port:   636,
		BaseDN: "dc=prod,dc=com",
		UseSSL: true,
	}
	
	cfg.AddSavedConnection(conn1)
	cfg.AddSavedConnection(conn2)
	cfg.AddSavedConnection(conn3)
	
	if len(cfg.LDAP.SavedConnections) != 3 {
		t.Errorf("Expected 3 saved connections, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	// Test selecting different connections
	cfg.SetActiveConnection(1) // Staging
	activeConn := cfg.GetActiveConnection()
	if activeConn.Name != "Staging" {
		t.Errorf("Expected active connection to be 'Staging', got %s", activeConn.Name)
	}
	if activeConn.Host != "ldap.staging.com" {
		t.Errorf("Expected active host to be 'ldap.staging.com', got %s", activeConn.Host)
	}
	
	// Test updating a saved connection
	updatedConn := SavedConnection{
		Name:   "Staging Updated",
		Host:   "ldap2.staging.com",
		Port:   636,
		BaseDN: "dc=staging,dc=com",
		UseSSL: true,
	}
	cfg.UpdateSavedConnection(1, updatedConn)
	
	if cfg.LDAP.SavedConnections[1].Name != "Staging Updated" {
		t.Errorf("Expected connection name to be updated to 'Staging Updated', got %s", cfg.LDAP.SavedConnections[1].Name)
	}
	if cfg.LDAP.SavedConnections[1].Host != "ldap2.staging.com" {
		t.Errorf("Expected connection host to be updated to 'ldap2.staging.com', got %s", cfg.LDAP.SavedConnections[1].Host)
	}
	
	// Since this was the active connection, it should be updated automatically
	activeConn = cfg.GetActiveConnection()
	if activeConn.Name != "Staging Updated" {
		t.Errorf("Expected active connection name to be updated to 'Staging Updated', got %s", activeConn.Name)
	}
	if activeConn.Host != "ldap2.staging.com" {
		t.Errorf("Expected active host to be updated to 'ldap2.staging.com', got %s", activeConn.Host)
	}
	
	// Test removing middle connection and index adjustment
	cfg.RemoveSavedConnection(1) // Remove "Staging Updated"
	if len(cfg.LDAP.SavedConnections) != 2 {
		t.Errorf("Expected 2 saved connections after removal, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	// Should fall back to default since we removed the active connection
	if cfg.LDAP.SelectedConnection != -1 {
		t.Errorf("Expected selected connection to be reset to -1, got %d", cfg.LDAP.SelectedConnection)
	}
}

func TestConfigWithSavedConnections(t *testing.T) {
	// Create a config with saved connections in YAML
	configYAML := `
ldap:
  host: "ldap.default.com"
  port: 389
  base_dn: "dc=default,dc=com"
  use_ssl: false
  use_tls: false
  bind_user: "cn=admin,dc=default,dc=com"
  bind_pass: "defaultpass"
  selected_connection: 0
  saved_connections:
    - name: "Production"
      host: "ldap.prod.com"
      port: 636
      base_dn: "dc=prod,dc=com"
      use_ssl: true
      use_tls: false
      bind_user: "cn=prodadmin,dc=prod,dc=com"
      bind_pass: "prodpass"
    - name: "Development"
      host: "ldap.dev.com"
      port: 389
      base_dn: "dc=dev,dc=com"
      use_ssl: false
      use_tls: true
      bind_user: "cn=devadmin,dc=dev,dc=com"
      bind_pass: "devpass"
pagination:
  page_size: 50
retry:
  enabled: true
  max_attempts: 3
  initial_delay_ms: 500
  max_delay_ms: 5000
`
	
	// Create a temporary file
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)
	
	configPath := filepath.Join(tempDir, "config.yaml")
	if err := os.WriteFile(configPath, []byte(configYAML), 0644); err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}
	
	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}
	
	// Test that saved connections were loaded
	if len(cfg.LDAP.SavedConnections) != 2 {
		t.Errorf("Expected 2 saved connections, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	if cfg.LDAP.SelectedConnection != 0 {
		t.Errorf("Expected selected connection to be 0, got %d", cfg.LDAP.SelectedConnection)
	}
	
	// Test that the active connection is the selected one
	activeConn := cfg.GetActiveConnection()
	if activeConn.Name != "Production" {
		t.Errorf("Expected active connection to be 'Production', got %s", activeConn.Name)
	}
	if activeConn.Host != "ldap.prod.com" {
		t.Errorf("Expected active host to be 'ldap.prod.com', got %s", activeConn.Host)
	}
	if activeConn.Port != 636 {
		t.Errorf("Expected active port to be 636, got %d", activeConn.Port)
	}
	if !activeConn.UseSSL {
		t.Errorf("Expected active connection to use SSL")
	}
}

func TestBackwardCompatibility(t *testing.T) {
	// Test that old config format still works
	oldConfigYAML := `
ldap:
  host: "ldap.legacy.com"
  port: 389
  base_dn: "dc=legacy,dc=com"
  use_ssl: false
  use_tls: true
  bind_user: "cn=legacy,dc=legacy,dc=com"
  bind_pass: "legacypass"
pagination:
  page_size: 100
retry:
  enabled: true
  max_attempts: 5
`
	
	// Create a temporary file
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)
	
	configPath := filepath.Join(tempDir, "config.yaml")
	if err := os.WriteFile(configPath, []byte(oldConfigYAML), 0644); err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}
	
	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}
	
	// Test that old config fields are still accessible
	if cfg.LDAP.Host != "ldap.legacy.com" {
		t.Errorf("Expected host to be 'ldap.legacy.com', got %s", cfg.LDAP.Host)
	}
	
	// Test that saved connections is empty
	if len(cfg.LDAP.SavedConnections) != 0 {
		t.Errorf("Expected no saved connections in legacy config, got %d", len(cfg.LDAP.SavedConnections))
	}
	
	// Test that active connection returns the default values
	activeConn := cfg.GetActiveConnection()
	if activeConn.Name != "Default" {
		t.Errorf("Expected active connection name to be 'Default', got %s", activeConn.Name)
	}
	if activeConn.Host != "ldap.legacy.com" {
		t.Errorf("Expected active host to be 'ldap.legacy.com', got %s", activeConn.Host)
	}
	if activeConn.Port != 389 {
		t.Errorf("Expected active port to be 389, got %d", activeConn.Port)
	}
}
