//! LDAP client implementation
//!
//! This module provides the LDAP client for connecting to LDAP servers,
//! performing authentication, and executing search operations.

use crate::config::ConnectionSettings;
use crate::error::{LdapError, Result};
use crate::types::{Entry, SearchPage, TreeNode};
use ldap3::controls::ControlParser;
use ldap3::{LdapConn, LdapConnSettings, Scope, SearchEntry};
use std::collections::HashMap;

/// LDAP client for performing directory operations
///
/// The client manages a connection to an LDAP server and provides
/// methods for authentication and search operations.
pub struct LdapClient {
    /// The underlying LDAP connection
    conn: LdapConn,

    /// Connection settings used to establish this connection
    settings: ConnectionSettings,
}

impl LdapClient {
    /// Connect to an LDAP server using the provided settings
    ///
    /// This establishes a connection but does not authenticate.
    /// Call `bind()` after connecting to authenticate.
    ///
    /// # Example
    /// ```no_run
    /// use moribito_core::{LdapClient, ConnectionSettings, LdapConfig};
    ///
    /// let config = LdapConfig {
    ///     host: "ldap.example.com".to_string(),
    ///     port: 389,
    ///     base_dn: "dc=example,dc=com".to_string(),
    ///     use_ssl: false,
    ///     use_tls: false,
    ///     bind_user: "cn=admin,dc=example,dc=com".to_string(),
    ///     bind_pass: "password".to_string(),
    ///     saved_connections: vec![],
    ///     selected_connection: -1,
    /// };
    /// let settings = ConnectionSettings::from_ldap_config(&config);
    /// let client = LdapClient::connect(&settings)?;
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn connect(settings: &ConnectionSettings) -> Result<Self> {
        let url = settings.get_url();

        let ldap_settings = if settings.use_tls && !settings.use_ssl {
            // Use STARTTLS (upgrade plain connection to TLS)
            LdapConnSettings::new().set_starttls(true)
        } else {
            LdapConnSettings::new()
        };

        let conn = LdapConn::with_settings(ldap_settings, &url)
            .map_err(|e| LdapError::connection_failed(format!("Failed to connect: {}", e)))?;

        Ok(Self {
            conn,
            settings: settings.clone(),
        })
    }

    /// Authenticate (bind) to the LDAP server
    ///
    /// Uses the bind_user and bind_pass from the connection settings.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// let mut client = LdapClient::connect(&settings)?;
    /// client.bind()?;
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn bind(&mut self) -> Result<()> {
        self.conn
            .simple_bind(&self.settings.bind_user, &self.settings.bind_pass)
            .map_err(|e| LdapError::auth_failed(format!("Bind failed: {}", e)))?
            .success()
            .map_err(|e| LdapError::auth_failed(format!("Bind failed: {}", e)))?;

        Ok(())
    }

    /// Test a connection without keeping it open
    ///
    /// This is useful for validating connection settings in the GUI
    /// without creating a persistent client.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// LdapClient::test_connection(&settings)?;
    /// println!("Connection successful!");
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn test_connection(settings: &ConnectionSettings) -> Result<()> {
        let mut client = Self::connect(settings)?;
        client.bind()?;
        client.close()?;
        Ok(())
    }

    /// Get immediate children of a DN
    ///
    /// This performs a one-level search to find all entries directly
    /// under the given DN. Used for tree navigation.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// # let mut client = LdapClient::connect(&settings)?;
    /// # client.bind()?;
    /// let children = client.get_children("ou=users,dc=example,dc=com")?;
    /// for child in children {
    ///     println!("Child: {} ({})", child.name, child.dn);
    /// }
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn get_children(&mut self, dn: &str) -> Result<Vec<TreeNode>> {
        let (entries, _res) = self
            .conn
            .search(dn, Scope::OneLevel, "(objectClass=*)", vec!["*"])
            .map_err(|e| LdapError::search_failed(format!("Search failed: {}", e)))?
            .success()
            .map_err(|e| LdapError::search_failed(format!("Search failed: {}", e)))?;

        let nodes = entries
            .into_iter()
            .map(|entry| {
                let search_entry = SearchEntry::construct(entry);
                let entry = Self::search_entry_to_entry(search_entry);
                TreeNode::from_entry(&entry)
            })
            .collect();

        Ok(nodes)
    }

    /// Get a single entry with all its attributes
    ///
    /// Performs a base-level search for the specific DN.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// # let mut client = LdapClient::connect(&settings)?;
    /// # client.bind()?;
    /// let entry = client.get_entry("cn=admin,dc=example,dc=com")?;
    /// println!("DN: {}", entry.dn);
    /// for (attr, values) in &entry.attributes {
    ///     println!("  {}: {:?}", attr, values);
    /// }
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn get_entry(&mut self, dn: &str) -> Result<Entry> {
        let (entries, _res) = self
            .conn
            .search(dn, Scope::Base, "(objectClass=*)", vec!["*"])
            .map_err(|e| LdapError::search_failed(format!("Search failed: {}", e)))?
            .success()
            .map_err(|e| LdapError::search_failed(format!("Search failed: {}", e)))?;

        entries
            .into_iter()
            .next()
            .map(|entry| {
                let search_entry = SearchEntry::construct(entry);
                Self::search_entry_to_entry(search_entry)
            })
            .ok_or_else(|| LdapError::search_failed("Entry not found"))
    }

    /// Perform a paginated search
    ///
    /// This is useful for retrieving large result sets incrementally.
    /// The cookie from the returned SearchPage should be passed to
    /// subsequent calls to get the next page.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// # let mut client = LdapClient::connect(&settings)?;
    /// # client.bind()?;
    /// let mut cookie = None;
    /// let mut total = 0;
    ///
    /// loop {
    ///     let page = client.search_paged(
    ///         "dc=example,dc=com",
    ///         "(objectClass=person)",
    ///         ldap3::Scope::Subtree,
    ///         &["cn", "mail"],
    ///         50,
    ///         cookie,
    ///     )?;
    ///
    ///     total += page.entry_count();
    ///     println!("Retrieved {} entries (total: {})", page.entry_count(), total);
    ///
    ///     if !page.has_more {
    ///         break;
    ///     }
    ///     cookie = page.cookie;
    /// }
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn search_paged(
        &mut self,
        base_dn: &str,
        filter: &str,
        scope: Scope,
        attrs: &[&str],
        page_size: u32,
        cookie: Option<Vec<u8>>,
    ) -> Result<SearchPage> {
        use ldap3::controls::{PagedResults, RawControl};

        let page_control = PagedResults {
            size: page_size as i32,
            cookie: cookie.clone().unwrap_or_default(),
        };

        let page_raw_control = RawControl::from(page_control);
        let search = self.conn.with_controls(vec![page_raw_control]);

        let (entries, result) = search
            .search(base_dn, scope, filter, attrs)
            .map_err(|e| LdapError::search_failed(format!("Paged search failed: {}", e)))?
            .success()
            .map_err(|e| LdapError::search_failed(format!("Paged search failed: {}", e)))?;

        // Extract pagination info from response controls
        let controls = result.ctrls;
        let (has_more, new_cookie) = controls
            .iter()
            .find_map(|ctrl| {
                // The control is a tuple (Option<ControlType>, RawControl)
                // We need to check the RawControl's ctype field
                let raw_ctrl = &ctrl.1;
                if raw_ctrl.ctype == "1.2.840.113556.1.4.319" {
                    raw_ctrl.val.as_ref().map(|v| PagedResults::parse(v))
                } else {
                    None
                }
            })
            .map(|pr| {
                let has_more = !pr.cookie.is_empty();
                let cookie = if has_more { Some(pr.cookie) } else { None };
                (has_more, cookie)
            })
            .unwrap_or((false, None));

        let entry_count = entries.len();
        let entries: Vec<Entry> = entries
            .into_iter()
            .map(|entry| {
                let search_entry = SearchEntry::construct(entry);
                Self::search_entry_to_entry(search_entry)
            })
            .collect();

        let total_count = if cookie.is_none() {
            entry_count
        } else {
            entry_count // This is just the current page count
        };

        Ok(SearchPage::with_pagination(
            entries,
            has_more,
            new_cookie,
            page_size,
            total_count,
        ))
    }

    /// Close the LDAP connection
    ///
    /// This performs an unbind operation and closes the connection.
    ///
    /// # Example
    /// ```no_run
    /// # use moribito_core::{LdapClient, ConnectionSettings, Config};
    /// # let config = Config::default();
    /// # let settings = ConnectionSettings::from_ldap_config(&config.ldap);
    /// # let mut client = LdapClient::connect(&settings)?;
    /// client.close()?;
    /// # Ok::<(), moribito_core::LdapError>(())
    /// ```
    pub fn close(mut self) -> Result<()> {
        self.conn
            .unbind()
            .map_err(|e| LdapError::connection_failed(format!("Unbind failed: {}", e)))?;
        Ok(())
    }

    /// Convert ldap3::SearchEntry to our Entry type
    fn search_entry_to_entry(search_entry: SearchEntry) -> Entry {
        let mut attributes = HashMap::new();

        for (key, values) in search_entry.attrs {
            attributes.insert(key, values);
        }

        Entry::with_attributes(search_entry.dn, attributes)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_search_entry_conversion() {
        use ldap3::SearchEntry;

        let mut attrs = HashMap::new();
        attrs.insert("cn".to_string(), vec!["Test User".to_string()]);
        attrs.insert("mail".to_string(), vec!["test@example.com".to_string()]);

        let search_entry = SearchEntry {
            dn: "cn=Test User,dc=example,dc=com".to_string(),
            attrs,
            bin_attrs: HashMap::new(),
        };

        let entry = LdapClient::search_entry_to_entry(search_entry);
        assert_eq!(entry.dn, "cn=Test User,dc=example,dc=com");
        assert_eq!(entry.get_attribute("cn"), Some("Test User"));
        assert_eq!(entry.get_attribute("mail"), Some("test@example.com"));
    }

    #[test]
    fn test_connection_settings_url() {
        let mut settings = ConnectionSettings {
            host: "ldap.example.com".to_string(),
            port: 389,
            base_dn: "dc=example,dc=com".to_string(),
            use_ssl: false,
            use_tls: false,
            bind_user: "cn=admin,dc=example,dc=com".to_string(),
            bind_pass: "password".to_string(),
        };

        assert_eq!(settings.get_url(), "ldap://ldap.example.com:389");

        settings.use_ssl = true;
        assert_eq!(settings.get_url(), "ldaps://ldap.example.com:389");
    }

    // Note: Integration tests that require a real LDAP server should be
    // placed in tests/integration_tests.rs with #[ignore] attribute
}
