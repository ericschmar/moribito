//! Tree View Component
//!
//! Displays LDAP directory hierarchy as an expandable tree.

use gpui::prelude::*;
use gpui::*;
use gpui_component::tree::{tree, TreeItem, TreeState};
use gpui_component::{h_flex, list::ListItem, theme::ActiveTheme, Icon, IconName};
use moribito_core::types::TreeNode;

use crate::actions::*;
use crate::app_state::SharedAppState;
use gpui::{EventEmitter, Entity};

/// Tree view component for LDAP directory navigation
pub struct TreeView {
    app_state: SharedAppState,
    tree_state: Entity<TreeState>,
    focus_handle: FocusHandle,
    pub(crate) entity: Entity<Self>,
}

impl TreeView {
    /// Create a new TreeView
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        // Initialize with empty tree state
        let tree_state = cx.new(|cx| TreeState::new(cx));
        let focus_handle = cx.focus_handle();

        Self {
            app_state,
            tree_state,
            focus_handle,
            entity: unsafe { std::mem::zeroed() }, // Will be set later
        }
    }

    /// Update the tree with root nodes
    pub fn set_root_nodes(&mut self, nodes: Vec<TreeNode>, cx: &mut App) {
        let items: Vec<TreeItem> = nodes
            .iter()
            .map(|node| self.tree_node_to_tree_item(node, cx))
            .collect();

        self.tree_state.update(cx, |state, cx| {
            *state = TreeState::new(cx).items(items);
        });
    }

    /// Convert a TreeNode to a TreeItem
    fn tree_node_to_tree_item(&self, node: &TreeNode, _cx: &mut App) -> TreeItem {
        let state = self.app_state.read();
        let is_expanded = state.is_node_expanded(&node.dn);

        let mut item = TreeItem::new(node.dn.clone(), node.name.clone());

        // Check if we have cached children
        if let Some(children) = state.get_cached_children(&node.dn) {
            let child_items: Vec<TreeItem> = children
                .iter()
                .map(|child| self.tree_node_to_tree_item(child, _cx))
                .collect();

            item = item.children(child_items);
        }

        if is_expanded {
            item = item.expanded(true);
        }

        item
    }

    /// Load children for a node
    pub fn load_children(&self, dn: &str, window: &mut Window, cx: &mut Context<Self>) {
        let mut state = self.app_state.write();

        // Check if already loading or already cached
        if state.is_node_loading(dn) || state.get_cached_children(dn).is_some() {
            return;
        }

        // Mark as loading
        state.set_node_loading(dn.to_string(), true);

        // Get the LDAP client
        let client = match state.active_client.as_mut() {
            Some(client) => client,
            None => {
                state.set_node_loading(dn.to_string(), false);
                state.set_status("Not connected to LDAP server");
                return;
            }
        };

        // Load children from LDAP
        match client.get_children(dn) {
            Ok(children) => {
                state.cache_node_children(dn.to_string(), children.clone());
                state.set_node_loading(dn.to_string(), false);
                state.set_status(format!("Loaded {} children for {}", children.len(), dn));

                // Update the tree
                drop(state);
                if let Some(base_dn) = self.app_state.read().current_base_dn.clone() {
                    // Reload the entire tree to reflect the new children
                    self.reload_tree(&base_dn, window, cx);
                }
            }
            Err(err) => {
                state.set_node_loading(dn.to_string(), false);
                state.set_status(format!("Error loading children: {}", err));
            }
        }
    }

    /// Reload the tree from the base DN
    pub fn reload_tree(&self, base_dn: &str, window: &mut Window, cx: &mut Context<Self>) {
        let mut state = self.app_state.write();

        // Get the LDAP client
        let client = match state.active_client.as_mut() {
            Some(client) => client,
            None => {
                state.set_status("Not connected to LDAP server");
                return;
            }
        };

        // Load root level children
        match client.get_children(base_dn) {
            Ok(children) => {
                state.cache_node_children(base_dn.to_string(), children.clone());
                state.set_status(format!("Loaded {} root entries", children.len()));

                // Update tree state
                drop(state);
                let state_read = self.app_state.read();
                let root_children = state_read
                    .get_cached_children(base_dn)
                    .cloned()
                    .unwrap_or_default();
                drop(state_read);

                let items: Vec<TreeItem> = root_children
                    .iter()
                    .map(|node| self.tree_node_to_tree_item(node, cx))
                    .collect();

                self.tree_state.update(cx, |tree_state, cx| {
                    *tree_state = TreeState::new(cx).items(items);
                });
            }
            Err(err) => {
                state.set_status(format!("Error loading tree: {}", err));
            }
        }
    }

    /// Render the tree view
    pub fn render_tree(&self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let foreground = cx.theme().foreground;
        let muted_foreground = cx.theme().muted_foreground;
        let app_state = self.app_state.clone();
        let tree_view_self = self.clone();
        let entity_id = cx.entity_id();

        tree(&self.tree_state, move |ix, entry, selected, window, cx| {
            let item = entry.item();
            let is_folder = entry.is_folder();
            let is_expanded = entry.is_expanded();
            let dn = item.id.clone();

            // Determine icon based on folder state
            let icon = if !is_folder {
                IconName::File
            } else if is_expanded {
                IconName::FolderOpen
            } else {
                IconName::Folder
            };

            // Check if this node is loading
            let is_loading = app_state.read().is_node_loading(&dn);

            let tree_view = tree_view_self.clone();

            ListItem::new(SharedString::from(format!("tree-item-{}", ix)))
                .selected(selected)
                .pl(px(16.0) * entry.depth() + px(12.0))
                .on_click({
                    let dn = dn.clone();
                    let tree_view = tree_view.clone();
                    let app_state = app_state.clone();
                    move |_event, window, cx| {
                        // Toggle expansion if it's a folder
                        if is_folder {
                            let mut state = app_state.write();
                            state.toggle_node_expansion(&dn);
                            drop(state);

                            // If expanding and not loaded, load children
                            if !is_expanded {
                                cx.update_entity(&tree_view.entity, |tree, cx| tree.load_children(&dn, window, cx));
                            }
                        }

                        // Emit selection action
                        cx.update_entity(&tree_view.entity, |tree, cx| cx.emit(SelectTreeEntry { dn: dn.to_string() }));
                    }
                })
                .child(
                    h_flex()
                        .gap_2()
                        .items_center()
                        .child(Icon::new(icon).text_color(foreground))
                        .child(
                            div()
                                .text_size(px(13.0))
                                .text_color(foreground)
                                .child(item.label.clone()),
                        )
                        .when(is_loading, |this| {
                            this.child(
                                div()
                                    .text_size(px(11.0))
                                    .text_color(muted_foreground)
                                    .child("(loading...)"),
                            )
                        }),
                )
        })
        .w_full()
        .h_full()
    }
}

impl Render for TreeView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> AnyElement {
        self.render_tree(window, cx).into_any_element()
    }
}

impl Focusable for TreeView {
    fn focus_handle(&self, _cx: &gpui::App) -> FocusHandle {
        self.focus_handle.clone()
    }
}

impl EventEmitter<SelectTreeEntry> for TreeView {}

impl Clone for TreeView {
    fn clone(&self) -> Self {
        Self {
            app_state: self.app_state.clone(),
            tree_state: self.tree_state.clone(),
            focus_handle: self.focus_handle.clone(),
            entity: self.entity.clone(),
        }
    }
}
