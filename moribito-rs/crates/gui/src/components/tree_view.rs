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
use gpui::{Entity, EventEmitter};

/// Tree view component for LDAP directory navigation
pub struct TreeView {
    app_state: SharedAppState,
    tree_state: Entity<TreeState>,
    focus_handle: FocusHandle,
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
        }
    }

    /// Update the tree with root nodes
    pub fn set_root_nodes(&mut self, nodes: Vec<TreeNode>, cx: &mut App) {
        // Organize entries by type (only [Service Accounts] virtual group)
        let organized = self.organize_entries(nodes);

        // Convert to TreeItems
        let items: Vec<TreeItem> = organized
            .iter()
            .map(|node| self.tree_node_to_tree_item(node, cx))
            .collect();

        self.tree_state.update(cx, |state, cx| {
            *state = TreeState::new(cx).items(items);
        });
    }

    /// Group entries by type for better organization
    fn organize_entries(&self, nodes: Vec<TreeNode>) -> Vec<TreeNode> {
        // Only group cn= entries into [Service Accounts]
        let (service_accounts, others): (Vec<_>, Vec<_>) = nodes
            .into_iter()
            .partition(|n| n.name.to_lowercase().starts_with("cn="));

        let mut organized = Vec::new();

        // Add service accounts group if any exist
        if !service_accounts.is_empty() {
            organized.push(Self::create_virtual_group(
                "[Service Accounts]",
                service_accounts,
            ));
        }

        // Add all other entries directly (OUs, uid= users, etc.)
        organized.extend(others);

        organized
    }

    /// Create a virtual grouping node (not a real LDAP entry)
    fn create_virtual_group(label: &str, children: Vec<TreeNode>) -> TreeNode {
        TreeNode {
            dn: format!("__virtual_{}", label.to_lowercase().replace(' ', "_")),
            name: label.to_string(),
            children: Some(children),
            is_loaded: true,
        }
    }

    /// Convert a TreeNode to a TreeItem
    fn tree_node_to_tree_item(&self, node: &TreeNode, _cx: &mut App) -> TreeItem {
        let state = self.app_state.read();
        let is_expanded = state.is_node_expanded(&node.dn);

        let mut item = TreeItem::new(node.dn.clone(), node.name.clone());

        // For virtual nodes, children are embedded in the node itself
        if node.dn.starts_with("__virtual_") {
            if let Some(ref children) = node.children {
                drop(state); // Release lock before recursion
                let child_items: Vec<TreeItem> = children
                    .iter()
                    .map(|child| self.tree_node_to_tree_item(child, _cx))
                    .collect();
                item = item.children(child_items);

                // Re-acquire state for expansion check
                let state = self.app_state.read();
                if is_expanded {
                    item = item.expanded(true);
                }
            }
        }
        // For regular nodes, check the cache
        else if let Some(children) = state.get_cached_children(&node.dn) {
            let child_items: Vec<TreeItem> = children
                .iter()
                .map(|child| self.tree_node_to_tree_item(child, _cx))
                .collect();

            item = item.children(child_items);

            if is_expanded {
                item = item.expanded(true);
            }
        } else {
            // No children yet, just check expansion
            if is_expanded {
                item = item.expanded(true);
            }
        }

        item
    }

    /// Load children for a node
    pub fn load_children(&mut self, dn: &str, window: &mut Window, cx: &mut App) {
        log::debug!("🔍 [TreeView] load_children called for DN: {}", dn);
        let mut state = self.app_state.write();

        // Check if already loading or already cached
        if state.is_node_loading(dn) {
            log::debug!("⏳ [TreeView] Node already loading: {}", dn);
            return;
        }
        if state.get_cached_children(dn).is_some() {
            log::debug!("✅ [TreeView] Node children already cached: {}", dn);
            return;
        }

        // Mark as loading
        state.set_node_loading(dn.to_string(), true);
        log::info!("⏳ [TreeView] Loading children for: {}", dn);

        // Get the LDAP client
        let client = match state.active_client.as_mut() {
            Some(client) => client,
            None => {
                log::error!("❌ [TreeView] No active LDAP client");
                state.set_node_loading(dn.to_string(), false);
                state.set_status("Not connected to LDAP server");
                return;
            }
        };

        // Load children from LDAP
        match client.get_children(dn) {
            Ok(children) => {
                log::info!(
                    "✅ [TreeView] Loaded {} children for {}",
                    children.len(),
                    dn
                );
                state.cache_node_children(dn.to_string(), children.clone());
                state.set_node_loading(dn.to_string(), false);
                state.set_status(format!("Loaded {} children for {}", children.len(), dn));

                // Update the tree
                drop(state);

                log::debug!("🔄 [TreeView] Reloading tree to show new children");
                // Reload the entire tree to reflect the new children
                self.reload_tree(window, cx);
            }
            Err(err) => {
                log::error!("❌ [TreeView] Error loading children for {}: {}", dn, err);
                state.set_node_loading(dn.to_string(), false);
                state.set_status(format!("Error loading children: {}", err));
            }
        }
    }

    /// Reload the tree from the current context DN
    pub fn reload_tree(&mut self, _window: &mut Window, cx: &mut App) {
        // Get the current context DN
        let context_dn = {
            let state = self.app_state.read();
            state.tree_state.current_context_dn.clone()
        };

        log::info!(
            "🌲 [TreeView] reload_tree called for context DN: {}",
            context_dn
        );
        let mut state = self.app_state.write();

        // Get the LDAP client
        let client = match state.active_client.as_mut() {
            Some(client) => {
                log::debug!("✅ [TreeView] Active LDAP client found");
                client
            }
            None => {
                log::error!("❌ [TreeView] No active LDAP client available");
                state.set_status("Not connected to LDAP server");
                return;
            }
        };

        log::info!("🔍 [TreeView] Searching for children under: {}", context_dn);
        // Load children for the current context
        match client.get_children(&context_dn) {
            Ok(children) => {
                log::info!(
                    "✅ [TreeView] Successfully loaded {} entries from LDAP",
                    children.len()
                );

                // Log the first few entries for debugging
                for (i, child) in children.iter().take(5).enumerate() {
                    log::debug!("  Entry {}: {} ({})", i + 1, child.name, child.dn);
                }
                if children.len() > 5 {
                    log::debug!("  ... and {} more entries", children.len() - 5);
                }

                state.cache_node_children(context_dn.clone(), children.clone());
                state.set_status(format!("Loaded {} entries", children.len()));

                // Release the lock before calling set_root_nodes
                drop(state);

                // Use set_root_nodes to apply organization
                log::info!("🔄 [TreeView] Calling set_root_nodes with organization");
                self.set_root_nodes(children, cx);
                log::info!("✅ [TreeView] Tree state updated successfully");
            }
            Err(err) => {
                log::error!("❌ [TreeView] Error loading tree from LDAP: {}", err);
                state.set_status(format!("Error loading tree: {}", err));
            }
        }
    }

    /// Render the tree view
    pub fn render_tree(&self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let foreground = cx.theme().foreground;
        let muted_foreground = cx.theme().muted_foreground;
        let app_state = self.app_state.clone();
        let tree_entity = cx.entity();

        tree(&self.tree_state, move |ix, entry, selected, window, cx| {
            let item = entry.item();
            let is_folder = entry.is_folder();
            let is_expanded = entry.is_expanded();
            let dn = item.id.clone();

            // Determine if this is a virtual node
            let is_virtual = dn.starts_with("__virtual_");

            // Determine icon based on folder state and node type
            let icon = if is_virtual {
                IconName::Folder // Virtual grouping nodes use folder icon
            } else if !is_folder {
                IconName::File
            } else if is_expanded {
                IconName::FolderOpen
            } else {
                IconName::Folder
            };

            // Check if this node is loading
            let is_loading = app_state.read().is_node_loading(&dn);

            let tree_entity_handle = tree_entity.clone();

            ListItem::new(SharedString::from(format!("tree-item-{}", ix)))
                .selected(selected)
                .pl(px(16.0) * entry.depth() + px(12.0))
                .on_click({
                    let dn = dn.clone();
                    let name = item.label.clone();
                    let tree_entity = tree_entity_handle.clone();
                    let app_state = app_state.clone();
                    move |_event, window, cx| {
                        // Check if this is an OU node (and not a virtual node)
                        let is_ou = !is_virtual && name.to_lowercase().starts_with("ou=");

                        if is_virtual {
                            // Virtual nodes: just toggle expansion
                            let mut state = app_state.write();
                            state.toggle_node_expansion(&dn);
                        } else if is_ou {
                            // OU nodes: Navigate INTO them
                            window.dispatch_action(&NavigateIntoOu { dn: dn.to_string() });
                        } else {
                            // Regular entries: emit selection event
                            cx.update_entity(&tree_entity, |tree, cx| {
                                cx.emit(SelectTreeEntry { dn: dn.to_string() })
                            });
                        }
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
        }
    }
}
