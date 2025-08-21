package tui

import (
	"testing"

	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestTreeView_ViewportIntegration(t *testing.T) {
	var client *ldap.Client
	tv := NewTreeView(client)

	// Test viewport initialization
	if tv.viewport.Width != 0 || tv.viewport.Height != 0 {
		t.Error("Viewport should be initialized with zero dimensions")
	}

	// Test SetSize configures viewport
	tv.SetSize(80, 24)
	if tv.viewport.Width != 80 {
		t.Errorf("Expected viewport width 80, got %d", tv.viewport.Width)
	}
	if tv.viewport.Height != 24 {
		t.Errorf("Expected viewport height 24, got %d", tv.viewport.Height)
	}

	// Test that View() returns expected content when loading
	tv.loading = true
	view := tv.View()
	if view == "" {
		t.Error("View should return loading message when loading")
	}

	// Test that View() returns expected content when no entries
	tv.loading = false
	tv.flattenedTree = nil
	view = tv.View()
	if view == "" {
		t.Error("View should return 'No entries found' message when no tree")
	}
}

func TestTreeView_ViewportContentUpdate(t *testing.T) {
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

	// Test that viewport content is updated
	content := tv.viewport.View()
	if content == "" {
		t.Error("Viewport should have content after rebuilding tree")
	}

	// Test cursor movement updates viewport
	tv.cursor = 0
	tv.updateViewportForCursor()
	// No errors should occur
}

func TestTreeView_CursorVisibility(t *testing.T) {
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 10) // Small height to test scrolling

	// Create multiple mock tree items
	tv.flattenedTree = make([]*TreeItem, 20)
	for i := 0; i < 20; i++ {
		tv.flattenedTree[i] = &TreeItem{
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
	tv.updateViewportForCursor()
	if tv.viewport.YOffset != 0 {
		t.Errorf("Expected viewport YOffset 0 when cursor at top, got %d", tv.viewport.YOffset)
	}

	// Test cursor past visible area
	tv.cursor = 15
	tv.updateViewportForCursor()
	if tv.viewport.YOffset < 0 {
		t.Errorf("Viewport YOffset should not be negative, got %d", tv.viewport.YOffset)
	}
}