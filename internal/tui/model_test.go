package tui

import (
	"testing"

	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
)

func TestModel_NavigationKeysWithQueryInputMode(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Since client is nil, manually create queryView for testing
	model.queryView = NewQueryView(client)

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

func TestModel_QuitKeyDuringStartViewEditing(t *testing.T) {
	// Create a model
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Set to start view (configuration mode)
	model.currentView = ViewModeStart

	// Start editing a field
	model.startView.editing = true
	model.startView.editingField = FieldHost
	model.startView.inputValue = "localhost"

	// Verify we're in editing mode
	if !model.startView.IsEditing() {
		t.Fatal("StartView should be in editing mode for this test")
	}

	// Test that 'q' key doesn't quit when in editing mode
	originalQuitting := model.quitting
	qKeyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'q'}}

	// Update the model
	_, cmd := model.Update(qKeyMsg)

	// Application should NOT quit when in editing mode
	if model.quitting != originalQuitting {
		t.Error("Application should not quit when pressing 'q' in editing mode")
	}

	// Command should not be tea.Quit
	if cmd != nil {
		// We need to check if the command would quit the app
		// Since we can't easily inspect the command, we check if quitting flag changed
		if model.quitting {
			t.Error("Quit command should not be returned when in editing mode")
		}
	}
}

func TestModel_QuitKeyWhenNotEditing(t *testing.T) {
	// Create a model
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Set to start view but NOT editing
	model.currentView = ViewModeStart
	model.startView.editing = false

	// Verify we're NOT in editing mode
	if model.startView.IsEditing() {
		t.Fatal("StartView should NOT be in editing mode for this test")
	}

	// Test that 'q' key DOES quit when not in editing mode
	qKeyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'q'}}

	// Update the model
	_, cmd := model.Update(qKeyMsg)

	// Application SHOULD quit when not in editing mode
	if !model.quitting {
		t.Error("Application should quit when pressing 'q' when not in editing mode")
	}

	// Command should be tea.Quit (we can't easily check this, but quitting flag should be true)
	if cmd == nil {
		t.Error("Quit command should be returned when not in editing mode")
	}
}

func TestModel_QuitKeyDuringQueryViewEditing(t *testing.T) {
	// Create a model
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Create query view manually since client is nil
	model.queryView = NewQueryView(client)

	// Set to query view
	model.currentView = ViewModeQuery

	// Verify query view is in input mode (should be by default)
	if !model.queryView.IsInputMode() {
		t.Fatal("QueryView should be in input mode for this test")
	}

	// Test that 'q' key doesn't quit when in query input mode
	originalQuitting := model.quitting
	qKeyMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'q'}}

	// Update the model
	_, cmd := model.Update(qKeyMsg)

	// Application should NOT quit when in query input mode
	if model.quitting != originalQuitting {
		t.Error("Application should not quit when pressing 'q' in query input mode")
	}

	// Command should not be tea.Quit
	if cmd != nil && model.quitting {
		t.Error("Quit command should not be returned when in query input mode")
	}
}

func TestModel_NavigationKeysWithQueryBrowseMode(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Since client is nil, manually create queryView for testing
	model.queryView = NewQueryView(client)

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
	cfg := config.Default()
	model := NewModel(client, cfg)

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

func TestModel_TreeLoadingHandledRegardlessOfCurrentView(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Ensure tree exists and is in loading state initially
	if model.tree == nil {
		t.Fatal("Tree should exist")
	}

	// Set tree to loading state
	model.tree.loading = true

	// Switch to a different view (not tree view)
	model.currentView = ViewModeRecord

	// Simulate RootNodeLoadedMsg arriving while not on tree view
	mockTreeNode := &ldap.TreeNode{
		DN:       "dc=example,dc=com",
		Name:     "example.com",
		Children: nil,
		IsLoaded: false,
	}
	rootLoadedMsg := RootNodeLoadedMsg{Node: mockTreeNode}

	// Update the model with the message
	_, _ = model.Update(rootLoadedMsg)

	// Tree should have received and processed the message even though we're not on tree view
	if model.tree.loading {
		t.Error("Tree should not be in loading state after RootNodeLoadedMsg")
	}

	if model.tree.root != mockTreeNode {
		t.Error("Tree root should have been set to the loaded node")
	}

	// Switch to tree view to verify it displays properly
	model.currentView = ViewModeTree
	view := model.tree.View()

	// Should not show loading or "No entries found" since root was loaded
	if view == "Loading LDAP tree..." {
		t.Error("Tree should not show loading message after root was loaded")
	}
}

func TestModel_NodeChildrenLoadingHandledRegardlessOfCurrentView(t *testing.T) {
	// Create a model with a mock client
	var client *ldap.Client
	cfg := config.Default()
	model := NewModel(client, cfg)

	// Set tree to loading state
	model.tree.loading = true

	// Switch to a different view (not tree view)
	model.currentView = ViewModeStart

	// Create a mock tree structure
	mockTreeNode := &ldap.TreeNode{
		DN:       "ou=users,dc=example,dc=com",
		Name:     "users",
		Children: nil,
		IsLoaded: true,
	}

	// Simulate NodeChildrenLoadedMsg arriving while not on tree view
	childrenLoadedMsg := NodeChildrenLoadedMsg{Node: mockTreeNode}

	// Update the model with the message
	_, _ = model.Update(childrenLoadedMsg)

	// Tree should have processed the message even though we're not on tree view
	if model.tree.loading {
		t.Error("Tree should not be in loading state after NodeChildrenLoadedMsg")
	}
}
