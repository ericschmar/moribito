package tui

import (
	"testing"
)

func TestGetGradientColor(t *testing.T) {
	testCases := []struct {
		position float64
		expected string
	}{
		{0.0, "#0066CC"}, // Pure blue
		{0.3, "#0066A4"}, // Used in RecordView
		{0.5, "#006690"}, // Used in TreeView
		{0.6, "#00667C"}, // Used in QueryView
		{0.7, "#00667C"}, // Used in StartView (should be same as 0.6)
		{1.0, "#008080"}, // Pure teal
	}

	for _, tc := range testCases {
		t.Run("", func(t *testing.T) {
			result := GetGradientColor(tc.position)
			if result != tc.expected {
				t.Errorf("GetGradientColor(%.1f) = %s, expected %s", tc.position, result, tc.expected)
			}
		})
	}

	// Test edge cases
	t.Run("negative position", func(t *testing.T) {
		result := GetGradientColor(-0.5)
		expected := "#0066CC" // Should clamp to 0
		if result != expected {
			t.Errorf("GetGradientColor(-0.5) = %s, expected %s", result, expected)
		}
	})

	t.Run("position over 1", func(t *testing.T) {
		result := GetGradientColor(1.5)
		expected := "#008080" // Should clamp to 1
		if result != expected {
			t.Errorf("GetGradientColor(1.5) = %s, expected %s", result, expected)
		}
	})
}
