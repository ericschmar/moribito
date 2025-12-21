//! OU Filter Component
//!
//! Provides a dropdown to filter the LDAP tree by Organizational Unit (OU).

use crate::actions::FilterByOu;
use crate::app_state::SharedAppState;
use gpui::*;
use gpui_component::{
    h_flex,
    label::Label,
    select::{Select, SelectEvent, SelectItem, SelectState},
    theme::ActiveTheme,
    IndexPath,
};

/// OU select item - represents an OU option in the dropdown
#[derive(Clone, Debug)]
struct OuSelectItem {
    dn: String,
    label: String,
}

impl SelectItem for OuSelectItem {
    type Value = String;

    fn title(&self) -> SharedString {
        self.label.clone().into()
    }

    fn value(&self) -> &Self::Value {
        &self.dn
    }

    fn matches(&self, query: &str) -> bool {
        let query_lower = query.to_lowercase();
        self.label.to_lowercase().contains(&query_lower)
            || self.dn.to_lowercase().contains(&query_lower)
    }
}

/// OU Filter component - dropdown to filter the LDAP tree by OU
pub struct OuFilter {
    app_state: SharedAppState,
    select_state: Entity<SelectState<Vec<OuSelectItem>>>,
}

impl OuFilter {
    /// Create a new OU filter component
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        // Build initial items list
        let items = Self::build_items(&app_state);
        let current_filter = app_state.read().tree_state.ou_filter.clone();

        // Find the index of the current filter
        let selected_index = items
            .iter()
            .position(|item| item.dn == current_filter)
            .map(|idx| IndexPath::default().row(idx));

        // Create the select state
        // Vec<OuSelectItem> automatically implements SelectDelegate
        let select_state = cx.new(|cx| {
            SelectState::new(items, selected_index, window, cx)
                .searchable(true)
        });

        let app_state_clone = app_state.clone();

        // Subscribe to selection changes
        cx.subscribe(&select_state, move |_select_state: Entity<SelectState<Vec<OuSelectItem>>>, event: &SelectEvent<Vec<OuSelectItem>>, cx: &mut App| {
            if let SelectEvent::Confirm(Some(dn)) = event {
                // Update the filter in app state
                let mut state = app_state_clone.write();
                state.tree_state.ou_filter = dn.clone();

                // Dispatch FilterByOu action to trigger tree reload
                let action = FilterByOu {
                    ou_dn: dn.clone(),
                };
                cx.dispatch_action(&action);
            }
        })
        .detach();

        Self {
            app_state,
            select_state,
        }
    }

    /// Build the list of OU items from app state
    fn build_items(app_state: &SharedAppState) -> Vec<OuSelectItem> {
        let state = app_state.read();

        // Start with "All" option
        let mut items = vec![OuSelectItem {
            dn: "All".to_string(),
            label: "All OUs".to_string(),
        }];

        // Add available OUs
        for ou in &state.tree_state.available_ous {
            items.push(OuSelectItem {
                dn: ou.dn.clone(),
                label: ou.label.clone(),
            });
        }

        items
    }

    /// Update the items in the select dropdown
    pub fn update_items(&mut self, window: &mut Window, cx: &mut App) {
        let items = Self::build_items(&self.app_state);
        let current_filter = self.app_state.read().tree_state.ou_filter.clone();

        // Find the index of the current filter
        let selected_index = items
            .iter()
            .position(|item| item.dn == current_filter)
            .map(|idx| IndexPath::default().row(idx));

        // Update the select state
        self.select_state.update(cx, |state, cx| {
            state.set_items(items, window, cx);
            state.set_selected_index(selected_index, window, cx);
        });
    }
}

impl Render for OuFilter {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();

        h_flex()
            .gap_2()
            .items_center()
            .child(
                Select::new(&self.select_state)
                    .w(px(250.0))
                    .placeholder("Select OU...")
                    .cleanable(true),
            )
    }
}

impl Clone for OuFilter {
    fn clone(&self) -> Self {
        Self {
            app_state: self.app_state.clone(),
            select_state: self.select_state.clone(),
        }
    }
}
