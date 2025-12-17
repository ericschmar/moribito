//! Application State Management
//!
//! This module manages the global application state including configuration,
//! active connections, and UI state.

use gpui::*;
use moribito_core::config::{Config, SavedConnection};
use moribito_core::types::{Entry, TreeNode};
use moribito_core::LdapClient;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;

/// Global application state
pub struct AppState {
    /// Application configuration
    pub config: Config,

    /// Currently selected connection (if any)
    pub selected_connection: Option<String>,

    /// List of saved connections
    pub saved_connections: Vec<SavedConnection>,

    /// Whether the app is currently connected to an LDAP server
    pub is_connected: bool,

    /// Current status message
    pub status_message: Option<String>,

    /// Active LDAP client (when connected)
    pub active_client: Option<LdapClient>,

    /// Current base DN for navigation
    pub current_base_dn: Option<String>,

    /// Tree state for the LDAP browser
    pub tree_state: TreeState,

    /// Currently selected entry in the tree
    pub selected_entry: Option<Entry>,

    /// Search results (when performing a search)
    pub search_results: Vec<Entry>,

    /// Current search filter
    pub search_filter: String,
}

/// State for the LDAP tree navigation
#[derive(Debug, Clone, Default)]
pub struct TreeState {
    /// Set of expanded node DNs
    pub expanded_nodes: HashSet<String>,

    /// Cache of node children (DN -> children)
    pub node_children: HashMap<String, Vec<TreeNode>>,

    /// Set of nodes currently being loaded
    pub loading_nodes: HashSet<String>,
}

impl AppState {
    /// Create a new application state
    pub fn new() -> Self {
        let config = Config::load_from_default_path().unwrap_or_default();
        let saved_connections = config.ldap.saved_connections.clone();

        Self {
            config,
            selected_connection: None,
            saved_connections,
            is_connected: false,
            status_message: None,
            active_client: None,
            current_base_dn: None,
            tree_state: TreeState::default(),
            selected_entry: None,
            search_results: Vec::new(),
            search_filter: String::new(),
        }
    }

    /// Load a saved connection by name
    pub fn load_connection(&mut self, name: &str) -> Option<&SavedConnection> {
        self.selected_connection = Some(name.to_string());
        self.saved_connections.iter().find(|c| c.name == name)
    }

    /// Save a new connection
    pub fn save_connection(&mut self, connection: SavedConnection) -> anyhow::Result<()> {
        // Remove existing connection with same name
        self.saved_connections.retain(|c| c.name != connection.name);

        // Add new connection
        self.saved_connections.push(connection.clone());

        // Update config
        self.config.ldap.saved_connections = self.saved_connections.clone();

        // Save to disk
        self.config.save(&Config::default_path())?;

        Ok(())
    }

    /// Delete a saved connection
    pub fn delete_connection(&mut self, name: &str) -> anyhow::Result<()> {
        self.saved_connections.retain(|c| c.name != name);

        // Clear selection if we deleted the selected connection
        if self.selected_connection.as_deref() == Some(name) {
            self.selected_connection = None;
        }

        // Update config
        self.config.ldap.saved_connections = self.saved_connections.clone();

        // Save to disk
        self.config.save(&Config::default_path())?;

        Ok(())
    }

    /// Set the status message
    pub fn set_status(&mut self, message: impl Into<String>) {
        self.status_message = Some(message.into());
    }

    /// Clear the status message
    pub fn clear_status(&mut self) {
        self.status_message = None;
    }

    /// Mark as connected
    pub fn set_connected(&mut self, connected: bool) {
        self.is_connected = connected;
    }

    /// Set the active LDAP client
    pub fn set_client(&mut self, client: LdapClient, base_dn: String) {
        self.active_client = Some(client);
        self.current_base_dn = Some(base_dn);
        self.is_connected = true;
    }

    /// Clear the active LDAP client and reset connection state
    pub fn disconnect(&mut self) {
        self.active_client = None;
        self.current_base_dn = None;
        self.is_connected = false;
        self.selected_entry = None;
        self.search_results.clear();
        self.tree_state = TreeState::default();
    }

    /// Set the currently selected entry
    pub fn set_selected_entry(&mut self, entry: Entry) {
        self.selected_entry = Some(entry);
    }

    /// Clear the selected entry
    pub fn clear_selected_entry(&mut self) {
        self.selected_entry = None;
    }

    /// Set search results
    pub fn set_search_results(&mut self, results: Vec<Entry>) {
        self.search_results = results;
    }

    /// Clear search results
    pub fn clear_search_results(&mut self) {
        self.search_results.clear();
    }

    /// Toggle expansion state of a tree node
    pub fn toggle_node_expansion(&mut self, dn: &str) {
        if self.tree_state.expanded_nodes.contains(dn) {
            self.tree_state.expanded_nodes.remove(dn);
        } else {
            self.tree_state.expanded_nodes.insert(dn.to_string());
        }
    }

    /// Check if a node is expanded
    pub fn is_node_expanded(&self, dn: &str) -> bool {
        self.tree_state.expanded_nodes.contains(dn)
    }

    /// Cache children for a node
    pub fn cache_node_children(&mut self, dn: String, children: Vec<TreeNode>) {
        self.tree_state.node_children.insert(dn, children);
    }

    /// Get cached children for a node
    pub fn get_cached_children(&self, dn: &str) -> Option<&Vec<TreeNode>> {
        self.tree_state.node_children.get(dn)
    }

    /// Mark a node as loading
    pub fn set_node_loading(&mut self, dn: String, loading: bool) {
        if loading {
            self.tree_state.loading_nodes.insert(dn);
        } else {
            self.tree_state.loading_nodes.remove(&dn);
        }
    }

    /// Check if a node is currently loading
    pub fn is_node_loading(&self, dn: &str) -> bool {
        self.tree_state.loading_nodes.contains(dn)
    }
}

impl Default for AppState {
    fn default() -> Self {
        Self::new()
    }
}

/// Wrapper for shared application state
#[derive(Clone)]
pub struct SharedAppState {
    inner: Arc<parking_lot::RwLock<AppState>>,
}

impl SharedAppState {
    /// Create a new shared application state
    pub fn new(state: AppState) -> Self {
        Self {
            inner: Arc::new(parking_lot::RwLock::new(state)),
        }
    }

    /// Get a read lock on the state
    pub fn read(&self) -> parking_lot::RwLockReadGuard<AppState> {
        self.inner.read()
    }

    /// Get a write lock on the state
    pub fn write(&self) -> parking_lot::RwLockWriteGuard<AppState> {
        self.inner.write()
    }

    /// Apply a connection result and update the status
    pub fn apply_connection(&self, name: String, client: LdapClient, base_dn: String) {
        let mut state = self.write();
        state.selected_connection = Some(name.clone());
        state.set_client(client, base_dn);
        state.set_status(format!("Connected to {}", name));
    }
}

impl Default for SharedAppState {
    fn default() -> Self {
        Self::new(AppState::default())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_app_state_creation() {
        let state = AppState::new();
        assert!(!state.is_connected);
        assert!(state.selected_connection.is_none());
    }

    #[test]
    fn test_set_status() {
        let mut state = AppState::new();
        state.set_status("Test message");
        assert_eq!(state.status_message, Some("Test message".to_string()));

        state.clear_status();
        assert!(state.status_message.is_none());
    }

    #[test]
    fn test_set_connected() {
        let mut state = AppState::new();
        assert!(!state.is_connected);

        state.set_connected(true);
        assert!(state.is_connected);

        state.set_connected(false);
        assert!(!state.is_connected);
    }

    #[test]
    fn test_shared_state() {
        let shared = SharedAppState::default();

        // Test write
        {
            let mut state = shared.write();
            state.set_status("Test");
        }

        // Test read
        {
            let state = shared.read();
            assert_eq!(state.status_message, Some("Test".to_string()));
        }
    }
}
