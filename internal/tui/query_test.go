package tui

import (
	"testing"

	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestQueryView_NumberInputInQueryMode(t *testing.T) {
	// Create a mock client (nil is fine for this test as we're not executing queries)
	var client *ldap.Client
	qv := NewQueryView(client)

	// Ensure we start in input mode
	if !qv.IsInputMode() {
		t.Fatal("QueryView should start in input mode")
	}

	// Test that number keys are added to the query when in input mode
	testCases := []struct {
		key      string
		expected string
	}{
		{"1", "1"},
		{"2", "2"},
		{"3", "3"},
		{"0", "0"},
	}

	for _, tc := range testCases {
		// Reset query
		qv.query = ""

		// Create key message
		keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{rune(tc.key[0])}}

		// Update should handle the key and add it to query
		_, _ = qv.handleInputMode(keyMsg)

		if qv.query != tc.expected {
			t.Errorf("Expected query to contain '%s' after pressing '%s', got '%s'", tc.expected, tc.key, qv.query)
		}
	}
}

func TestQueryView_IsInputMode(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)

	// Should start in input mode
	if !qv.IsInputMode() {
		t.Error("QueryView should start in input mode")
	}

	// Simulate switching to browse mode (this happens after query execution)
	qv.inputMode = false

	if qv.IsInputMode() {
		t.Error("IsInputMode should return false when inputMode is false")
	}
}

func TestQueryView_NumberKeysInBrowseMode(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.inputMode = false // Set to browse mode

	// In browse mode, number keys should not affect the query
	originalQuery := qv.query
	keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'1'}}

	_, _ = qv.handleBrowseMode(keyMsg)

	if qv.query != originalQuery {
		t.Error("Query should not be modified by number keys in browse mode")
	}
}
