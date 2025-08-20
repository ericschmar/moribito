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

func TestRecordView_LineWrapping(t *testing.T) {
	// Create a test entry with a very long value that should wrap
	longValue := "This is a very long value that should definitely wrap across multiple lines when the column width is limited and should not be truncated but instead broken into multiple lines"
	entry := &ldap.Entry{
		DN: "cn=wrap test,dc=example,dc=com",
		Attributes: map[string][]string{
			"description": {longValue},
			"cn":          {"wrap test"},
		},
	}

	// Create record view with small width to force wrapping
	rv := NewRecordView()
	rv.SetSize(50, 20) // Small width to force wrapping
	rv.SetEntry(entry)

	// Check that lines were generated
	if len(rv.lines) == 0 {
		t.Fatal("Expected lines to be generated, but got empty slice")
	}

	// Join all lines and check that the full value is present (not truncated)
	contentStr := strings.Join(rv.lines, "\n")
	if !strings.Contains(contentStr, "This is a very long value") {
		t.Error("Expected long value to be present and not truncated")
	}
	if !strings.Contains(contentStr, "multiple lines") {
		t.Error("Expected end of long value to be present and not truncated")
	}

	// Check that the value was wrapped (should have more lines than attributes)
	// We expect: DN header, empty line, top border, header row, separator,
	// at least 2 rows for description (wrapped), 1 row for cn, bottom border
	expectedMinLines := 8 // minimum if description wraps to 2 lines
	if len(rv.lines) < expectedMinLines {
		t.Errorf("Expected at least %d lines for wrapped content, got %d", expectedMinLines, len(rv.lines))
	}

	// Check that we don't have truncation indicators
	if strings.Contains(contentStr, "...") {
		t.Error("Found truncation indicator '...' - values should be wrapped, not truncated")
	}
}

func TestWrapText(t *testing.T) {
	tests := []struct {
		name     string
		text     string
		width    int
		expected []string
	}{
		{
			name:     "short text no wrap needed",
			text:     "short",
			width:    10,
			expected: []string{"short"},
		},
		{
			name:     "exact width",
			text:     "exact",
			width:    5,
			expected: []string{"exact"},
		},
		{
			name:     "long text needs wrapping",
			text:     "this is a long text that needs wrapping",
			width:    10,
			expected: []string{"this is a", "long text", "that", "needs", "wrapping"},
		},
		{
			name:     "wrap at word boundary",
			text:     "word boundary test",
			width:    8,
			expected: []string{"word", "boundary", "test"},
		},
		{
			name:     "zero width",
			text:     "test",
			width:    0,
			expected: []string{"test"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := wrapText(tt.text, tt.width)
			if len(result) != len(tt.expected) {
				t.Errorf("Expected %d lines, got %d", len(tt.expected), len(result))
				return
			}
			for i, line := range result {
				if line != tt.expected[i] {
					t.Errorf("Line %d: expected '%s', got '%s'", i, tt.expected[i], line)
				}
			}
		})
	}
}
