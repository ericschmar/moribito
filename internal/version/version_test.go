package version

import "testing"

func TestVersionInfo(t *testing.T) {
	info := Get()

	// Test that basic fields exist
	if info.Version == "" {
		t.Error("Version should not be empty")
	}

	if info.Commit == "" {
		t.Error("Commit should not be empty")
	}

	if info.Date == "" {
		t.Error("Date should not be empty")
	}

	if info.GoVersion == "" {
		t.Error("GoVersion should not be empty")
	}

	if info.Platform == "" {
		t.Error("Platform should not be empty")
	}

	// Test string methods
	longVersion := info.String()
	shortVersion := info.ShortString()

	if longVersion == "" {
		t.Error("String() should not be empty")
	}

	if shortVersion == "" {
		t.Error("ShortString() should not be empty")
	}

	if shortVersion != info.Version {
		t.Error("ShortString() should equal Version field")
	}
}
