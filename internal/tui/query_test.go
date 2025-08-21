package tui

import (
	"testing"

	"github.com/atotto/clipboard"
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

func TestQueryView_PasteInQueryMode(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.query = "(objectClass="

	// Set clipboard content for testing
	testContent := "person)"
	err := clipboard.WriteAll(testContent)
	if err != nil {
		t.Skipf("Clipboard not available in test environment: %v", err)
	}

	// Create ctrl+v key message
	keyMsg := tea.KeyMsg{Type: tea.KeyCtrlV}

	// Update should handle the paste
	_, _ = qv.handleInputMode(keyMsg)

	expected := "(objectClass=person)"
	if qv.query != expected {
		t.Errorf("Expected query to be '%s' after paste, got '%s'", expected, qv.query)
	}
}

func TestQueryView_ExistingFunctionalityPreserved(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.query = "test"

	// Test that backspace still works
	keyMsg := tea.KeyMsg{Type: tea.KeyBackspace}
	_, _ = qv.handleInputMode(keyMsg)
	if qv.query != "tes" {
		t.Errorf("Expected query to be 'tes' after backspace, got '%s'", qv.query)
	}

	// Test that ctrl+u still works
	keyMsg = tea.KeyMsg{Type: tea.KeyCtrlU}
	_, _ = qv.handleInputMode(keyMsg)
	if qv.query != "" {
		t.Errorf("Expected query to be empty after ctrl+u, got '%s'", qv.query)
	}

	// Test that regular character input still works
	keyMsg = tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'a'}}
	_, _ = qv.handleInputMode(keyMsg)
	if qv.query != "a" {
		t.Errorf("Expected query to be 'a' after character input, got '%s'", qv.query)
	}
}

func TestQueryView_ReturnToInputModeFromBrowseMode(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	
	// Start in input mode
	if !qv.IsInputMode() {
		t.Fatal("QueryView should start in input mode")
	}
	
	// Simulate query execution by switching to browse mode (like the Update method does)
	qv.inputMode = false
	qv.results = []*ldap.Entry{{DN: "test=example,dc=test"}}
	qv.buildResultLines()
	
	// Verify we're in browse mode
	if qv.IsInputMode() {
		t.Fatal("Should be in browse mode after simulating query execution")
	}
	
	// Test escape key to return to input mode
	escapeMsg := tea.KeyMsg{Type: tea.KeyEscape}
	t.Logf("Escape key string: '%s'", escapeMsg.String())
	_, _ = qv.handleBrowseMode(escapeMsg)
	
	t.Logf("After escape: inputMode=%v", qv.IsInputMode())
	if !qv.IsInputMode() {
		t.Error("Should be back in input mode after pressing escape")
	}
	
	// Test slash key to return to input mode
	qv.inputMode = false // Set back to browse mode
	slashMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'/'}}
	t.Logf("Slash key string: '%s'", slashMsg.String())
	_, _ = qv.handleBrowseMode(slashMsg)
	
	if !qv.IsInputMode() {
		t.Error("Should be back in input mode after pressing '/'")
	}
	
	// Test that we can type in the search box after returning to input mode
	qv.query = ""
	testMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'t', 'e', 's', 't'}}
	for _, r := range testMsg.Runes {
		charMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{r}}
		_, _ = qv.handleInputMode(charMsg)
	}
	
	if qv.query != "test" {
		t.Errorf("Should be able to type in search box after returning to input mode, got '%s'", qv.query)
	}
}
