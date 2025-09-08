package tui

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/ericschmar/moribito/internal/config"
)

func TestStartView_SaveConfigToDisk(t *testing.T) {
	// Create a temporary config file
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "test-config.yaml")

	// Create initial config
	cfg := config.Default()
	cfg.LDAP.Host = "initial.example.com"
	
	// Save initial config
	if err := cfg.Save(configPath); err != nil {
		t.Fatalf("Failed to save initial config: %v", err)
	}

	// Create StartView with config path
	sv := NewStartViewWithConfigPath(cfg, configPath)

	// Simulate editing the host field
	sv.editing = true
	sv.editingField = FieldHost
	sv.inputValue = "modified.example.com"

	// Call saveValue (which should save to disk)
	sv.saveValue()

	// Verify the config was saved to disk by reloading
	reloadedCfg, err := config.Load(configPath)
	if err != nil {
		t.Fatalf("Failed to reload config: %v", err)
	}

	if reloadedCfg.LDAP.Host != "modified.example.com" {
		t.Errorf("Expected host to be 'modified.example.com', got '%s'", reloadedCfg.LDAP.Host)
	}
}

func TestStartView_BackwardCompatibilityWithoutConfigPath(t *testing.T) {
	// Create StartView without config path (old style)
	cfg := config.Default()
	cfg.LDAP.Host = "test.example.com"
	
	sv := NewStartView(cfg) // Old constructor without config path

	// Simulate editing
	sv.editing = true
	sv.editingField = FieldHost
	sv.inputValue = "modified.example.com"

	// Call saveValue - should not panic and should update in-memory config
	sv.saveValue()

	// Verify in-memory config was updated
	if sv.config.LDAP.Host != "modified.example.com" {
		t.Errorf("Expected host to be 'modified.example.com', got '%s'", sv.config.LDAP.Host)
	}
}

func TestStartView_ConnectionManagementSaving(t *testing.T) {
	// Create a temporary config file
	tempDir, err := os.MkdirTemp("", "moribito-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "test-config.yaml")

	// Create initial config
	cfg := config.Default()
	cfg.LDAP.Host = "test.example.com"
	
	// Save initial config
	if err := cfg.Save(configPath); err != nil {
		t.Fatalf("Failed to save initial config: %v", err)
	}

	// Create StartView with config path
	sv := NewStartViewWithConfigPath(cfg, configPath)

	// Simulate adding a new connection manually
	newConn := config.SavedConnection{
		Name:     "Test Connection",
		Host:     cfg.LDAP.Host,
		Port:     cfg.LDAP.Port,
		BaseDN:   cfg.LDAP.BaseDN,
		UseSSL:   cfg.LDAP.UseSSL,
		UseTLS:   cfg.LDAP.UseTLS,
		BindUser: cfg.LDAP.BindUser,
		BindPass: cfg.LDAP.BindPass,
	}
	sv.config.AddSavedConnection(newConn)
	sv.config.SetActiveConnection(0)
	
	// Call saveConfigToDisk to simulate what would happen in the dialog
	sv.saveConfigToDisk()

	// Verify the config was saved to disk by reloading
	reloadedCfg, err := config.Load(configPath)
	if err != nil {
		t.Fatalf("Failed to reload config: %v", err)
	}

	if len(reloadedCfg.LDAP.SavedConnections) != 1 {
		t.Errorf("Expected 1 saved connection, got %d", len(reloadedCfg.LDAP.SavedConnections))
	}

	if reloadedCfg.LDAP.SavedConnections[0].Name != "Test Connection" {
		t.Errorf("Expected connection name 'Test Connection', got '%s'", reloadedCfg.LDAP.SavedConnections[0].Name)
	}
}

// Test helper - remove the keyMsg type since it's not needed