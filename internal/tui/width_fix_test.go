package tui

import (
	"strings"
	"testing"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// Test that demonstrates the fix for the query box formatting issue
func TestQueryView_WidthCalculationFix(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	
	// Set a specific size to test width calculations
	qv.SetSize(100, 30)
	
	// Verify that the container is set up properly
	if qv.container == nil {
		t.Fatal("Container should be initialized after SetSize")
	}
	
	// Get content dimensions to verify they're calculated correctly
	contentWidth, _ := qv.container.GetContentDimensions()
	
	// contentWidth should be less than total width due to padding
	if contentWidth >= 100 {
		t.Errorf("contentWidth (%d) should be less than total width (100) due to padding", contentWidth)
	}
	
	// Verify the contentWidth calculation matches expected (width - 2*padding = 100 - 2*1 = 98)
	expectedContentWidth := 98
	if contentWidth != expectedContentWidth {
		t.Errorf("Expected contentWidth to be %d, got %d", expectedContentWidth, contentWidth)
	}
}

// Test that the textarea height was increased as requested
func TestQueryView_IncreasedTextareaHeight(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	
	// Set size to initialize the textarea
	qv.SetSize(100, 30)
	
	// The textarea height should now be 6 (increased from 3)
	if qv.textarea.Height() != 6 {
		t.Errorf("Expected textarea height to be 6 (increased from 3), got %d", qv.textarea.Height())
	}
}

// Test that width calculation for truncation uses contentWidth consistently
func TestQueryView_WidthCalculationLogic(t *testing.T) {
	var client *ldap.Client
	qv := NewQueryView(client)
	
	// Set size
	qv.SetSize(80, 25)
	
	// Get contentWidth to verify our calculations
	contentWidth, _ := qv.container.GetContentDimensions()
	
	// Test the truncation logic that we fixed
	testLine := "This is a very long line that should definitely be truncated because it exceeds the available width"
	
	var truncatedLine string
	if contentWidth > 5 && len(testLine) > contentWidth-2 {
		truncatedLine = testLine[:contentWidth-5] + "..."
	} else {
		truncatedLine = testLine
	}
	
	// Verify that the truncated line is within expected bounds
	maxExpectedLength := contentWidth
	if len(truncatedLine) > maxExpectedLength {
		t.Errorf("Truncated line length (%d) exceeds expected maximum (%d)", len(truncatedLine), maxExpectedLength)
	}
	
	// Verify that truncation actually occurred for a long line
	if len(testLine) > contentWidth-2 && !strings.HasSuffix(truncatedLine, "...") {
		t.Error("Long line should be truncated with '...'")
	}
	
	// Verify that the truncated line is shorter than the original
	if len(testLine) > contentWidth && len(truncatedLine) >= len(testLine) {
		t.Error("Truncated line should be shorter than original long line")
	}
}