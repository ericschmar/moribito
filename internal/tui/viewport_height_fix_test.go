package tui

import (
	"strings"
	"testing"

	"github.com/ericschmar/ldap-cli/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

// TestQueryViewHeightConstraint tests that QueryView respects height constraints
// This test verifies the fix for the tab bar cutoff issue
func TestQueryViewHeightConstraint(t *testing.T) {
	// Initialize bubblezone for testing
	zone.NewGlobal()

	var client *ldap.Client
	qv := NewQueryView(client)

	// Test different height constraints
	testCases := []struct {
		name   string
		width  int
		height int
	}{
		{"Small viewport", 80, 10},
		{"Medium viewport", 100, 15},
		{"Large viewport", 120, 25},
		{"Very large viewport", 150, 35},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			qv.SetSize(tc.width, tc.height)

			// Get the rendered view
			view := qv.View()
			lines := strings.Split(view, "\n")

			// The view should never exceed the allocated height
			if len(lines) > tc.height {
				t.Errorf("QueryView exceeded height constraint: got %d lines, max allowed %d",
					len(lines), tc.height)
				t.Logf("This would cause the tab bar to be cut off!")

				// Log the content for debugging
				t.Logf("View content (first 10 lines):")
				for i := 0; i < min(10, len(lines)); i++ {
					t.Logf("  %d: %s", i+1, lines[i])
				}
			}
		})
	}
}

// TestTreeViewHeightConstraint tests that TreeView respects height constraints
func TestTreeViewHeightConstraint(t *testing.T) {
	// Initialize bubblezone for testing
	zone.NewGlobal()

	var client *ldap.Client
	tv := NewTreeView(client)

	// Create mock tree data that would potentially overflow
	tv.FlattenedTree = make([]*TreeItem, 50)
	for i := 0; i < 50; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       "ou=test,dc=example,dc=com",
				Name:     "Test Entry",
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 49,
		}
	}

	// Test different height constraints
	testCases := []struct {
		name   string
		width  int
		height int
	}{
		{"Small viewport", 80, 10},
		{"Medium viewport", 100, 15},
		{"Large viewport", 120, 25},
		{"Very large viewport", 150, 35},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			tv.SetSize(tc.width, tc.height)

			// Get the rendered view
			view := tv.View()
			lines := strings.Split(view, "\n")

			// The view should never exceed the allocated height
			if len(lines) > tc.height {
				t.Errorf("TreeView exceeded height constraint: got %d lines, max allowed %d",
					len(lines), tc.height)
				t.Logf("This would cause the tab bar to be cut off!")
			}
		})
	}
}

// TestQueryViewWithResults tests QueryView with simulated results
func TestQueryViewWithResults(t *testing.T) {
	// Initialize bubblezone for testing
	zone.NewGlobal()

	var client *ldap.Client
	qv := NewQueryView(client)
	qv.SetSize(120, 25)

	// Simulate having results by directly setting the ResultLines
	// This bypasses the need for an actual LDAP connection
	mockResultLines := make([]string, 30)
	for i := 0; i < 30; i++ {
		mockResultLines[i] = "DN: cn=user" + string(rune(i+'0')) + ",ou=people,dc=example,dc=com"
	}
	qv.ResultLines = mockResultLines

	// Simulate having actual results (private field access via reflection would be complex,
	// so we'll just test the basic case here)
	view := qv.View()
	lines := strings.Split(view, "\n")

	if len(lines) > 25 {
		t.Errorf("QueryView with results exceeded height constraint: got %d lines, max allowed 25", len(lines))
		t.Logf("This confirms the tab bar cutoff issue would occur with results")
	}
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
