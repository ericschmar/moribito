package tui

import (
	"testing"

	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestTreeView_HighlightingConsistency(t *testing.T) {
	// Test to verify the highlighting issue is fixed
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 8) // Small height to force scrolling

	// Create mock tree data - enough to require scrolling
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

	// Test highlighting at different cursor positions to verify viewport consistency
	testCases := []struct {
		cursor      int
		description string
	}{
		{0, "cursor at top"},
		{5, "cursor in middle of first view"},
		{10, "cursor requiring scroll"},
		{15, "cursor further down"},
		{19, "cursor at end"},
	}

	for _, tc := range testCases {
		t.Run(tc.description, func(t *testing.T) {
			// Set cursor position and adjust viewport
			tv.cursor = tc.cursor
			tv.adjustViewport()

			// Get content height consistently
			_, contentHeight := tv.container.GetContentDimensions()
			if tv.container == nil {
				contentHeight = tv.height
			}

			// Verify cursor is within visible range after viewport adjustment
			if tc.cursor < tv.viewport || tc.cursor >= tv.viewport+contentHeight {
				t.Errorf("Cursor %d should be visible within viewport %d to %d (content height %d)",
					tc.cursor, tv.viewport, tv.viewport+contentHeight-1, contentHeight)
			}

			// Verify viewport bounds are reasonable
			if tv.viewport < 0 {
				t.Errorf("Viewport should not be negative, got %d", tv.viewport)
			}

			if tv.viewport >= len(tv.FlattenedTree) {
				t.Errorf("Viewport %d should be less than tree length %d", tv.viewport, len(tv.FlattenedTree))
			}
		})
	}
}
