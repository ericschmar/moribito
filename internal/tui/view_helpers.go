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

	// Only apply container styling for content that doesn't have proper styling already
	if vc.shouldApplyContainerStyling(content) {
		style := lipgloss.NewStyle().
			MaxWidth(vc.width).
			MaxHeight(vc.height).
			Padding(0, vc.padding)
		content = style.Render(content)
	}

	// CRITICAL: Enforce height constraints AFTER styling to prevent content from pushing UI elements off screen
	// This accounts for borders, padding, and other styling that lipgloss adds
	lines := strings.Split(content, "\n")

	// Enforce maximum height to prevent UI overflow
	if len(lines) > vc.height {
		lines = lines[:vc.height]
		// Add truncation indicator if content was cut off
		if vc.height > 0 {
			lastLine := lines[vc.height-1]
			// Find the actual content in the line (skip ANSI codes and box drawing)
			visibleLen := lipgloss.Width(lastLine)
			if visibleLen > 3 {
				// Truncate visible content and add ellipsis
				lines[vc.height-1] = truncateWithEllipsis(lastLine, visibleLen-3)
			} else {
				lines[vc.height-1] = "..."
			}
		}
	}

	// Return the height-enforced content
	return strings.Join(lines, "\n")
}

// truncateWithEllipsis truncates a line to the specified visible width and adds ellipsis
func truncateWithEllipsis(line string, targetWidth int) string {
	// Simple truncation - for styled text, we keep the beginning which usually has the ANSI codes
	currentWidth := lipgloss.Width(line)
	if currentWidth <= targetWidth+3 {
		return line + "..."
	}

	// Truncate by removing characters from the end until we fit
	runes := []rune(line)
	for len(runes) > 0 && lipgloss.Width(string(runes)) > targetWidth {
		runes = runes[:len(runes)-1]
	}
	return string(runes) + "..."
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
		MaxHeight(vc.height). // Use MaxHeight instead of Height
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

// GetGradientColor returns a blue-to-teal gradient color based on position
// position should be 0-1 where 0 is pure blue and 1 is pure teal
func GetGradientColor(position float64) string {
	// Clamp position between 0 and 1
	if position < 0 {
		position = 0
	}
	if position > 1 {
		position = 1
	}

	// Define blue-to-teal gradient color progression
	// Using hex colors that transition from blue (#0066CC) to teal (#008080)
	colors := []string{
		"#0066CC", // Blue
		"#0066B8", // Blue-teal 1
		"#0066A4", // Blue-teal 2
		"#006690", // Blue-teal 3
		"#00667C", // Blue-teal 4
		"#006668", // Blue-teal 5
		"#006654", // Blue-teal 6
		"#008080", // Teal
	}

	// Calculate which color to use based on position
	index := int(position * float64(len(colors)-1))
	if index >= len(colors) {
		index = len(colors) - 1
	}

	return colors[index]
}
