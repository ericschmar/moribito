package tui

import (
	"strings"
	"testing"

	"github.com/ericschmar/ldap-cli/internal/ldap"
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

	// Check that lines were generated
	if len(rv.lines) == 0 {
		t.Fatal("Expected lines to be generated, but got empty slice")
	}

	// Check DN header is present and styled
	if len(rv.lines) < 1 {
		t.Fatal("Expected at least one line for DN header")
	}

	dnLine := rv.lines[0]
	if !strings.Contains(dnLine, "DN: cn=test user,ou=users,dc=example,dc=com") {
		t.Errorf("Expected DN header to contain the DN, got: %s", dnLine)
	}

	// Check that we have table structure
	// Look for the header row with "Attribute" and "Value(s)"
	var headerFound bool
	for _, line := range rv.lines {
		if strings.Contains(line, "Attribute") && strings.Contains(line, "Value(s)") {
			headerFound = true
			break
		}
	}
	if !headerFound {
		t.Error("Expected to find table header with 'Attribute' and 'Value(s)' columns")
	}

	// Check that attributes are present in the table
	contentStr := strings.Join(rv.lines, "\n")
	expectedAttribs := []string{"cn", "objectClass", "mail", "department"}
	for _, attr := range expectedAttribs {
		if !strings.Contains(contentStr, attr) {
			t.Errorf("Expected attribute '%s' to be present in table", attr)
		}
	}

	// Check that values are present
	expectedValues := []string{"test user", "test@example.com", "person", "Engineering"}
	for _, value := range expectedValues {
		if !strings.Contains(contentStr, value) {
			t.Errorf("Expected value '%s' to be present in table", value)
		}
	}
}

func TestRecordView_buildLines_EmptyEntry(t *testing.T) {
	rv := NewRecordView()
	rv.SetSize(80, 20)
	rv.SetEntry(nil)

	if len(rv.lines) != 0 {
		t.Errorf("Expected empty lines for nil entry, got %d lines", len(rv.lines))
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

	// Should have at least DN header
	if len(rv.lines) < 1 {
		t.Fatal("Expected at least DN header for entry with no attributes")
	}

	dnLine := rv.lines[0]
	if !strings.Contains(dnLine, "DN: cn=empty,dc=example,dc=com") {
		t.Errorf("Expected DN header, got: %s", dnLine)
	}
}
