package tui

import (
	"testing"

	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestViewportConsistency(t *testing.T) {
	// Test that all views handle viewport dimensions consistently
	var client *ldap.Client

	// Test TreeView
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	if tv.width != 80 || tv.height != 24 {
		t.Errorf("TreeView dimensions not set correctly: got %dx%d, want 80x24", tv.width, tv.height)
	}

	// Create mock tree data for testing scrolling
	tv.FlattenedTree = make([]*TreeItem, 50) // More items than can fit on screen
	for i := 0; i < 50; i++ {
		tv.FlattenedTree[i] = &TreeItem{
			Node: &ldap.TreeNode{
				DN:       "ou=test" + string(rune(i+'0')) + ",dc=example,dc=com",
				Name:     "test" + string(rune(i+'0')),
				Children: nil,
				IsLoaded: true,
			},
			Level:  0,
			IsLast: i == 49,
		}
	}

	// Test viewport adjustment at top
	tv.cursor = 0
	tv.adjustViewport()
	if tv.viewport != 0 {
		t.Errorf("Viewport should be 0 when cursor at top, got %d", tv.viewport)
	}

	// Test viewport adjustment when cursor moves beyond visible area
	tv.cursor = 30 // Move cursor beyond visible area (assuming 24 lines visible)
	tv.adjustViewport()
	expectedViewport := tv.cursor - tv.height + 1
	if expectedViewport < 0 {
		expectedViewport = 0
	}
	if tv.viewport < 0 || tv.viewport > tv.cursor {
		t.Errorf("Viewport should keep cursor visible, cursor=%d, viewport=%d, height=%d", tv.cursor, tv.viewport, tv.height)
	}

	// Test QueryView
	qv := NewQueryView(client)
	qv.SetSize(80, 24)

	if qv.width != 80 || qv.height != 24 {
		t.Errorf("QueryView dimensions not set correctly: got %dx%d, want 80x24", qv.width, qv.height)
	}

	// Create mock result data
	qv.results = make([]*ldap.Entry, 30)
	for i := 0; i < 30; i++ {
		qv.results[i] = &ldap.Entry{
			DN: "cn=user" + string(rune(i+'0')) + ",ou=users,dc=example,dc=com",
			Attributes: map[string][]string{
				"cn":   {"user" + string(rune(i+'0'))},
				"mail": {"user" + string(rune(i+'0')) + "@example.com"},
			},
		}
	}
	qv.buildResultLines()

	// Test viewport adjustment
	qv.cursor = 0
	qv.adjustViewport()
	if qv.viewport != 0 {
		t.Errorf("QueryView viewport should be 0 when cursor at top, got %d", qv.viewport)
	}

	// Test cursor beyond visible area
	qv.cursor = len(qv.ResultLines) - 1
	qv.adjustViewport()
	if qv.viewport < 0 {
		t.Errorf("QueryView viewport should not be negative, got %d", qv.viewport)
	}
}

func TestViewportScrolling(t *testing.T) {
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 10) // Small height for easier testing

	// Create tree items
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

	// Test scrolling down
	tv.cursor = 0
	tv.adjustViewport()
	initialViewport := tv.viewport

	// Move cursor to middle
	tv.cursor = 5
	tv.adjustViewport()
	// Viewport should still be 0 since cursor is still visible
	if tv.viewport != initialViewport {
		t.Errorf("Viewport changed unnecessarily when cursor moved to %d: viewport %d -> %d", tv.cursor, initialViewport, tv.viewport)
	}

	// Move cursor past visible area
	tv.cursor = 15
	tv.adjustViewport()
	// Viewport should adjust to keep cursor visible
	if tv.viewport == initialViewport {
		t.Errorf("Viewport should have adjusted when cursor moved beyond visible area (cursor=%d, viewport=%d)", tv.cursor, tv.viewport)
	}

	// Test scrolling up
	tv.cursor = 5
	tv.adjustViewport()
	if tv.cursor < tv.viewport {
		t.Errorf("Cursor should be visible after scrolling up: cursor=%d, viewport=%d", tv.cursor, tv.viewport)
	}
}

func TestQueryViewNavigation(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.SetSize(80, 20)

	// Create test data
	qv.results = make([]*ldap.Entry, 5)
	for i := 0; i < 5; i++ {
		qv.results[i] = &ldap.Entry{
			DN: "cn=user" + string(rune(i+'0')) + ",dc=example,dc=com",
			Attributes: map[string][]string{
				"cn": {"user" + string(rune(i+'0'))},
			},
		}
	}
	qv.buildResultLines()
	qv.inputMode = false // Set to browse mode

	// Test navigation
	initialCursor := qv.cursor

	// Test down navigation
	downMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune("j")}
	_, _ = qv.Update(downMsg)

	if qv.cursor != initialCursor+1 {
		t.Errorf("Down navigation failed: cursor should be %d, got %d", initialCursor+1, qv.cursor)
	}

	// Test up navigation
	upMsg := tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune("k")}
	_, _ = qv.Update(upMsg)

	if qv.cursor != initialCursor {
		t.Errorf("Up navigation failed: cursor should be %d, got %d", initialCursor, qv.cursor)
	}
}
