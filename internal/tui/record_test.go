package tui

import (
	"strings"
	"testing"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/table"
	"github.com/charmbracelet/bubbletea"
	"github.com/ericschmar/moribito/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
)

func TestRecordView_buildLines_TableFormat(t *testing.T) {
	// Create a test entry with multiple attributes and values
	entry := &ldap.Entry{
		DN: "cn=test user,ou=users,dc=example,dc=com",
		Attributes: map[string][]string{
			"cn":          {"test user"},
			"objectClass": {"person", "organizationalPerson", "user"},
			"mail":        {"test@example.com"},
			"department":  {"Engineering", "Development"},
		},
	}

	// Create record view and set the entry
	rv := NewRecordView()
	rv.SetSize(80, 20) // Set a reasonable size for testing
	rv.SetEntry(entry)

	// Check that rows were generated in the table
	rows := rv.table.Rows()
	if len(rows) == 0 {
		t.Fatal("Expected table rows to be generated, but got empty slice")
	}

	// Check DN header is present and styled
	if rv.dnHeader == "" {
		t.Fatal("Expected DN header to be set")
	}

	if !strings.Contains(rv.dnHeader, "DN: cn=test user,ou=users,dc=example,dc=com") {
		t.Errorf("Expected DN header to contain the DN, got: %s", rv.dnHeader)
	}

	// Check that table columns are properly configured
	columns := rv.table.Columns()
	if len(columns) != 2 {
		t.Fatalf("Expected 2 columns, got %d", len(columns))
	}

	if columns[0].Title != "Attribute" || columns[1].Title != "Value(s)" {
		t.Errorf("Expected columns 'Attribute' and 'Value(s)', got: %s, %s", columns[0].Title, columns[1].Title)
	}

	// Check that attributes are present in the table
	expectedAttribs := []string{"cn", "objectClass", "mail", "department"}
	foundAttribs := make(map[string]bool)

	for _, row := range rows {
		if len(row) >= 1 {
			foundAttribs[row[0]] = true
		}
	}

	for _, attr := range expectedAttribs {
		if !foundAttribs[attr] {
			t.Errorf("Expected attribute '%s' to be present in table", attr)
		}
	}

	// Check that values are present in table rows
	expectedValues := []string{"test user", "test@example.com", "person"}
	for _, expectedValue := range expectedValues {
		found := false
		for _, row := range rows {
			if len(row) >= 2 && strings.Contains(row[1], expectedValue) {
				found = true
				break
			}
		}
		if !found {
			t.Errorf("Expected value '%s' to be present in table", expectedValue)
		}
	}
}

func TestRecordView_buildLines_EmptyEntry(t *testing.T) {
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(nil)

	if len(rv.table.Rows()) != 0 {
		t.Errorf("Expected empty table rows for nil entry, got %d rows", len(rv.table.Rows()))
	}

	if rv.dnHeader != "" {
		t.Errorf("Expected empty DN header for nil entry, got: %s", rv.dnHeader)
	}
}

func TestRecordView_buildLines_NoAttributes(t *testing.T) {
	entry := &ldap.Entry{
		DN:         "cn=empty,dc=example,dc=com",
		Attributes: map[string][]string{},
	}

	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(entry)

	// Should have DN header but no table rows
	if rv.dnHeader == "" {
		t.Fatal("Expected DN header for entry with no attributes")
	}

	if !strings.Contains(rv.dnHeader, "DN: cn=empty,dc=example,dc=com") {
		t.Errorf("Expected DN header, got: %s", rv.dnHeader)
	}

	if len(rv.table.Rows()) != 0 {
		t.Errorf("Expected empty table for entry with no attributes, got %d rows", len(rv.table.Rows()))
	}
}

func TestRecordView_LineWrapping(t *testing.T) {
	// Create a test entry with a very long value
	longValue := "This is a very long value that should definitely wrap across multiple lines when the column width is limited and should not be truncated but instead broken into multiple lines"
	entry := &ldap.Entry{
		DN: "cn=wrap test,dc=example,dc=com",
		Attributes: map[string][]string{
			"description": {longValue},
			"cn":          {"wrap test"},
		},
	}

	// Create record view with small width
	rv := NewRecordView()
	rv.SetSize(50, 20) // Small width
	rv.SetEntry(entry)

	// Check that rows were generated
	rows := rv.table.Rows()
	if len(rows) == 0 {
		t.Fatal("Expected table rows to be generated, but got empty slice")
	}

	// Find the description row and check that the full value is present
	var descriptionRow table.Row
	for _, row := range rows {
		if len(row) >= 2 && row[0] == "description" {
			descriptionRow = row
			break
		}
	}

	if len(descriptionRow) == 0 {
		t.Fatal("Expected to find description row in table")
	}

	// The Bubbles table handles long content internally, so we just verify the content is present
	if !strings.Contains(descriptionRow[1], "This is a very long value") {
		t.Error("Expected long value to be present in table")
	}
	if !strings.Contains(descriptionRow[1], "multiple lines") {
		t.Error("Expected end of long value to be present in table")
	}
}

func TestRecordView_CopyFunctionality(t *testing.T) {
	// Create a test entry
	entry := &ldap.Entry{
		DN: "cn=test user,ou=users,dc=example,dc=com",
		Attributes: map[string][]string{
			"cn":   {"test user"},
			"mail": {"test@example.com"},
		},
	}

	// Create record view and set the entry
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(entry)

	// The table should be focused on the first row (index 0)
	rows := rv.table.Rows()
	if len(rows) == 0 {
		t.Fatal("Expected table to have rows")
	}

	// Test copying the current row value
	cmd := rv.copyCurrentValue()
	if cmd == nil {
		t.Error("Expected copy command to be returned")
	}

	// Check clipboard content - it should contain the value from the selected row
	clipContent, err := clipboard.ReadAll()
	if err != nil {
		t.Skipf("Clipboard not available in test environment: %v", err)
	}

	// The first row should be either "cn" or "mail" (alphabetically sorted)
	// Let's just check that we got some valid content
	if clipContent == "" {
		t.Error("Expected non-empty clipboard content")
	}

	// Verify that the clipboard contains one of our expected values
	if clipContent != "test user" && clipContent != "test@example.com" {
		t.Errorf("Expected clipboard to contain attribute value, got: %s", clipContent)
	}
}

func TestRecordView_CopyKeyBinding(t *testing.T) {
	// Create a test entry
	entry := &ldap.Entry{
		DN: "cn=test user,ou=users,dc=example,dc=com",
		Attributes: map[string][]string{
			"cn": {"test user"},
		},
	}

	// Create record view and set the entry
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(entry)

	// Test 'c' key binding
	model, cmd := rv.Update(tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'c'}})
	if model != rv {
		t.Error("Expected model to be returned unchanged")
	}
	if cmd == nil {
		t.Error("Expected copy command to be returned")
	}

	// Test 'C' key binding
	model, cmd = rv.Update(tea.KeyMsg{Type: tea.KeyRunes, Runes: []rune{'C'}})
	if model != rv {
		t.Error("Expected model to be returned unchanged")
	}
	if cmd == nil {
		t.Error("Expected copy command to be returned")
	}
}

func TestRecordView_LineDataMapping(t *testing.T) {
	// Create a test entry with multiple attributes
	entry := &ldap.Entry{
		DN: "cn=test user,ou=users,dc=example,dc=com",
		Attributes: map[string][]string{
			"cn":          {"test user"},
			"objectClass": {"person", "organizationalPerson"},
			"mail":        {"test@example.com"},
		},
	}

	// Create record view and set the entry
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(entry)

	// Verify that we have table rows for all attributes
	rows := rv.table.Rows()
	expectedRowCount := len(entry.Attributes) // One row per attribute
	if len(rows) != expectedRowCount {
		t.Errorf("Expected %d table rows, got %d", expectedRowCount, len(rows))
	}

	// Verify DN header is set
	if rv.dnHeader == "" {
		t.Error("Expected DN header to be set")
	}
	if !strings.Contains(rv.dnHeader, entry.DN) {
		t.Errorf("Expected DN header to contain DN, got: %s", rv.dnHeader)
	}

	// Verify that each attribute appears in the table
	foundAttribs := make(map[string]bool)
	for _, row := range rows {
		if len(row) >= 1 {
			foundAttribs[row[0]] = true
		}
	}

	for attrName := range entry.Attributes {
		if !foundAttribs[attrName] {
			t.Errorf("Expected attribute '%s' to appear in table", attrName)
		}
	}
}

func TestRecordView_ClickableZones(t *testing.T) {
	// Initialize bubblezone manager for testing
	zone.NewGlobal()

	// Test that record view properly renders clickable zones
	entry := &ldap.Entry{
		DN: "cn=test user,ou=users,dc=example,dc=com",
		Attributes: map[string][]string{
			"cn":         {"test user"},
			"mail":       {"test@example.com"},
			"department": {"Engineering"},
		},
	}

	// Create record view and set the entry
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(entry)

	// Render the view
	view := rv.View()

	// The zone markers don't appear as literal text - they're invisible markers
	// that get processed by zone.Scan(). Check if zones are working by looking
	// for the zone markers (they appear as special characters at the start of marked content)
	hasZoneMarkers := false
	lines := strings.Split(view, "\n")
	for _, line := range lines {
		// Zone markers appear at the beginning of clickable content
		trimmed := strings.TrimSpace(line)
		if len(trimmed) > 0 && trimmed[0] > 127 { // Zone markers are high ASCII
			hasZoneMarkers = true
			break
		}
	}

	if !hasZoneMarkers {
		t.Error("Expected rendered view to contain zone markers")
	}

	// Check that renderedRows is populated
	if len(rv.renderedRows) != len(entry.Attributes) {
		t.Errorf("Expected %d rendered rows, got %d", len(entry.Attributes), len(rv.renderedRows))
	}

	// Verify renderedRows contain the correct data
	expectedAttribs := make(map[string][]string)
	for name, values := range entry.Attributes {
		expectedAttribs[name] = values
	}

	for _, rowData := range rv.renderedRows {
		expectedValues, exists := expectedAttribs[rowData.AttributeName]
		if !exists {
			t.Errorf("Unexpected attribute in renderedRows: %s", rowData.AttributeName)
			continue
		}

		// Check that values match
		if len(rowData.Values) != len(expectedValues) {
			t.Errorf("Attribute %s: expected %d values, got %d",
				rowData.AttributeName, len(expectedValues), len(rowData.Values))
			continue
		}

		for i, value := range rowData.Values {
			if value != expectedValues[i] {
				t.Errorf("Attribute %s, value %d: expected '%s', got '%s'",
					rowData.AttributeName, i, expectedValues[i], value)
			}
		}

		// Remove from expected to track what we've seen
		delete(expectedAttribs, rowData.AttributeName)
	}

	// Check that all attributes were found
	if len(expectedAttribs) > 0 {
		for missing := range expectedAttribs {
			t.Errorf("Attribute '%s' not found in renderedRows", missing)
		}
	}
}
