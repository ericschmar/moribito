package tui

import (
	"strings"
	"testing"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/config"
	zone "github.com/lrstanley/bubblezone"
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

func TestStartView_LayoutAndAlignment(t *testing.T) {
	// Initialize bubblezone for tests
	zone.NewGlobal()
	
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host:     "ldap.example.com",
			Port:     636,
			BaseDN:   "dc=example,dc=com",
			UseSSL:   true,
			UseTLS:   false,
			BindUser: "cn=admin,dc=example,dc=com",
			BindPass: "secretpassword",
		},
		Pagination: config.PaginationConfig{
			PageSize: 100,
		},
	}

	sv := NewStartView(cfg)
	sv.SetSize(120, 25)

	// Test that all fields are present in the output
	output := sv.View()

	// Should contain all field names (including new connection management fields)  
	expectedFields := []string{
		"Connection Management", "Add New Connection", "Delete Connection", "Save Current as New",
		"Host:", "Port:", "Base DN:", "Use SSL:", "Use TLS:",
		"Bind User:", "Bind Password:", "Page Size:",
	}

	for _, field := range expectedFields {
		if !strings.Contains(output, field) {
			t.Errorf("Expected field '%s' to be present in output", field)
		}
	}

	// Should contain the configuration values
	expectedValues := []string{
		"ldap.example.com", "636", "dc=example,dc=com",
		"true", "false", "cn=admin,dc=example,dc=com", "********", "100",
	}

	for _, value := range expectedValues {
		if !strings.Contains(output, value) {
			t.Errorf("Expected value '%s' to be present in output", value)
		}
	}

	// Check that title is present
	if !strings.Contains(output, "Configure your LDAP connection settings") {
		t.Error("Expected configuration title to be present in output")
	}

	// Check that instructions are present  
	if !strings.Contains(output, "navigate") {
		t.Error("Expected navigation instructions to be present in output")
	}
}
