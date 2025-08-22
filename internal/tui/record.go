package tui

import (
	"fmt"
	"image/color"
	"sort"
	"strings"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/table"
	tea "github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
	zone "github.com/lrstanley/bubblezone"
	"github.com/lucasb-eyer/go-colorful"
	"github.com/muesli/gamut"
)

// RecordView displays detailed information about an LDAP entry
type RecordView struct {
	entry     *ldap.Entry
	table     table.Model
	dnHeader  string
	width     int
	height    int
	container *ViewContainer
	// For clickable zones
	renderedRows []RowData // Store row data for click handling
}

// RowData represents a single attribute row with its data
type RowData struct {
	AttributeName string
	Values        []string
}

var (
	// Color blend for table rows - using blue to teal gradient similar to the example
	startColor, _ = colorful.Hex("#0066CC") // Blue
	endColor, _   = colorful.Hex("#008080") // Teal
	blends    = gamut.Blends(lipgloss.Color("#0066CC"), lipgloss.Color("#008080"), 50)
)

// getRowColor returns a color for a table row based on its index
func getRowColor(index int) lipgloss.Color {
	// Create a blend between start and end colors based on row index
	ratio := float64(index%10) / 9.0 // Use modulo 10 to cycle through colors
	blended := startColor.BlendRgb(endColor, ratio)
	return lipgloss.Color(blended.Hex())
}

// NewRecordView creates a new record view
func NewRecordView() *RecordView {
	// Create table with columns
	columns := []table.Column{
		{Title: "Attribute", Width: 20},
		{Title: "Value(s)", Width: 40},
	}

	t := table.New(
		table.WithColumns(columns),
		table.WithRows([]table.Row{}),
		table.WithFocused(true),
		table.WithHeight(10),
	)

	// Style the table
	s := table.DefaultStyles()
	s.Header = s.Header.
		BorderStyle(lipgloss.NormalBorder()).
		BorderForeground(lipgloss.Color("240")).
		BorderBottom(true).
		Bold(false)
	s.Selected = s.Selected.
		Foreground(lipgloss.Color("15")).
		Background(getRowColor(0)). // Use the first color from our blend for selected
		Bold(false)
	t.SetStyles(s)

	return &RecordView{
		table: t,
	}
}

// Init initializes the record view
func (rv *RecordView) Init() tea.Cmd {
	return nil
}

// SetSize sets the size of the record view
func (rv *RecordView) SetSize(width, height int) {
	rv.width = width
	rv.height = height
	rv.container = NewViewContainer(width, height)

	// Get content dimensions accounting for padding
	contentWidth, contentHeight := rv.container.GetContentDimensions()

	// Reserve space for DN header (2 lines)
	tableHeight := contentHeight - 2
	if tableHeight < 3 {
		tableHeight = 3
	}

	// Calculate column widths based on available content space
	nameWidth := contentWidth / 3
	if nameWidth < 15 {
		nameWidth = 15
	}
	valueWidth := contentWidth - nameWidth - 4 // Account for borders and padding
	if valueWidth < 20 {
		valueWidth = 20
		nameWidth = contentWidth - valueWidth - 4
	}

	// Update table dimensions
	columns := []table.Column{
		{Title: "Attribute", Width: nameWidth},
		{Title: "Value(s)", Width: valueWidth},
	}
	rv.table.SetColumns(columns)
	rv.table.SetHeight(tableHeight)
	rv.table.SetWidth(contentWidth)
}

// SetEntry sets the entry to display
func (rv *RecordView) SetEntry(entry *ldap.Entry) {
	rv.entry = entry
	rv.buildTable()
}

// Update handles messages for the record view
func (rv *RecordView) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	switch msg := msg.(type) {
	case tea.KeyMsg:
		switch msg.String() {
		case "c", "C":
			return rv, rv.copyCurrentValue()
		}
	}

	// Forward other messages to the table
	rv.table, cmd = rv.table.Update(msg)
	return rv, cmd
}
func (rv *RecordView) View() string {
	if rv.container == nil {
		rv.container = NewViewContainer(rv.width, rv.height)
	}

	if rv.entry == nil {
		// Make sure we have the same content structure as when we have data
		// This ensures consistent spacing and layout
		content := "No record selected"
		return rv.container.RenderWithPadding(content)
	}

	// Create content with DN header and custom table rendering
	content := rv.dnHeader + "\n\n" + rv.renderTable()
	return rv.container.RenderWithPadding(content)
}


// buildTable builds the table data from the entry
func (rv *RecordView) buildTable() {
	if rv.entry == nil {
		rv.table.SetRows([]table.Row{})
		rv.renderedRows = nil
		rv.dnHeader = ""
		return
	}

	// Build DN header
	contentWidth, _ := rv.container.GetContentDimensions()
	dnStyle := lipgloss.NewStyle().
		Foreground(lipgloss.Color("14")).
		Bold(true).
		Background(lipgloss.Color("238")).
		Width(contentWidth)

	rv.dnHeader = dnStyle.Render(fmt.Sprintf("DN: %s", rv.entry.DN))

	// Build table rows and row data
	var rows []table.Row
	rv.renderedRows = nil

	// Sort attributes for consistent display
	var attrNames []string
	for name := range rv.entry.Attributes {
		attrNames = append(attrNames, name)
	}
	sort.Strings(attrNames)

	// Add attribute rows
	for _, name := range attrNames {
		values := rv.entry.Attributes[name]

		// Store row data for click handling
		rv.renderedRows = append(rv.renderedRows, RowData{
			AttributeName: name,
			Values:        values,
		})

		// Create value display
		var valueText string
		if len(values) == 1 {
			valueText = values[0]
		} else {
			// For multiple values, join with bullet points
			valueText = "• " + strings.Join(values, " • ")
		}

		rows = append(rows, table.Row{name, valueText})
	}

	rv.table.SetRows(rows)
}
func (rv *RecordView) renderTable() string {
	if len(rv.renderedRows) == 0 {
		return "No attributes to display"
	}

	contentWidth, _ := rv.container.GetContentDimensions()
	nameWidth := contentWidth / 3
	if nameWidth < 15 {
		nameWidth = 15
	}
	valueWidth := contentWidth - nameWidth - 4

	// Create table header (unchanged)
	headerStyle := lipgloss.NewStyle().
		Border(lipgloss.NormalBorder()).
		BorderForeground(lipgloss.Color("240")).
		BorderBottom(true).
		Bold(false)

	attributeHeader := lipgloss.NewStyle().Width(nameWidth).Render("Attribute")
	valueHeader := lipgloss.NewStyle().Width(valueWidth).Render("Value(s)")
	header := headerStyle.Render(
		lipgloss.JoinHorizontal(lipgloss.Top, attributeHeader, "  ", valueHeader),
	)

	var rows []string
	rows = append(rows, header)

	currentCursor := rv.table.Cursor()

	for i, rowData := range rv.renderedRows {
		// Create value display
		var valueText string
		if len(rowData.Values) == 1 {
			valueText = rowData.Values[0]
		} else {
			valueText = "• " + strings.Join(rowData.Values, " • ")
		}

		if len(valueText) > valueWidth-3 {
			valueText = valueText[:valueWidth-6] + "..."
		}

		var attrStyle, valueStyle lipgloss.Style

		if i == currentCursor {
			// Selected row: Apply gradient colors
			blendColors := gamut.Blends(lipgloss.Color("#0066CC"), lipgloss.Color("#008080"), 6)
			gradientColors := colorsToHex(blendColors)

			// Use different gradient colors for attribute and value columns
			attrColorIndex := 1 // Use early gradient color for attribute
			valueColorIndex := 4 // Use later gradient color for value

			attrStyle = lipgloss.NewStyle().
				Background(lipgloss.Color(gradientColors[attrColorIndex])).
				Foreground(lipgloss.Color("15")).
				Bold(true).
				Width(nameWidth)
			valueStyle = lipgloss.NewStyle().
				Background(lipgloss.Color(gradientColors[valueColorIndex])).
				Foreground(lipgloss.Color("15")).
				Bold(true).
				Width(valueWidth)
		} else {
			// Normal row: Use subtle/no background
			attrStyle = lipgloss.NewStyle().
				Width(nameWidth)
			valueStyle = lipgloss.NewStyle().
				Width(valueWidth)
		}

		attributeCell := attrStyle.Render(rowData.AttributeName)
		valueCell := valueStyle.Render(valueText)
		rowContent := lipgloss.JoinHorizontal(lipgloss.Top, attributeCell, "  ", valueCell)

		// Add clickable zone
		zoneID := fmt.Sprintf("record-row-%d", i)
		clickableRow := zone.Mark(zoneID, rowContent)

		rows = append(rows, clickableRow)
	}

	return strings.Join(rows, "\n")
}

// copyCurrentValue copies the current row's value to clipboard
func (rv *RecordView) copyCurrentValue() tea.Cmd {
	if rv.entry == nil {
		return SendError(fmt.Errorf("no record selected"))
	}

	selectedRow := rv.table.Cursor()
	rows := rv.table.Rows()

	if selectedRow < 0 || selectedRow >= len(rows) {
		return SendError(fmt.Errorf("no row selected"))
	}

	row := rows[selectedRow]
	if len(row) < 2 {
		return SendError(fmt.Errorf("invalid row data"))
	}

	attributeName := row[0]

	// Get the original values from the entry for copying
	values, exists := rv.entry.Attributes[attributeName]
	if !exists {
		return SendError(fmt.Errorf("attribute not found"))
	}

	// Join multiple values with comma and space
	valueText := strings.Join(values, ", ")

	// Copy the value to clipboard
	err := clipboard.WriteAll(valueText)
	if err != nil {
		return SendError(fmt.Errorf("failed to copy to clipboard: %w", err))
	}

	// Provide feedback about what was copied
	msg := fmt.Sprintf("Copied %s value to clipboard", attributeName)
	return SendStatus(msg)
}
// Helper function to convert gamut colors to hex strings
func colorsToHex(colors []color.Color) []string {
	var hexColors []string
	for _, c := range colors {
		if colorfulColor, ok := c.(colorful.Color); ok {
			hexColors = append(hexColors, colorfulColor.Hex())
		} else {
			// Fallback: convert via colorful
			colorfulColor, _ := colorful.MakeColor(c)
			hexColors = append(hexColors, colorfulColor.Hex())
		}
	}
	return hexColors
}
