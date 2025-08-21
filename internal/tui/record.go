package tui

import (
	"fmt"
	"sort"
	"strings"

	"github.com/atotto/clipboard"
	"github.com/charmbracelet/bubbles/table"
	"github.com/charmbracelet/bubbletea"
	"github.com/charmbracelet/lipgloss"
	"github.com/ericschmar/ldap-cli/internal/ldap"
)

// RecordView displays detailed information about an LDAP entry
type RecordView struct {
	entry     *ldap.Entry
	table     table.Model
	dnHeader  string
	width     int
	height    int
	container *ViewContainer
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
		Background(lipgloss.Color("240")).
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

// View renders the record view
func (rv *RecordView) View() string {
	if rv.container == nil {
		rv.container = NewViewContainer(rv.width, rv.height)
	}
	
	if rv.entry == nil {
		return rv.container.RenderCentered("No record selected")
	}
	
	// Create content with DN header and table
	content := rv.dnHeader + "\n\n" + rv.table.View()
	return rv.container.RenderWithPadding(content)
}

// buildTable builds the table data from the entry
func (rv *RecordView) buildTable() {
	if rv.entry == nil {
		rv.table.SetRows([]table.Row{})
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

	// Build table rows
	var rows []table.Row

	// Sort attributes for consistent display
	var attrNames []string
	for name := range rv.entry.Attributes {
		attrNames = append(attrNames, name)
	}
	sort.Strings(attrNames)

	// Add attribute rows
	for _, name := range attrNames {
		values := rv.entry.Attributes[name]

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
