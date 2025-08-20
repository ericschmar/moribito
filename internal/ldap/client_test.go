package ldap

import (
	"testing"
)

func TestSearchPage(t *testing.T) {
	// Test SearchPage struct initialization
	page := &SearchPage{
		Entries:    []*Entry{},
		HasMore:    false,
		Cookie:     []byte{},
		PageSize:   50,
		TotalCount: -1,
	}

	if page.PageSize != 50 {
		t.Errorf("Expected PageSize to be 50, got %d", page.PageSize)
	}

	if page.HasMore != false {
		t.Errorf("Expected HasMore to be false, got %t", page.HasMore)
	}

	if page.TotalCount != -1 {
		t.Errorf("Expected TotalCount to be -1, got %d", page.TotalCount)
	}
}

func TestEntry(t *testing.T) {
	// Test Entry struct initialization
	entry := &Entry{
		DN:         "cn=test,dc=example,dc=com",
		Attributes: make(map[string][]string),
	}

	entry.Attributes["cn"] = []string{"test"}

	if entry.DN != "cn=test,dc=example,dc=com" {
		t.Errorf("Expected DN to be 'cn=test,dc=example,dc=com', got %s", entry.DN)
	}

	if len(entry.Attributes["cn"]) != 1 || entry.Attributes["cn"][0] != "test" {
		t.Errorf("Expected cn attribute to be ['test'], got %v", entry.Attributes["cn"])
	}
}
