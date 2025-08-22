package tui

import (
	"testing"

	tea "github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

func TestTreeView_ClickNavigation(t *testing.T) {
	// Initialize bubblezone for tests
	zone.NewGlobal()

	var client *ldap.Client
	cfg := &config.Config{} // Empty config for test
	model := NewModel(client, cfg)

	// Create a tree view with mock data
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	// Create mock tree data
	tv.FlattenedTree = make([]*TreeItem, 5)
	for i := 0; i < 5; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       "cn=user" + string(rune(i+'0')) + ",dc=example,dc=com",
				Name:     "user" + string(rune(i+'0')),
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 4,
		}
	}

	model.tree = tv
	model.currentView = ViewModeTree
	model.SetSize(80, 24)

	// Generate the view to create the zones
	view := model.View()
	if view == "" {
		t.Fatal("View should not be empty")
	}

	// Test clicking on tree item 2
	zoneID := "tree-item-2"

	// Simulate zone click
	newModel, cmd := model.handleTreeViewClick(zoneID)
	if newModel == nil {
		t.Fatal("Expected model to be returned")
	}

	// Update model from returned model
	updatedModel := newModel.(*Model)

	// Check that cursor moved to clicked item
	if updatedModel.tree.cursor != 2 {
		t.Errorf("Expected cursor to be at position 2, got %d", updatedModel.tree.cursor)
	}

	// Check that a command was returned (should simulate Enter key)
	if cmd == nil {
		t.Error("Expected a command to be returned for node interaction")
	}

	// Test that invalid zone IDs are handled gracefully
	invalidModel, invalidCmd := model.handleTreeViewClick("invalid-zone")
	if invalidModel != model {
		t.Error("Expected original model to be returned for invalid zone")
	}
	if invalidCmd != nil {
		t.Error("Expected no command for invalid zone")
	}

	// Test out of bounds index
	outOfBoundsModel, outOfBoundsCmd := model.handleTreeViewClick("tree-item-10")
	if outOfBoundsModel != model {
		t.Error("Expected original model to be returned for out of bounds index")
	}
	if outOfBoundsCmd != nil {
		t.Error("Expected no command for out of bounds index")
	}
}

func TestTreeView_ClickWithMouseEvent(t *testing.T) {
	// Initialize bubblezone for tests
	zone.NewGlobal()

	var client *ldap.Client
	cfg := &config.Config{}
	model := NewModel(client, cfg)

	// Create tree view with mock data
	tv := NewTreeView(client)
	tv.SetSize(80, 24)
	tv.FlattenedTree = make([]*TreeItem, 3)
	for i := 0; i < 3; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       "ou=group" + string(rune(i+'0')) + ",dc=test,dc=com",
				Name:     "group" + string(rune(i+'0')),
				Children: nil,
				IsLoaded: false,
			},
			Level:  0,
			IsLast: i == 2,
		}
	}

	model.tree = tv
	model.currentView = ViewModeTree
	model.SetSize(80, 24)

	// Generate the view to create zones
	view := model.View()
	if view == "" {
		t.Fatal("View should not be empty")
	}

	// Create a mock mouse event for testing zone bounds
	mouseEvent := tea.MouseMsg{
		X:      10,
		Y:      5, // Should be within tree content area
		Type:   tea.MouseLeft,
		Button: tea.MouseButtonLeft,
	}

	// Create zone message
	zoneMsg := zone.MsgZoneInBounds{
		Event: mouseEvent,
	}

	// Test zone message handling
	newModel, cmd := model.handleZoneMessage(zoneMsg)

	// Since we can't easily mock the exact zone bounds without complex setup,
	// we just verify the handler doesn't crash and returns a valid model
	if newModel == nil {
		t.Fatal("Expected model to be returned from zone message handler")
	}

	// The command could be nil if no zone was actually clicked, which is fine for this test
	_ = cmd
}
