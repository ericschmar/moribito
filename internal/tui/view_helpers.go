package tui

import (
	"strings"

	"github.com/charmbracelet/lipgloss"
)

// ViewContainer provides consistent styling for all views
type ViewContainer struct {
	width   int
	height  int
	padding int
}

// NewViewContainer creates a new view container with consistent styling
func NewViewContainer(width, height int) *ViewContainer {
	return &ViewContainer{
		width:   width,
		height:  height,
		padding: 1, // Consistent horizontal padding for all views
	}
}

// RenderWithPadding renders content with consistent padding and ensures exact height
func (vc *ViewContainer) RenderWithPadding(content string) string {
	// Split content into lines
	lines := strings.Split(content, "\n")

	// Calculate available content area (accounting for padding)
	contentWidth := vc.width - (vc.padding * 2)
	if contentWidth < 1 {
		contentWidth = 1
	}

	// Process each line to fit within content width
	var processedLines []string
	for _, line := range lines {
		if len(line) <= contentWidth {
			processedLines = append(processedLines, line)
		} else {
			// Truncate long lines with ellipsis
			processedLines = append(processedLines, line[:contentWidth-3]+"...")
		}
	}

	// Ensure we have exactly the right number of lines for the height
	for len(processedLines) < vc.height {
		processedLines = append(processedLines, "")
	}
	if len(processedLines) > vc.height {
		processedLines = processedLines[:vc.height]
	}

	// Create the final content
	finalContent := strings.Join(processedLines, "\n")

	// Apply consistent styling with padding
	style := lipgloss.NewStyle().
		Width(vc.width).
		Height(vc.height).
		Padding(0, vc.padding) // Apply horizontal padding

	return style.Render(finalContent)
}

// RenderCentered renders content centered in the view with consistent padding
func (vc *ViewContainer) RenderCentered(content string) string {
	style := lipgloss.NewStyle().
		Width(vc.width).
		Height(vc.height).
		AlignHorizontal(lipgloss.Center).
		AlignVertical(lipgloss.Center).
		Padding(0, vc.padding)

	return style.Render(content)
}

// GetContentDimensions returns the available content area dimensions
func (vc *ViewContainer) GetContentDimensions() (width, height int) {
	contentWidth := vc.width - (vc.padding * 2)
	if contentWidth < 1 {
		contentWidth = 1
	}
	return contentWidth, vc.height
}
