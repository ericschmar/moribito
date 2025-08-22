package updater

import (
	"context"
	"testing"
)

func TestNew(t *testing.T) {
	checker := New("ericschmar", "moribito")
	if checker == nil {
		t.Fatal("Expected non-nil checker")
	}

	if checker.owner != "ericschmar" {
		t.Errorf("Expected owner 'ericschmar', got '%s'", checker.owner)
	}

	if checker.repo != "moribito" {
		t.Errorf("Expected repo 'moribito', got '%s'", checker.repo)
	}

	if checker.client == nil {
		t.Error("Expected non-nil HTTP client")
	}
}

func TestIsNewerVersion(t *testing.T) {
	tests := []struct {
		name     string
		latest   string
		current  string
		expected bool
	}{
		{
			name:     "dev version should always need update",
			latest:   "v0.0.1",
			current:  "dev",
			expected: true,
		},
		{
			name:     "same version should not need update",
			latest:   "v0.0.1",
			current:  "v0.0.1",
			expected: false,
		},
		{
			name:     "newer version available",
			latest:   "v0.1.0",
			current:  "v0.0.1",
			expected: true,
		},
		{
			name:     "current version is newer",
			latest:   "v0.0.1",
			current:  "v0.1.0",
			expected: false,
		},
		{
			name:     "versions without v prefix",
			latest:   "0.1.0",
			current:  "0.0.1",
			expected: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := isNewerVersion(tt.latest, tt.current)
			if result != tt.expected {
				t.Errorf("isNewerVersion(%s, %s) = %v, want %v",
					tt.latest, tt.current, result, tt.expected)
			}
		})
	}
}

func TestCheckForUpdate_Integration(t *testing.T) {
	// This is an integration test - only run if we have network access
	// Skip in CI environments or if no network is available
	if testing.Short() {
		t.Skip("Skipping integration test in short mode")
	}

	checker := New("ericschmar", "moribito")
	ctx := context.Background()

	// Test with dev version - should always return update available
	release, err := checker.CheckForUpdate(ctx, "dev")
	if err != nil {
		t.Errorf("Unexpected error: %v", err)
	}

	if release == nil {
		t.Error("Expected update available for dev version")
	} else {
		if release.TagName == "" {
			t.Error("Expected non-empty tag name")
		}
		if release.Name == "" {
			t.Error("Expected non-empty name")
		}
		if release.URL == "" {
			t.Error("Expected non-empty URL")
		}
	}
}
