package tui

import (
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/ericschmar/moribito/internal/config"
)

// TestStartView_ErrorHandlingOnSave tests that save errors are captured and displayed
func TestStartView_ErrorHandlingOnSave(t *testing.T) {
	cfg := config.Default()

	// Create a StartView with an invalid path (directory that doesn't exist and can't be created)
	invalidPath := "/root/nonexistent/path/that/cannot/be/created/config.yaml"
	sv := NewStartViewWithConfigPath(cfg, invalidPath)

	// Try to save config
	sv.saveConfigToDisk()

	// Verify error was captured
	if sv.saveError == nil {
		t.Error("Expected save error to be set when saving to invalid path")
	}

	// Verify error time was set
	if sv.saveErrorTime.IsZero() {
		t.Error("Expected saveErrorTime to be set when error occurs")
	}

	// Verify error is recent (within last second)
	if time.Since(sv.saveErrorTime) > time.Second {
		t.Error("Expected saveErrorTime to be recent")
	}
}

// TestStartView_ErrorClearedOnSuccessfulSave tests that errors are cleared on successful save
func TestStartView_ErrorClearedOnSuccessfulSave(t *testing.T) {
	cfg := config.Default()

	// Create temp directory for test
	tempDir := t.TempDir()
	configPath := filepath.Join(tempDir, "config.yaml")

	sv := NewStartViewWithConfigPath(cfg, configPath)

	// Set a fake error
	sv.saveError = os.ErrPermission
	sv.saveErrorTime = time.Now()

	// Save config successfully
	sv.saveConfigToDisk()

	// Verify error was cleared
	if sv.saveError != nil {
		t.Errorf("Expected error to be cleared after successful save, got: %v", sv.saveError)
	}

	// Verify error time was cleared
	if !sv.saveErrorTime.IsZero() {
		t.Error("Expected saveErrorTime to be cleared after successful save")
	}
}

// TestStartView_NoConfigPathError tests that missing config path is reported
func TestStartView_NoConfigPathError(t *testing.T) {
	cfg := config.Default()

	// Create StartView with empty config path
	sv := &StartView{
		config:     cfg,
		configPath: "",
		cursor:     0,
	}

	// Try to save
	sv.saveConfigToDisk()

	// Verify error was set
	if sv.saveError == nil {
		t.Error("Expected error when config path is empty")
	}

	// Verify error message mentions config path
	if sv.saveError != nil && sv.saveError.Error() != "no config file path set - changes will not persist" {
		t.Errorf("Expected error about missing config path, got: %v", sv.saveError)
	}
}

// TestConfig_ValidateAndRepair tests that config validation detects and repairs issues
func TestConfig_ValidateAndRepair(t *testing.T) {
	cfg := config.Default()

	// Add some saved connections
	cfg.LDAP.SavedConnections = []config.SavedConnection{
		{Name: "Conn1", Host: "host1", Port: 389},
		{Name: "Conn2", Host: "host2", Port: 389},
	}

	// Set invalid connection index
	cfg.LDAP.SelectedConnection = 5

	// Validate and repair
	warnings := cfg.ValidateAndRepair()

	// Should have warnings
	if len(warnings) == 0 {
		t.Error("Expected warnings about invalid connection index")
	}

	// Index should be repaired to 0
	if cfg.LDAP.SelectedConnection != 0 {
		t.Errorf("Expected connection index to be repaired to 0, got %d", cfg.LDAP.SelectedConnection)
	}
}

// TestStartView_ConfigWarningsDisplayed tests that config warnings are captured on initialization
func TestStartView_ConfigWarningsDisplayed(t *testing.T) {
	cfg := config.Default()

	// Add some saved connections
	cfg.LDAP.SavedConnections = []config.SavedConnection{
		{Name: "Conn1", Host: "host1", Port: 389},
		{Name: "Conn2", Host: "host2", Port: 389},
	}

	// Set invalid connection index
	cfg.LDAP.SelectedConnection = 10

	// Create StartView (should validate and capture warnings)
	sv := NewStartViewWithConfigPath(cfg, "/tmp/config.yaml")

	// Verify warnings were captured
	if len(sv.configWarnings) == 0 {
		t.Error("Expected config warnings to be captured on initialization")
	}

	// Verify warning time was set
	if sv.configWarningsTime.IsZero() {
		t.Error("Expected configWarningsTime to be set when warnings exist")
	}

	// Verify config was repaired
	if cfg.LDAP.SelectedConnection != 0 {
		t.Errorf("Expected connection index to be repaired, got %d", cfg.LDAP.SelectedConnection)
	}
}
