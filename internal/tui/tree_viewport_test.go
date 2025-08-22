package tui

import (
	"testing"

	"github.com/ericschmar/moribito/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

func TestTreeView_ViewportIntegration(t *testing.T) {
	var client *ldap.Client
	tv := NewTreeView(client)

	// Test viewport initialization
	if tv.viewport != 0 {
		t.Error("Viewport should be initialized to 0")
	}

	// Test SetSize configures dimensions
	tv.SetSize(80, 24)
	if tv.width != 80 {
		t.Errorf("Expected width 80, got %d", tv.width)
	}
	if tv.height != 24 {
		t.Errorf("Expected height 24, got %d", tv.height)
	}

	// Test that View() returns expected content when loading
	tv.loading = true
	view := tv.View()
	if view == "" {
		t.Error("View should return loading message when loading")
	}

	// Test that View() returns expected content when no entries
	tv.loading = false
	tv.FlattenedTree = nil
	view = tv.View()
	if view == "" {
		t.Error("View should return 'No entries found' message when no tree")
	}
}

func TestTreeView_ViewportContentUpdate(t *testing.T) {
	// Initialize bubblezone for test environment
	zone.NewGlobal()

	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	// Create some mock tree data
	tv.root = &ldap.TreeNode{
		DN:       "dc=example,dc=com",
		Name:     "example.com",
		Children: nil,
		IsLoaded: true,
	}
	tv.rebuildFlattenedTree()

	// Test that view content is updated
	view := tv.View()
	if view == "" {
		t.Error("View should have content after rebuilding tree")
	}

	// Test cursor movement updates viewport
	tv.cursor = 0
	tv.adjustViewport()
	// No errors should occur
}

func TestTreeView_CursorVisibility(t *testing.T) {
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 10) // Small height to test scrolling

	// Create multiple mock tree items
	tv.FlattenedTree = make([]*TreeItem, 20)
	for i := 0; i < 20; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       "test",
				Name:     "test",
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 19,
		}
	}

	// Test cursor at top
	tv.cursor = 0
	tv.adjustViewport()
	if tv.viewport != 0 {
		t.Errorf("Expected viewport 0 when cursor at top, got %d", tv.viewport)
	}

	// Test cursor past visible area
	tv.cursor = 15
	tv.adjustViewport()
	if tv.viewport < 0 {
		t.Errorf("Viewport should not be negative, got %d", tv.viewport)
	}
}
