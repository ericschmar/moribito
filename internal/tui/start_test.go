package tui

import (
	"strings"
	"testing"

	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/config"
	zone "github.com/lrstanley/bubblezone"
)

func TestStartView_TextInputIntegration(t *testing.T) {
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
	sv.textInput.SetValue("ldap.example.com")

	// Call saveValue to ensure textInput value is saved
	sv.saveValue()

	expected := "ldap.example.com"
	if sv.config.LDAP.Host != expected {
		t.Errorf("Expected host to be '%s' after save, got '%s'", expected, sv.config.LDAP.Host)
	}
}

func TestStartView_NewConnectionDialog(t *testing.T) {
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host: "localhost",
			Port: 389,
		},
	}

	sv := NewStartView(cfg)
	sv.showNewConnectionDialog = true
	sv.newConnInput.SetValue("test-connection")

	// Simulate pressing enter to create connection
	keyMsg := tea.KeyMsg{Type: tea.KeyEnter}
	_, _ = sv.handleNewConnectionDialog(keyMsg)

	// Verify connection was created
	if len(sv.config.LDAP.SavedConnections) != 1 {
		t.Errorf("Expected 1 saved connection, got %d", len(sv.config.LDAP.SavedConnections))
	}

	if sv.config.LDAP.SavedConnections[0].Name != "test-connection" {
		t.Errorf("Expected connection name 'test-connection', got '%s'", sv.config.LDAP.SavedConnections[0].Name)
	}
}

func TestStartView_BooleanToggle(t *testing.T) {
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host:   "localhost",
			Port:   389,
			UseSSL: false,
		},
	}

	sv := NewStartView(cfg)
	sv.editing = true
	sv.editingField = FieldUseSSL

	// Test toggling with space key
	keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{' '}}
	_, _ = sv.handleEditMode(keyMsg)

	if !sv.config.LDAP.UseSSL {
		t.Error("Expected UseSSL to be toggled to true")
	}

	// Toggle back
	_, _ = sv.handleEditMode(keyMsg)
	if sv.config.LDAP.UseSSL {
		t.Error("Expected UseSSL to be toggled back to false")
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
		"Connection Management", "Save", "Delete",
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
