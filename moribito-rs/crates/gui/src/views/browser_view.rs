//! Browser View
//!
//! Main LDAP browser interface with tree navigation, entry details, and search.

use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    resizable::{h_resizable, resizable_panel},
    theme::ActiveTheme,
    v_flex, Root,
};

use crate::actions::*;
use crate::app_state::SharedAppState;
use crate::components::{
    entry_details_table::EntryDetailsTable, ou_filter::OuFilter, search_bar::SearchBar,
    status_bar::StatusBar, tree_view::TreeView,
};
use crate::views::ConfigView;
use gpui::Entity;

/// Main browser view for LDAP navigation
pub struct BrowserView {
    app_state: SharedAppState,
    tree_view: Entity<TreeView>,
    details_table: Entity<EntryDetailsTable>,
    ou_filter: Entity<OuFilter>,
    search_bar: SearchBar,
    status_bar: StatusBar,
    focus_handle: FocusHandle,
    last_initialized_base_dn: Option<String>,
}

impl BrowserView {
    /// Create a new BrowserView
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        let tree_view = cx.new(|cx| TreeView::new(app_state.clone(), window, cx));
        let details_table = cx.new(|cx| EntryDetailsTable::new(app_state.clone(), window, cx));
        let ou_filter = cx.new(|cx| OuFilter::new(app_state.clone(), window, cx));

        // Initialize search bar with base DN if connected
        let base_dn = app_state.read().current_base_dn.clone().unwrap_or_default();
        let search_bar = SearchBar::new("(objectClass=*)", base_dn, window, cx);
        let status_bar = StatusBar::new(app_state.clone());
        let focus_handle = cx.focus_handle();

        Self {
            app_state,
            tree_view,
            details_table,
            ou_filter,
            search_bar,
            status_bar,
            focus_handle,
            last_initialized_base_dn: None,
        }
    }

    /// Handle OpenConfigWindow action
    fn handle_open_config_window(
        &mut self,
        _: &OpenConfigWindow,
        _window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        self.open_config_window(cx);
    }

    /// Handle Disconnect action
    fn handle_disconnect(
        &mut self,
        _: &Disconnect,
        _window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        let mut state = self.app_state.write();
        state.disconnect();
        state.set_status("Disconnected");
        cx.notify();
    }

    /// Handle Refresh action
    fn handle_refresh(&mut self, _: &Refresh, window: &mut Window, cx: &mut Context<Self>) {
        let base_dn = self.app_state.read().current_base_dn.clone();
        if let Some(base_dn) = base_dn {
            self.tree_view
                .update(cx, |tree, cx| tree.reload_tree(&base_dn, window, cx));
        }
    }

    /// Handle ExpandAll action
    fn handle_expand_all(&mut self, _: &ExpandAll, window: &mut Window, cx: &mut Context<Self>) {
        let mut state = self.app_state.write();
        // For now, expand all currently cached nodes
        // TODO: Implement recursive expansion
        let cached_dns: Vec<String> = state.tree_state.node_children.keys().cloned().collect();
        for dn in cached_dns {
            state.tree_state.expanded_nodes.insert(dn);
        }
        state.set_status("Expanded all loaded nodes");
    }

    /// Handle CollapseAll action
    fn handle_collapse_all(
        &mut self,
        _: &CollapseAll,
        _window: &mut Window,
        _cx: &mut Context<Self>,
    ) {
        let mut state = self.app_state.write();
        state.tree_state.expanded_nodes.clear();
        state.set_status("Collapsed all nodes");
    }

    /// Handle FilterByOu action
    fn handle_filter_by_ou(
        &mut self,
        action: &FilterByOu,
        window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        log::info!("🔍 [BrowserView] Filtering tree by OU: {}", action.ou_dn);

        // Update the filter in app state (already done by OuFilter component)
        // Now reload the tree to apply the new filter
        if let Some(base_dn) = self.app_state.read().current_base_dn.clone() {
            self.tree_view
                .update(cx, |tree, cx| tree.reload_tree(&base_dn, window, cx));
        }

        let filter_label = if action.ou_dn == "All" {
            "All entries".to_string()
        } else {
            action.ou_dn.clone()
        };
        self.app_state
            .write()
            .set_status(format!("Filtering by: {}", filter_label));
    }

    /// Initialize the browser with a base DN
    pub fn initialize(&mut self, base_dn: &str, window: &mut Window, cx: &mut Context<Self>) {
        log::info!(
            "🌳 [BrowserView] Initializing tree with base DN: {}",
            base_dn
        );
        self.tree_view
            .update(cx, |tree, cx| tree.reload_tree(base_dn, window, cx));
        self.last_initialized_base_dn = Some(base_dn.to_string());
        log::info!("✅ [BrowserView] Tree initialization complete");
    }

    /// Check if tree needs initialization and trigger it if needed
    fn check_and_initialize(&mut self, window: &mut Window, cx: &mut Context<Self>) {
        // Determine if initialization is needed and get base DN
        let should_init = {
            let app_state = self.app_state.read();

            log::debug!(
                "🔍 [BrowserView] Checking initialization: is_connected={}, current_base_dn={:?}, last_initialized={:?}",
                app_state.is_connected,
                app_state.current_base_dn,
                self.last_initialized_base_dn
            );

            // Only initialize if:
            // 1. Connected to LDAP server
            // 2. Have a base DN
            // 3. Haven't initialized with this base DN yet
            if app_state.is_connected {
                if let Some(ref current_base_dn) = app_state.current_base_dn {
                    if current_base_dn.trim().is_empty() {
                        log::debug!("⚠️  [BrowserView] Base DN is empty, skipping initialization");
                        None
                    } else {
                        let needs_init = match &self.last_initialized_base_dn {
                            None => {
                                log::info!(
                                    "🆕 [BrowserView] Tree never initialized, will initialize now"
                                );
                                true
                            }
                            Some(last_dn) => {
                                let different = last_dn != current_base_dn;
                                if different {
                                    log::info!(
                                        "🔄 [BrowserView] Base DN changed from {} to {}, will reinitialize",
                                        last_dn,
                                        current_base_dn
                                    );
                                }
                                different
                            }
                        };

                        if needs_init {
                            Some(current_base_dn.clone())
                        } else {
                            None
                        }
                    }
                } else {
                    log::debug!("⚠️  [BrowserView] No base DN set, skipping initialization");
                    None
                }
            } else {
                log::debug!("⚠️  [BrowserView] Not connected, skipping initialization");
                None
            }
        };

        // Now that app_state lock is released, we can call initialize
        if let Some(base_dn) = should_init {
            self.initialize(&base_dn, window, cx);
        }
    }

    /// Handle refreshing the selected entry
    fn handle_refresh_entry(
        &mut self,
        _: &RefreshEntry,
        window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        let dn = self
            .app_state
            .read()
            .selected_entry
            .as_ref()
            .map(|e| e.dn.clone());
        if let Some(dn) = dn {
            self.handle_entry_selection(dn, window, cx);
        }
    }

    /// Handle deleting the selected entry
    fn handle_delete_entry(
        &mut self,
        _: &DeleteEntry,
        _window: &mut Window,
        _cx: &mut Context<Self>,
    ) {
        // TODO: Implement entry deletion
        println!("Delete entry not implemented yet");
    }

    /// Handle exporting the selected entry
    fn handle_export_entry(
        &mut self,
        _: &ExportEntry,
        _window: &mut Window,
        _cx: &mut Context<Self>,
    ) {
        // TODO: Implement entry export
        println!("Export entry not implemented yet");
    }

    /// Handle entry selection from the tree view
    fn handle_select_entry(
        &mut self,
        action: &SelectTreeEntry,
        window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        self.handle_entry_selection(action.dn.clone(), window, cx);
    }

    /// Handle entry selection from the tree view
    fn handle_entry_selection(&mut self, dn: String, window: &mut Window, cx: &mut Context<Self>) {
        let mut state = self.app_state.write();

        // Get the LDAP client
        let client = match state.active_client.as_mut() {
            Some(client) => client,
            None => {
                state.set_status("Not connected to LDAP server");
                return;
            }
        };

        // Load the entry
        match client.get_entry(&dn) {
            Ok(entry) => {
                state.set_selected_entry(entry);
                state.set_status(format!("Loaded entry: {}", dn));
            }
            Err(err) => {
                state.set_status(format!("Error loading entry: {}", err));
            }
        }
    }

    /// Open the configuration window
    pub fn open_config_window(&self, cx: &mut Context<Self>) {
        let bounds = Bounds::centered(None, size(px(850.0), px(800.0)), cx);
        let app_state = self.app_state.clone();

        cx.open_window(
            WindowOptions {
                window_bounds: Some(WindowBounds::Windowed(bounds)),
                focus: true,
                show: true,
                kind: WindowKind::Normal,
                is_movable: true,
                titlebar: Some(TitlebarOptions {
                    title: Some("Connection Settings".into()),
                    ..Default::default()
                }),
                ..Default::default()
            },
            |window, cx| {
                let config_view = ConfigView::new(app_state, window, cx);
                cx.new(|cx| Root::new(config_view, window, cx))
            },
        )
        .ok();
    }
}

impl Render for BrowserView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // Check if we need to initialize the tree when connection is established
        self.check_and_initialize(window, cx);

        let background = cx.theme().background;
        let border = cx.theme().border;
        let app_state = self.app_state.clone();

        v_flex()
            .size_full()
            .bg(background)
            .track_focus(&self.focus_handle)
            .on_action(cx.listener(Self::handle_open_config_window))
            .on_action(cx.listener(Self::handle_disconnect))
            .on_action(cx.listener(Self::handle_refresh))
            .on_action(cx.listener(Self::handle_expand_all))
            .on_action(cx.listener(Self::handle_collapse_all))
            .on_action(cx.listener(Self::handle_filter_by_ou))
            .on_action(cx.listener(Self::handle_select_entry))
            .on_action(cx.listener(Self::handle_refresh_entry))
            .on_action(cx.listener(Self::handle_delete_entry))
            .on_action(cx.listener(Self::handle_export_entry))
            .child(
                // Search bar at top
                self.search_bar.render(
                    move |filter, base_dn| {
                        // TODO: Implement search handler
                        println!("Search: {} in {}", filter, base_dn);
                    },
                    move || {
                        // TODO: Implement clear handler
                        println!("Clear search");
                    },
                    window,
                    cx,
                ),
            )
            .child(
                // Main content area with resizable tree and details panels
                div().flex_1().w_full().child(
                    h_resizable("browser-main-split")
                        .child(
                            // Left panel: Tree view with OU filter
                            resizable_panel()
                                .size(px(300.0))
                                .size_range(px(200.0)..px(600.0))
                                .child(
                                    v_flex()
                                        .h_full()
                                        .w_full()
                                        .border_r_1()
                                        .border_color(border)
                                        // OU Filter at top of tree panel
                                        .child(
                                            div()
                                                .p_2()
                                                .border_b_1()
                                                .border_color(border)
                                                .child(self.ou_filter.clone()),
                                        )
                                        // Tree view below filter
                                        .child(
                                            div()
                                                .id("tree-panel-scroll")
                                                .flex_1()
                                                .overflow_y_scroll()
                                                .child(self.tree_view.clone()),
                                        ),
                                ),
                        )
                        .child(
                            // Right panel: Details view
                            v_flex()
                                .h_full()
                                .w_full()
                                .child(self.details_table.clone())
                                .into_any_element(),
                        ),
                ),
            )
            .child(
                // Status bar at bottom
                self.status_bar.render(window, cx),
            )
    }
}

impl Clone for BrowserView {
    fn clone(&self) -> Self {
        Self {
            app_state: self.app_state.clone(),
            tree_view: self.tree_view.clone(),
            details_table: self.details_table.clone(),
            ou_filter: self.ou_filter.clone(),
            search_bar: self.search_bar.clone(),
            status_bar: self.status_bar.clone(),
            focus_handle: self.focus_handle.clone(),
            last_initialized_base_dn: self.last_initialized_base_dn.clone(),
        }
    }
}

impl Focusable for BrowserView {
    fn focus_handle(&self, _cx: &gpui::App) -> FocusHandle {
        self.focus_handle.clone()
    }
}
