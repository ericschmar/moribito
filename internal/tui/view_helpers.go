package tui

import (
	"strings"

	"github.com/charmbracelet/lipgloss"
)

// ViewContainer provides consistent sizing and basic rendering for all views
type ViewContainer struct {
	width   int
	height  int
	padding int
}

// NewViewContainer creates a new view container with consistent sizing
func NewViewContainer(width, height int) *ViewContainer {
	return &ViewContainer{
		width:   width,
		height:  height,
		padding: 1, // Consistent horizontal padding for all views
	}
}

// RenderWithPadding renders content with minimal processing, letting child components handle their own styling
func (vc *ViewContainer) RenderWithPadding(content string) string {
	// For properly styled content (like our refactored StartView),
	// we should avoid aggressive processing and let LipGloss handle the layout

	// However, we MUST enforce height constraints to prevent content from pushing UI elements off screen
	lines := strings.Split(content, "\n")

	// Enforce maximum height to prevent UI overflow
	if len(lines) > vc.height {
		lines = lines[:vc.height]
		// Add truncation indicator if content was cut off
		if vc.height > 0 {
			lastLine := lines[vc.height-1]
			if len(lastLine) > 3 {
				lines[vc.height-1] = lastLine[:len(lastLine)-3] + "..."
			} else {
				lines[vc.height-1] = "..."
			}
		}
	}

	// Rejoin the content
	content = strings.Join(lines, "\n")

	// Only apply container styling for content that doesn't have proper styling already
	if vc.shouldApplyContainerStyling(content) {
		style := lipgloss.NewStyle().
			MaxWidth(vc.width).
			MaxHeight(vc.height).
			Padding(0, vc.padding)
		return style.Render(content)
	}

	// For content that already has proper styling (borders, etc.), return with height enforcement
	return content
}

// shouldApplyContainerStyling determines if the container should apply additional styling
func (vc *ViewContainer) shouldApplyContainerStyling(content string) bool {
	// If content already has borders (indicated by box drawing characters),
	// don't apply additional container styling
	boxDrawingChars := "┌┐└┘─│┬┴├┤┼╭╮╰╯"
	for _, char := range boxDrawingChars {
		if strings.ContainsRune(content, char) {
			return false
		}
	}

	// Also check for rounded border characters
	roundedBorderChars := "╭╮╰╯"
	for _, char := range roundedBorderChars {
		if strings.ContainsRune(content, char) {
			return false
		}
	}

	return true
}

// RenderCentered renders content centered in the view
func (vc *ViewContainer) RenderCentered(content string) string {
	style := lipgloss.NewStyle().
		Width(vc.width).
		Height(vc.height).
		AlignHorizontal(lipgloss.Center).
		AlignVertical(lipgloss.Center).
		Padding(0, vc.padding)

	return style.Render(content)
}

// RenderPlain renders content with basic truncation for simple views
func (vc *ViewContainer) RenderPlain(content string) string {
	lines := strings.Split(content, "\n")
	contentWidth := vc.width - (vc.padding * 2)
	if contentWidth < 1 {
		contentWidth = 1
	}

	var processedLines []string
	for _, line := range lines {
		if len(line) <= contentWidth {
			processedLines = append(processedLines, line)
		} else {
			// Only truncate for plain content
			processedLines = append(processedLines, line[:contentWidth-3]+"...")
		}
	}

	// Enforce height limit for plain content
	if len(processedLines) > vc.height {
		processedLines = processedLines[:vc.height]
	}

	finalContent := strings.Join(processedLines, "\n")

	style := lipgloss.NewStyle().
		MaxWidth(vc.width).
		MaxHeight(vc.height).
		Padding(0, vc.padding)

	return style.Render(finalContent)
}

// GetContentDimensions returns the available content area dimensions
func (vc *ViewContainer) GetContentDimensions() (width, height int) {
	contentWidth := vc.width - (vc.padding * 2)
	if contentWidth < 1 {
		contentWidth = 1
	}
	return contentWidth, vc.height
}
