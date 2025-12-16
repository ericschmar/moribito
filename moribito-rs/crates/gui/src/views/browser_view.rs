//! Browser View
//!
//! Main LDAP browser interface with tree navigation, entry details, and search.

use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    h_flex,
    resizable::{h_resizable, resizable_panel},
    theme::ActiveTheme,
    v_flex, Root,
};

use crate::actions::*;
use crate::app_state::SharedAppState;
use crate::components::{
    entry_details_table::EntryDetailsTable, search_bar::SearchBar, status_bar::StatusBar,
    tree_view::TreeView,
};
use crate::views::ConfigView;

/// Main browser view for LDAP navigation
pub struct BrowserView {
    app_state: SharedAppState,
    tree_view: TreeView,
    details_table: EntryDetailsTable,
    search_bar: SearchBar,
    status_bar: StatusBar,
    focus_handle: FocusHandle,
    last_initialized_base_dn: Option<String>,
}

impl BrowserView {
    /// Create a new BrowserView
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        let tree_view = TreeView::new(app_state.clone(), window, cx);
        let details_table = EntryDetailsTable::new(window, cx);

        // Initialize search bar with base DN if connected
        let base_dn = app_state.read().current_base_dn.clone().unwrap_or_default();
        let search_bar = SearchBar::new("(objectClass=*)", base_dn, window, cx);
        let status_bar = StatusBar::new(app_state.clone());
        let focus_handle = cx.focus_handle();

        Self {
            app_state,
            tree_view,
            details_table,
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

    /// Handle Refresh action
    fn handle_refresh(&mut self, _: &Refresh, _window: &mut Window, _cx: &mut Context<Self>) {
        // TODO: Implement refresh logic
        println!("Refresh action triggered");
    }

    /// Handle ExpandAll action
    fn handle_expand_all(&mut self, _: &ExpandAll, _window: &mut Window, _cx: &mut Context<Self>) {
        // TODO: Implement expand all logic
        println!("Expand All action triggered");
    }

    /// Handle CollapseAll action
    fn handle_collapse_all(
        &mut self,
        _: &CollapseAll,
        _window: &mut Window,
        _cx: &mut Context<Self>,
    ) {
        // TODO: Implement collapse all logic
        println!("Collapse All action triggered");
    }

    /// Initialize the browser with a base DN
    pub fn initialize(&mut self, base_dn: &str, window: &mut Window, cx: &mut App) {
        self.tree_view.reload_tree(base_dn, window, cx);
        self.last_initialized_base_dn = Some(base_dn.to_string());
    }

    /// Check if tree needs initialization and trigger it if needed
    fn check_and_initialize(&mut self, window: &mut Window, cx: &mut App) {
        // Determine if initialization is needed and get base DN
        let should_init = {
            let app_state = self.app_state.read();

            // Only initialize if:
            // 1. Connected to LDAP server
            // 2. Have a base DN
            // 3. Haven't initialized with this base DN yet
            if app_state.is_connected {
                if let Some(ref current_base_dn) = app_state.current_base_dn {
                    let needs_init = match &self.last_initialized_base_dn {
                        None => true,  // Never initialized
                        Some(last_dn) => last_dn != current_base_dn,  // Different base DN
                    };

                    if needs_init {
                        Some(current_base_dn.clone())
                    } else {
                        None
                    }
                } else {
                    None
                }
            } else {
                None
            }
        };

        // Now that app_state lock is released, we can call initialize
        if let Some(base_dn) = should_init {
            self.initialize(&base_dn, window, cx);
        }
    }

    /// Open the configuration window
    pub fn open_config_window(&self, cx: &mut Context<Self>) {
        let bounds = Bounds::centered(None, size(px(700.0), px(650.0)), cx);
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
            .on_action(cx.listener(Self::handle_refresh))
            .on_action(cx.listener(Self::handle_expand_all))
            .on_action(cx.listener(Self::handle_collapse_all))
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
                            // Left panel: Tree view
                            resizable_panel()
                                .size(px(300.0))
                                .size_range(px(200.0)..px(600.0))
                                .child(
                                    v_flex()
                                        .id("tree-panel-scroll")
                                        .h_full()
                                        .w_full()
                                        .border_r_1()
                                        .border_color(border)
                                        .overflow_y_scroll()
                                        .child(self.tree_view.render(
                                            move |dn| {
                                                // TODO: Implement entry selection handler
                                                println!("Selected: {}", dn);
                                            },
                                            window,
                                            cx,
                                        )),
                                ),
                        )
                        .child(
                            // Right panel: Details view
                            v_flex()
                                .h_full()
                                .w_full()
                                .child(self.details_table.render(
                                    move || {
                                        // TODO: Implement refresh handler
                                        println!("Refresh entry");
                                    },
                                    move || {
                                        // TODO: Implement delete handler
                                        println!("Delete entry");
                                    },
                                    move || {
                                        // TODO: Implement export handler
                                        println!("Export entry");
                                    },
                                    window,
                                    cx,
                                ))
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
