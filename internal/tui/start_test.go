package tui

import (
	"testing"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/ldap-cli/internal/config"
)

func TestStartView_PasteInEditMode(t *testing.T) {
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host: "localhost",
			Port: 389,
		},
		Pagination: config.PaginationConfig{
			PageSize: 50,
		},
	}

	sv := NewStartView(cfg)
	sv.editing = true
	sv.editingField = FieldHost
	sv.inputValue = "ldap"

	// Set clipboard content for testing
	testContent := ".example.com"
	err := clipboard.WriteAll(testContent)
	if err != nil {
		t.Skipf("Clipboard not available in test environment: %v", err)
	}

	// Create ctrl+v key message
	keyMsg := tea.KeyMsg{Type: tea.KeyCtrlV}

	// Update should handle the paste
	_, _ = sv.handleEditMode(keyMsg)

	expected := "ldap.example.com"
	if sv.inputValue != expected {
		t.Errorf("Expected inputValue to be '%s' after paste, got '%s'", expected, sv.inputValue)
	}
}

func TestStartView_ExistingFunctionalityPreserved(t *testing.T) {
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host: "localhost",
			Port: 389,
		},
		Pagination: config.PaginationConfig{
			PageSize: 50,
		},
	}

	sv := NewStartView(cfg)
	sv.editing = true
	sv.editingField = FieldHost
	sv.inputValue = "test"

	// Test that backspace still works
	keyMsg := tea.KeyMsg{Type: tea.KeyBackspace}
	_, _ = sv.handleEditMode(keyMsg)
	if sv.inputValue != "tes" {
		t.Errorf("Expected inputValue to be 'tes' after backspace, got '%s'", sv.inputValue)
	}

	// Test that regular character input still works
	keyMsg = tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'t'}}
	_, _ = sv.handleEditMode(keyMsg)
	if sv.inputValue != "test" {
		t.Errorf("Expected inputValue to be 'test' after character input, got '%s'", sv.inputValue)
	}
}
