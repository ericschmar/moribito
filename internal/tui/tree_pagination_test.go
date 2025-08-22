package tui

import (
	"fmt"
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone"

	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestTreeViewPagination(t *testing.T) {
	// Initialize bubblezone for test environment
	zone.NewGlobal()

	// Create a tree view with many items
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 10) // Small height to force pagination

	// Create mock tree items - more than can fit on screen
	tv.FlattenedTree = make([]*TreeItem, 20)
	for i := 0; i < 20; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       fmt.Sprintf("ou=test%d,dc=example,dc=com", i),
				Name:     fmt.Sprintf("test%d", i),
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 19,
		}
	}

	// Test that pagination info appears in view
	view := tv.View()
	if !strings.Contains(view, "Showing") || !strings.Contains(view, "entries") {
		t.Errorf("Expected pagination info in view, got: %s", view)
	}

	// Test that pagination shows correct range for first page
	if !strings.Contains(view, "Showing 1-") {
		t.Errorf("Expected to show from entry 1, got: %s", view)
	}

	// Test cursor movement and viewport adjustment
	tv.cursor = 15
	tv.adjustViewport()
	view2 := tv.View()

	// Should show a range that includes cursor position 15
	if !strings.Contains(view2, "entries") {
		t.Errorf("Expected pagination info after cursor movement, got: %s", view2)
	}

	t.Logf("Initial tree view with pagination:\n%s\n", view)
	t.Logf("After cursor movement:\n%s\n", view2)
}

func TestTreeViewNoPaginationWhenFewItems(t *testing.T) {
	// Initialize bubblezone for test environment
	zone.NewGlobal()

	// Create a tree view with few items
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 20) // Large height, few items

	// Create few mock tree items - less than screen height
	tv.FlattenedTree = make([]*TreeItem, 5)
	for i := 0; i < 5; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       fmt.Sprintf("ou=test%d,dc=example,dc=com", i),
				Name:     fmt.Sprintf("test%d", i),
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 4,
		}
	}

	// Test that pagination info does NOT appear
	view := tv.View()
	if strings.Contains(view, "Showing") && strings.Contains(view, "entries") {
		t.Errorf("Expected NO pagination info when few items, got: %s", view)
	}

	t.Logf("Tree view without pagination:\n%s\n", view)
}
