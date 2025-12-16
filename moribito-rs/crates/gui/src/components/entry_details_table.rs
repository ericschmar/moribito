//! Entry Details Table Component
//!
//! Displays LDAP entry attributes in a table format with action buttons.

use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    button::{Button, ButtonVariants},
    h_flex,
    table::{Column, Table, TableDelegate, TableState},
    theme::ActiveTheme,
    v_flex, Disableable, Sizable,
};
use moribito_core::types::Entry;
use once_cell::sync::Lazy;

/// Row data for the attributes table
#[derive(Clone, Debug)]
struct AttributeRow {
    attribute: String,
    value: String,
}

/// Static column definitions
static COLUMNS: Lazy<Vec<Column>> = Lazy::new(|| {
    vec![
        Column::new("attribute", "Attribute")
            .width(200.0)
            .sortable()
            .resizable(true),
        Column::new("value", "Value").width(400.0).resizable(true),
    ]
});

/// Table delegate for LDAP entry attributes
struct AttributesTableDelegate {
    rows: Vec<AttributeRow>,
}

impl AttributesTableDelegate {
    fn new(rows: Vec<AttributeRow>) -> Self {
        Self { rows }
    }
}

impl TableDelegate for AttributesTableDelegate {
    fn columns_count(&self, _cx: &App) -> usize {
        2
    }

    fn rows_count(&self, _cx: &App) -> usize {
        self.rows.len()
    }

    fn column(&self, ix: usize, _cx: &App) -> &Column {
        &COLUMNS[ix]
    }

    fn render_th(
        &mut self,
        col_ix: usize,
        _window: &mut Window,
        cx: &mut Context<TableState<Self>>,
    ) -> AnyElement {
        let theme = cx.theme();
        let col = &COLUMNS[col_ix];
        div()
            .text_size(px(12.0))
            .text_color(theme.foreground)
            .font_weight(FontWeight::SEMIBOLD)
            .child(col.name.clone())
            .into_any_element()
    }

    fn render_td(
        &mut self,
        row_ix: usize,
        col_ix: usize,
        _window: &mut Window,
        cx: &mut Context<TableState<Self>>,
    ) -> AnyElement {
        let theme = cx.theme();

        if row_ix >= self.rows.len() {
            return div().into_any_element();
        }

        let row = &self.rows[row_ix];
        let content = match col_ix {
            0 => row.attribute.clone(),
            1 => row.value.clone(),
            _ => String::new(),
        };

        div()
            .text_size(px(13.0))
            .text_color(theme.foreground)
            .child(content)
            .into_any_element()
    }
}

/// Entry details table component for displaying LDAP entry attributes
pub struct EntryDetailsTable {
    entry: Option<Entry>,
    table_state: Entity<TableState<AttributesTableDelegate>>,
}

impl EntryDetailsTable {
    /// Create a new EntryDetailsTable
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        let delegate = AttributesTableDelegate::new(vec![]);
        let table_state = cx.new(|cx| TableState::new(delegate, window, cx));

        Self {
            entry: None,
            table_state,
        }
    }

    /// Set the entry to display
    pub fn set_entry(&mut self, entry: Option<Entry>, window: &mut Window, cx: &mut App) {
        self.entry = entry;
        self.update_table(window, cx);
    }

    /// Update the table with current entry data
    fn update_table(&mut self, window: &mut Window, cx: &mut App) {
        if let Some(ref entry) = self.entry {
            // Build rows from attributes
            let mut rows: Vec<AttributeRow> = entry
                .attributes
                .iter()
                .flat_map(|(attr, values)| {
                    if values.len() == 1 {
                        // Single value - one row
                        vec![AttributeRow {
                            attribute: attr.clone(),
                            value: values[0].clone(),
                        }]
                    } else {
                        // Multiple values - one row per value
                        values
                            .iter()
                            .map(|val| AttributeRow {
                                attribute: attr.clone(),
                                value: val.clone(),
                            })
                            .collect()
                    }
                })
                .collect();

            // Sort by attribute name
            rows.sort_by(|a, b| a.attribute.cmp(&b.attribute));

            let delegate = AttributesTableDelegate::new(rows);
            self.table_state.update(cx, |state, cx| {
                *state = TableState::new(delegate, window, cx);
            });
        } else {
            // Clear table
            let delegate = AttributesTableDelegate::new(vec![]);
            self.table_state.update(cx, |state, cx| {
                *state = TableState::new(delegate, window, cx);
            });
        }
    }

    /// Render the entry details
    pub fn render<R, D, X>(
        &self,
        on_refresh: R,
        on_delete: D,
        on_export: X,
        _window: &mut Window,
        cx: &mut App,
    ) -> impl IntoElement
    where
        R: Fn() + 'static,
        D: Fn() + 'static,
        X: Fn() + 'static,
    {
        let theme = cx.theme();

        v_flex()
            .w_full()
            .h_full()
            .gap_2()
            .bg(theme.background)
            .when_some(self.entry.as_ref(), |this, entry| {
                this.child(
                    // Header with DN and actions
                    v_flex()
                        .w_full()
                        .gap_2()
                        .p_3()
                        .border_b_1()
                        .border_color(theme.border)
                        .child(
                            div()
                                .text_size(px(11.0))
                                .text_color(theme.muted_foreground)
                                .child("Distinguished Name"),
                        )
                        .child(
                            div()
                                .text_size(px(13.0))
                                .text_color(theme.foreground)
                                .font_weight(FontWeight::SEMIBOLD)
                                .child(entry.dn.clone()),
                        )
                        .child(
                            // Action buttons
                            h_flex()
                                .gap_2()
                                .mt_2()
                                .child(
                                    Button::new("refresh")
                                        .outline()
                                        .small()
                                        .label("Refresh")
                                        .on_click(move |_event, _window, _cx| {
                                            on_refresh();
                                        }),
                                )
                                .child(
                                    Button::new("export")
                                        .outline()
                                        .small()
                                        .label("Export")
                                        .on_click(move |_event, _window, _cx| {
                                            on_export();
                                        }),
                                )
                                .child(
                                    Button::new("delete")
                                        .danger()
                                        .small()
                                        .label("Delete")
                                        .on_click(move |_event, _window, _cx| {
                                            on_delete();
                                        }),
                                ),
                        ),
                )
                .child(
                    // Attributes table
                    v_flex()
                        .flex_1()
                        .overflow_hidden()
                        .child(Table::new(&self.table_state).stripe(true)),
                )
            })
            .when(self.entry.is_none(), |this| {
                this.child(
                    v_flex()
                        .w_full()
                        .h_full()
                        .items_center()
                        .justify_center()
                        .child(
                            div()
                                .text_size(px(14.0))
                                .text_color(theme.muted_foreground)
                                .child("Select an entry to view details"),
                        ),
                )
            })
    }
}

impl Clone for EntryDetailsTable {
    fn clone(&self) -> Self {
        Self {
            entry: self.entry.clone(),
            table_state: self.table_state.clone(),
        }
    }
}
