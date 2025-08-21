package tui

import (
	"testing"

	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestModel_NavigationKeysWithQueryInputMode(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	model := NewModel(client)

	// Set to query view
	model.currentView = ViewModeQuery

	// Ensure query view is in input mode
	if !model.queryView.IsInputMode() {
		t.Fatal("QueryView should be in input mode for this test")
	}

	// Test that number keys don't trigger navigation when in query input mode
	testCases := []string{"1", "2", "3"}

	for _, key := range testCases {
		originalView := model.currentView
		keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{rune(key[0])}}

		// Update the model
		_, _ = model.Update(keyMsg)

		// View should not have changed
		if model.currentView != originalView {
			t.Errorf("View should not change when pressing '%s' in query input mode, expected %d, got %d",
				key, originalView, model.currentView)
		}
	}
}

func TestModel_NavigationKeysWithQueryBrowseMode(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	model := NewModel(client)

	// Set to query view but in browse mode
	model.currentView = ViewModeQuery
	model.queryView.inputMode = false // Set to browse mode

	// Test that number keys DO trigger navigation when NOT in query input mode
	testCases := []struct {
		key          string
		expectedView ViewMode
	}{
		{"1", ViewModeTree},
		{"2", ViewModeRecord},
		{"3", ViewModeQuery},
	}

	for _, tc := range testCases {
		// Reset to query view
		model.currentView = ViewModeQuery

		keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{rune(tc.key[0])}}

		// Update the model
		_, _ = model.Update(keyMsg)

		// View should have changed to expected view
		if model.currentView != tc.expectedView {
			t.Errorf("View should change when pressing '%s' in query browse mode, expected %d, got %d",
				tc.key, tc.expectedView, model.currentView)
		}
	}
}

func TestModel_NavigationKeysInOtherViews(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	model := NewModel(client)

	// Test that number keys work normally in other views
	testCases := []struct {
		initialView  ViewMode
		key          string
		expectedView ViewMode
	}{
		{ViewModeTree, "2", ViewModeRecord},
		{ViewModeTree, "3", ViewModeQuery},
		{ViewModeRecord, "1", ViewModeTree},
		{ViewModeRecord, "3", ViewModeQuery},
	}

	for _, tc := range testCases {
		model.currentView = tc.initialView

		keyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{rune(tc.key[0])}}

		// Update the model
		_, _ = model.Update(keyMsg)

		// View should have changed to expected view
		if model.currentView != tc.expectedView {
			t.Errorf("Navigation should work normally in other views: pressing '%s' from view %d should go to %d, got %d",
				tc.key, tc.initialView, tc.expectedView, model.currentView)
		}
	}
}
