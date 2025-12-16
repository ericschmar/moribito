//! Core data types for LDAP operations
//!
//! This module defines the fundamental data structures used throughout
//! the LDAP client, including entries, tree nodes, and search results.

use std::collections::HashMap;

/// Represents an LDAP entry with its Distinguished Name and attributes
///
/// An Entry is the fundamental unit in LDAP, representing an object in the
/// directory with a unique DN and a set of attribute-value pairs.
#[derive(Debug, Clone, PartialEq)]
pub struct Entry {
    /// Distinguished Name (DN) - unique identifier for this entry
    pub dn: String,

    /// Attributes as key-value pairs where each attribute can have multiple values
    pub attributes: HashMap<String, Vec<String>>,
}

impl Entry {
    /// Create a new Entry with the given DN
    pub fn new(dn: impl Into<String>) -> Self {
        Self {
            dn: dn.into(),
            attributes: HashMap::new(),
        }
    }

    /// Create an Entry with DN and attributes
    pub fn with_attributes(
        dn: impl Into<String>,
        attributes: HashMap<String, Vec<String>>,
    ) -> Self {
        Self {
            dn: dn.into(),
            attributes,
        }
    }

    /// Get a single value for an attribute (returns the first value if multiple exist)
    pub fn get_attribute(&self, name: &str) -> Option<&str> {
        self.attributes
            .get(name)
            .and_then(|values| values.first())
            .map(|s| s.as_str())
    }

    /// Get all values for an attribute
    pub fn get_attribute_values(&self, name: &str) -> Option<&Vec<String>> {
        self.attributes.get(name)
    }

    /// Add an attribute value (creates the attribute if it doesn't exist)
    pub fn add_attribute(&mut self, name: String, value: String) {
        self.attributes
            .entry(name)
            .or_insert_with(Vec::new)
            .push(value);
    }

    /// Set an attribute to a single value (replaces existing values)
    pub fn set_attribute(&mut self, name: String, value: String) {
        self.attributes.insert(name, vec![value]);
    }

    /// Set an attribute to multiple values (replaces existing values)
    pub fn set_attribute_values(&mut self, name: String, values: Vec<String>) {
        self.attributes.insert(name, values);
    }

    /// Check if an attribute exists
    pub fn has_attribute(&self, name: &str) -> bool {
        self.attributes.contains_key(name)
    }

    /// Get the Relative Distinguished Name (RDN) from the DN
    ///
    /// For example, "cn=John Doe" from "cn=John Doe,ou=users,dc=example,dc=com"
    pub fn get_rdn(&self) -> Option<&str> {
        self.dn.split(',').next()
    }
}

/// Represents a node in the LDAP directory tree
///
/// TreeNode provides a hierarchical view of the LDAP directory with
/// support for lazy loading of children.
#[derive(Debug, Clone, PartialEq)]
pub struct TreeNode {
    /// Distinguished Name of this node
    pub dn: String,

    /// Display name (typically the RDN)
    pub name: String,

    /// Child nodes (None means not loaded yet, Some(vec![]) means loaded but no children)
    pub children: Option<Vec<TreeNode>>,

    /// Whether children have been loaded from the server
    pub is_loaded: bool,
}

impl TreeNode {
    /// Create a new TreeNode with the given DN
    ///
    /// Children are not loaded initially (lazy loading)
    pub fn new(dn: impl Into<String>, name: impl Into<String>) -> Self {
        Self {
            dn: dn.into(),
            name: name.into(),
            children: None,
            is_loaded: false,
        }
    }

    /// Create a TreeNode from an Entry, extracting the name from the RDN
    pub fn from_entry(entry: &Entry) -> Self {
        let name = entry.get_rdn().unwrap_or(&entry.dn).to_string();

        Self::new(entry.dn.clone(), name)
    }

    /// Mark this node as loaded with the given children
    pub fn set_children(&mut self, children: Vec<TreeNode>) {
        self.children = Some(children);
        self.is_loaded = true;
    }

    /// Check if this node has children (loaded or not)
    pub fn has_children(&self) -> bool {
        matches!(self.children, Some(ref children) if !children.is_empty())
    }

    /// Get a mutable reference to children, loading them if needed
    pub fn get_children_mut(&mut self) -> Option<&mut Vec<TreeNode>> {
        self.children.as_mut()
    }

    /// Find a child node by DN
    pub fn find_child(&self, dn: &str) -> Option<&TreeNode> {
        self.children.as_ref()?.iter().find(|child| child.dn == dn)
    }

    /// Find a child node by DN (mutable)
    pub fn find_child_mut(&mut self, dn: &str) -> Option<&mut TreeNode> {
        self.children
            .as_mut()?
            .iter_mut()
            .find(|child| child.dn == dn)
    }
}

/// Represents a page of search results with pagination state
///
/// SearchPage is used for paginated LDAP searches, allowing large result
/// sets to be retrieved incrementally.
#[derive(Debug, Clone)]
pub struct SearchPage {
    /// Entries in this page
    pub entries: Vec<Entry>,

    /// Whether more results are available
    pub has_more: bool,

    /// Pagination cookie for fetching the next page (opaque to the client)
    pub cookie: Option<Vec<u8>>,

    /// Page size used for this search
    pub page_size: u32,

    /// Total number of entries retrieved so far (across all pages)
    pub total_count: usize,
}

impl SearchPage {
    /// Create a new SearchPage
    pub fn new(entries: Vec<Entry>, page_size: u32) -> Self {
        let total_count = entries.len();
        Self {
            entries,
            has_more: false,
            cookie: None,
            page_size,
            total_count,
        }
    }

    /// Create a SearchPage with pagination state
    pub fn with_pagination(
        entries: Vec<Entry>,
        has_more: bool,
        cookie: Option<Vec<u8>>,
        page_size: u32,
        total_count: usize,
    ) -> Self {
        Self {
            entries,
            has_more,
            cookie,
            page_size,
            total_count,
        }
    }

    /// Check if this is the first page
    pub fn is_first_page(&self) -> bool {
        self.total_count == self.entries.len()
    }

    /// Check if this is the last page
    pub fn is_last_page(&self) -> bool {
        !self.has_more
    }

    /// Get the number of entries in this page
    pub fn entry_count(&self) -> usize {
        self.entries.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_entry_creation() {
        let entry = Entry::new("cn=test,dc=example,dc=com");
        assert_eq!(entry.dn, "cn=test,dc=example,dc=com");
        assert!(entry.attributes.is_empty());
    }

    #[test]
    fn test_entry_attributes() {
        let mut entry = Entry::new("cn=John Doe,ou=users,dc=example,dc=com");

        // Add attributes
        entry.add_attribute("cn".to_string(), "John Doe".to_string());
        entry.add_attribute("mail".to_string(), "john@example.com".to_string());
        entry.add_attribute("mail".to_string(), "jdoe@example.com".to_string());

        // Test retrieval
        assert_eq!(entry.get_attribute("cn"), Some("John Doe"));
        assert_eq!(entry.get_attribute("mail"), Some("john@example.com"));
        assert_eq!(entry.get_attribute_values("mail").unwrap().len(), 2);
        assert!(entry.has_attribute("cn"));
        assert!(!entry.has_attribute("nonexistent"));
    }

    #[test]
    fn test_entry_rdn() {
        let entry = Entry::new("cn=John Doe,ou=users,dc=example,dc=com");
        assert_eq!(entry.get_rdn(), Some("cn=John Doe"));

        let root = Entry::new("dc=com");
        assert_eq!(root.get_rdn(), Some("dc=com"));
    }

    #[test]
    fn test_tree_node_creation() {
        let node = TreeNode::new("ou=users,dc=example,dc=com", "users");
        assert_eq!(node.dn, "ou=users,dc=example,dc=com");
        assert_eq!(node.name, "users");
        assert!(!node.is_loaded);
        assert!(node.children.is_none());
    }

    #[test]
    fn test_tree_node_from_entry() {
        let mut entry = Entry::new("cn=John Doe,ou=users,dc=example,dc=com");
        entry.set_attribute("cn".to_string(), "John Doe".to_string());

        let node = TreeNode::from_entry(&entry);
        assert_eq!(node.dn, "cn=John Doe,ou=users,dc=example,dc=com");
        assert_eq!(node.name, "cn=John Doe");
    }

    #[test]
    fn test_tree_node_children() {
        let mut parent = TreeNode::new("ou=users,dc=example,dc=com", "users");
        assert!(!parent.has_children());
        assert!(!parent.is_loaded);

        let child1 = TreeNode::new("cn=user1,ou=users,dc=example,dc=com", "user1");
        let child2 = TreeNode::new("cn=user2,ou=users,dc=example,dc=com", "user2");

        parent.set_children(vec![child1, child2]);
        assert!(parent.has_children());
        assert!(parent.is_loaded);
        assert_eq!(parent.children.as_ref().unwrap().len(), 2);
    }

    #[test]
    fn test_tree_node_find_child() {
        let mut parent = TreeNode::new("ou=users,dc=example,dc=com", "users");
        let child1 = TreeNode::new("cn=user1,ou=users,dc=example,dc=com", "user1");
        let child2 = TreeNode::new("cn=user2,ou=users,dc=example,dc=com", "user2");

        parent.set_children(vec![child1, child2]);

        let found = parent.find_child("cn=user1,ou=users,dc=example,dc=com");
        assert!(found.is_some());
        assert_eq!(found.unwrap().name, "user1");

        let not_found = parent.find_child("cn=user3,ou=users,dc=example,dc=com");
        assert!(not_found.is_none());
    }

    #[test]
    fn test_search_page_creation() {
        let entries = vec![
            Entry::new("cn=user1,dc=example,dc=com"),
            Entry::new("cn=user2,dc=example,dc=com"),
        ];

        let page = SearchPage::new(entries.clone(), 10);
        assert_eq!(page.entry_count(), 2);
        assert_eq!(page.page_size, 10);
        assert!(!page.has_more);
        assert!(page.cookie.is_none());
        assert_eq!(page.total_count, 2);
    }

    #[test]
    fn test_search_page_pagination() {
        let entries = vec![Entry::new("cn=user1,dc=example,dc=com")];
        let cookie = vec![1, 2, 3, 4];

        let page = SearchPage::with_pagination(entries, true, Some(cookie.clone()), 10, 1);

        assert!(page.has_more);
        assert_eq!(page.cookie, Some(cookie));
        assert!(page.is_first_page());
        assert!(!page.is_last_page());
    }
}
