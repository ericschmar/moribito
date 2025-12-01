package tui

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/config"
)

// TestStartView_ConnectButton tests that the Connect button works correctly
func TestStartView_ConnectButton(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.Host = "ldap.example.com"
	cfg.LDAP.Port = 389
	cfg.LDAP.BaseDN = "dc=example,dc=com"

	tempDir := t.TempDir()
	configPath := filepath.Join(tempDir, "config.yaml")

	sv := NewStartViewWithConfigPath(cfg, configPath)
	sv.width = 100
	sv.height = 40

	// Navigate to the Connect button (it's the last field)
	sv.cursor = FieldConnect

	// Press enter to trigger connect
	model, cmd := sv.Update(tea.KeyMsg{Type: tea.KeyEnter})

	// Should return a command (the connection attempt)
	if cmd == nil {
		t.Error("Expected command to be returned when Connect is pressed, got nil")
	}

	// Model should still be StartView
	if _, ok := model.(*StartView); !ok {
		t.Errorf("Expected model to be *StartView, got %T", model)
	}

	// Check that config was saved
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		t.Error("Expected config file to be created when Connect is pressed")
	}
}

// TestStartView_ConnectWithoutRequiredFields tests error handling
func TestStartView_ConnectWithoutRequiredFields(t *testing.T) {
	cfg := config.Default()
	// Empty host and BaseDN - should fail validation
	cfg.LDAP.Host = ""
	cfg.LDAP.BaseDN = ""

	sv := NewStartViewWithConfigPath(cfg, "/tmp/test-config.yaml")
	sv.width = 100
	sv.height = 40

	// Navigate to Connect button
	sv.cursor = FieldConnect

	// Press enter
	_, cmd := sv.Update(tea.KeyMsg{Type: tea.KeyEnter})

	// Should still return a command (error message)
	if cmd == nil {
		t.Error("Expected command to be returned even when validation fails")
	}

	// Execute the command to get the message
	if cmd != nil {
		msg := cmd()
		if statusMsg, ok := msg.(StatusMsg); ok {
			if !strings.Contains(statusMsg.Message, "Error") {
				t.Errorf("Expected error message, got: %s", statusMsg.Message)
			}
		}
	}
}

// TestStartView_ConnectButtonAvailable tests that Connect field exists
func TestStartView_ConnectButtonAvailable(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.Host = "ldap.example.com"
	cfg.LDAP.Port = 389
	cfg.LDAP.BaseDN = "dc=example,dc=com"

	sv := NewStartViewWithConfigPath(cfg, "/tmp/test-config.yaml")

	// Check that FieldConnect exists and has correct value
	value := sv.getFieldValue(FieldConnect)
	if value != "Connect to LDAP" {
		t.Errorf("Expected FieldConnect value to be 'Connect to LDAP', got: %s", value)
	}

	// Verify Connect field is in bounds
	if FieldConnect >= FieldCount {
		t.Error("FieldConnect index is out of bounds")
	}
}
