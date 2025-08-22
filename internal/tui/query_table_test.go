package tui

import (
	"strings"
	"testing"

	"github.com/charmbracelet/bubbles/table"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

func TestQueryView_TableDisplay(t *testing.T) {
	// Create a mock client
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.SetSize(80, 20) // Set a reasonable size for testing

	// Create test entries
	entries := []*ldap.Entry{
		{
			DN: "cn=john,ou=users,dc=example,dc=com",
			Attributes: map[string][]string{
				"cn":          {"john"},
				"mail":        {"john@example.com"},
				"objectClass": {"person", "organizationalPerson"},
			},
		},
		{
			DN: "cn=jane,ou=users,dc=example,dc=com",
			Attributes: map[string][]string{
				"cn":   {"jane"},
				"mail": {"jane@example.com"},
			},
		},
	}

	// Set results and build table
	qv.results = entries
	qv.buildTableRows()

	// Check that table has rows
	rows := qv.table.Rows()
	if len(rows) != 2 {
		t.Fatalf("Expected 2 table rows, got %d", len(rows))
	}

	// Check first row content
	if rows[0][0] != "cn=john,ou=users,dc=example,dc=com" {
		t.Errorf("Expected first row DN to be 'cn=john,ou=users,dc=example,dc=com', got '%s'", rows[0][0])
	}

	// Check that summary contains some attributes
	if !strings.Contains(rows[0][1], "cn: john") {
		t.Errorf("Expected first row summary to contain 'cn: john', got '%s'", rows[0][1])
	}

	// Check table columns
	columns := qv.table.Columns()
	if len(columns) != 2 {
		t.Fatalf("Expected 2 columns, got %d", len(columns))
	}

	if columns[0].Title != "DN" {
		t.Errorf("Expected first column title to be 'DN', got '%s'", columns[0].Title)
	}

	if columns[1].Title != "Summary" {
		t.Errorf("Expected second column title to be 'Summary', got '%s'", columns[1].Title)
	}
}

func TestQueryView_EmptyTableDisplay(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.SetSize(80, 20)

	// Test with empty results
	qv.results = []*ldap.Entry{}
	qv.buildTableRows()

	rows := qv.table.Rows()
	if len(rows) != 0 {
		t.Errorf("Expected 0 table rows for empty results, got %d", len(rows))
	}

	// Test rendering
	tableView := qv.renderTable()
	if tableView != "No results" {
		t.Errorf("Expected 'No results' for empty table, got '%s'", tableView)
	}
}

func TestQueryView_TableNavigation(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	qv.SetSize(80, 20)

	// Create test entries
	entries := []*ldap.Entry{
		{DN: "cn=user1,dc=example,dc=com", Attributes: map[string][]string{"cn": {"user1"}}},
		{DN: "cn=user2,dc=example,dc=com", Attributes: map[string][]string{"cn": {"user2"}}},
		{DN: "cn=user3,dc=example,dc=com", Attributes: map[string][]string{"cn": {"user3"}}},
	}

	qv.results = entries
	qv.inputMode = false // Set to browse mode
	qv.buildTableRows()

	// Test that table cursor starts at 0
	if qv.table.Cursor() != 0 {
		t.Errorf("Expected initial table cursor to be 0, got %d", qv.table.Cursor())
	}

	// Test navigation by checking that we can get the current selection
	selectedRow := qv.table.Cursor()
	if selectedRow >= len(qv.results) {
		t.Errorf("Table cursor %d is out of bounds for %d results", selectedRow, len(qv.results))
	}

	selectedEntry := qv.results[selectedRow]
	if selectedEntry.DN != "cn=user1,dc=example,dc=com" {
		t.Errorf("Expected selected entry DN to be 'cn=user1,dc=example,dc=com', got '%s'", selectedEntry.DN)
	}
}

func TestQueryView_FocusHandling(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)

	// Should start in input mode with textarea focused
	if !qv.inputMode {
		t.Error("QueryView should start in input mode")
	}

	// Simulate having results and switching to browse mode
	qv.results = []*ldap.Entry{
		{DN: "cn=test,dc=example,dc=com", Attributes: map[string][]string{}},
	}
	qv.buildTableRows()

	// Switch to browse mode
	qv.inputMode = false
	qv.textarea.Blur()
	qv.table.Focus()

	// Verify browse mode
	if qv.inputMode {
		t.Error("Should be in browse mode after setting inputMode to false")
	}

	// Switch back to input mode
	qv.inputMode = true
	qv.table.Blur()
	qv.textarea.Focus()

	if !qv.inputMode {
		t.Error("Should be back in input mode")
	}
}