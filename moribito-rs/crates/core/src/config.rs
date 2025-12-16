//! Configuration management for LDAP connections
//!
//! This module handles loading, saving, and managing LDAP connection configurations
//! with support for multiple saved connections and OS-specific configuration paths.

use crate::error::{LdapError, Result};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

/// Root configuration structure
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Config {
    /// LDAP connection settings
    pub ldap: LdapConfig,

    /// Pagination settings
    #[serde(default)]
    pub pagination: PaginationConfig,

    /// Retry settings
    #[serde(default)]
    pub retry: RetryConfig,
}

/// LDAP connection configuration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct LdapConfig {
    /// LDAP server hostname or IP
    pub host: String,

    /// LDAP server port (typically 389 for LDAP, 636 for LDAPS)
    #[serde(default = "default_port")]
    pub port: u16,

    /// Base Distinguished Name for searches
    pub base_dn: String,

    /// Use SSL/LDAPS (ldaps://)
    #[serde(default)]
    pub use_ssl: bool,

    /// Use STARTTLS (upgrade connection to TLS)
    #[serde(default)]
    pub use_tls: bool,

    /// Bind user DN (for authentication)
    pub bind_user: String,

    /// Bind password (for authentication)
    pub bind_pass: String,

    /// List of saved connection profiles
    #[serde(default)]
    pub saved_connections: Vec<SavedConnection>,

    /// Index of the selected connection (-1 means use inline config)
    #[serde(default = "default_selected_connection")]
    pub selected_connection: i32,
}

/// A saved connection profile
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct SavedConnection {
    /// Display name for this connection
    pub name: String,

    /// LDAP server hostname or IP
    pub host: String,

    /// LDAP server port
    #[serde(default = "default_port")]
    pub port: u16,

    /// Base Distinguished Name
    pub base_dn: String,

    /// Use SSL/LDAPS
    #[serde(default)]
    pub use_ssl: bool,

    /// Use STARTTLS
    #[serde(default)]
    pub use_tls: bool,

    /// Bind user DN
    pub bind_user: String,

    /// Bind password
    pub bind_pass: String,
}

/// Pagination configuration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct PaginationConfig {
    /// Number of entries per page
    #[serde(default = "default_page_size")]
    pub page_size: u32,
}

/// Retry configuration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct RetryConfig {
    /// Enable retry logic
    #[serde(default = "default_retry_enabled")]
    pub enabled: bool,

    /// Maximum number of retry attempts
    #[serde(default = "default_max_attempts")]
    pub max_attempts: usize,

    /// Initial delay in milliseconds before first retry
    #[serde(default = "default_initial_delay_ms")]
    pub initial_delay_ms: u64,

    /// Maximum delay in milliseconds between retries
    #[serde(default = "default_max_delay_ms")]
    pub max_delay_ms: u64,
}

// Default value functions for serde
fn default_port() -> u16 {
    389
}

fn default_selected_connection() -> i32 {
    -1
}

fn default_page_size() -> u32 {
    50
}

fn default_retry_enabled() -> bool {
    true
}

fn default_max_attempts() -> usize {
    3
}

fn default_initial_delay_ms() -> u64 {
    500
}

fn default_max_delay_ms() -> u64 {
    5000
}

impl Default for PaginationConfig {
    fn default() -> Self {
        Self {
            page_size: default_page_size(),
        }
    }
}

impl Default for RetryConfig {
    fn default() -> Self {
        Self {
            enabled: default_retry_enabled(),
            max_attempts: default_max_attempts(),
            initial_delay_ms: default_initial_delay_ms(),
            max_delay_ms: default_max_delay_ms(),
        }
    }
}

impl Default for Config {
    fn default() -> Self {
        Self {
            ldap: LdapConfig {
                host: "localhost".to_string(),
                port: default_port(),
                base_dn: "dc=example,dc=com".to_string(),
                use_ssl: false,
                use_tls: false,
                bind_user: "cn=admin,dc=example,dc=com".to_string(),
                bind_pass: String::new(),
                saved_connections: Vec::new(),
                selected_connection: default_selected_connection(),
            },
            pagination: PaginationConfig::default(),
            retry: RetryConfig::default(),
        }
    }
}

impl Config {
    /// Load configuration from a specific file path
    pub fn load(path: impl AsRef<Path>) -> Result<Self> {
        let path = path.as_ref();
        let contents = fs::read_to_string(path)
            .map_err(|e| LdapError::config_error(format!("Failed to read config file: {}", e)))?;

        let config: Config = serde_yaml::from_str(&contents)?;
        Ok(config)
    }

    /// Load configuration from the default OS-specific path
    ///
    /// Searches for config.yaml in platform-specific locations:
    /// - macOS: ~/.moribito/config.yaml
    /// - Linux: $XDG_CONFIG_HOME/moribito/config.yaml or ~/.config/moribito/config.yaml
    /// - Windows: %APPDATA%\moribito\config.yaml
    pub fn load_from_default_path() -> Result<Self> {
        let path = Self::default_path();

        if path.exists() {
            Self::load(&path)
        } else {
            // Return default config if file doesn't exist
            Ok(Self::default())
        }
    }

    /// Save configuration to a specific file path
    pub fn save(&self, path: impl AsRef<Path>) -> Result<()> {
        let path = path.as_ref();

        // Create parent directories if they don't exist
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent).map_err(|e| {
                LdapError::config_error(format!("Failed to create config directory: {}", e))
            })?;
        }

        let yaml = serde_yaml::to_string(self)?;
        fs::write(path, yaml)
            .map_err(|e| LdapError::config_error(format!("Failed to write config file: {}", e)))?;

        Ok(())
    }

    /// Save configuration to the default OS-specific path
    pub fn save_to_default_path(&self) -> Result<()> {
        let path = Self::default_path();
        self.save(path)
    }

    /// Get the default configuration path for the current OS
    ///
    /// Returns platform-specific paths:
    /// - macOS: ~/.moribito/config.yaml
    /// - Linux: $XDG_CONFIG_HOME/moribito/config.yaml or ~/.config/moribito/config.yaml
    /// - Windows: %APPDATA%\moribito\config.yaml
    pub fn default_path() -> PathBuf {
        let config_dir = if cfg!(target_os = "macos") {
            // macOS: ~/.moribito/
            dirs::home_dir()
                .expect("Could not determine home directory")
                .join(".moribito")
        } else if cfg!(target_os = "windows") {
            // Windows: %APPDATA%\moribito\
            dirs::config_dir()
                .expect("Could not determine config directory")
                .join("moribito")
        } else {
            // Linux/Unix: $XDG_CONFIG_HOME/moribito/ or ~/.config/moribito/
            dirs::config_dir()
                .expect("Could not determine config directory")
                .join("moribito")
        };

        config_dir.join("config.yaml")
    }

    /// Get the active connection settings
    ///
    /// Returns the selected saved connection if one is selected (index >= 0),
    /// otherwise returns the inline configuration.
    pub fn get_active_connection(&self) -> ConnectionSettings {
        if self.ldap.selected_connection >= 0 {
            let index = self.ldap.selected_connection as usize;
            if let Some(saved) = self.ldap.saved_connections.get(index) {
                return ConnectionSettings::from_saved(saved);
            }
        }

        // Fall back to inline config
        ConnectionSettings::from_ldap_config(&self.ldap)
    }

    /// Set the active connection by index
    ///
    /// Use -1 to use the inline configuration
    pub fn set_active_connection(&mut self, index: i32) {
        self.ldap.selected_connection = index;
    }

    /// Add a new saved connection
    pub fn add_saved_connection(&mut self, connection: SavedConnection) {
        self.ldap.saved_connections.push(connection);
    }

    /// Remove a saved connection by index
    pub fn remove_saved_connection(&mut self, index: usize) -> Option<SavedConnection> {
        if index < self.ldap.saved_connections.len() {
            // Adjust selected_connection if needed
            if self.ldap.selected_connection as usize == index {
                self.ldap.selected_connection = -1;
            } else if self.ldap.selected_connection as usize > index {
                self.ldap.selected_connection -= 1;
            }

            Some(self.ldap.saved_connections.remove(index))
        } else {
            None
        }
    }

    /// Update a saved connection by index
    pub fn update_saved_connection(&mut self, index: usize, connection: SavedConnection) -> bool {
        if index < self.ldap.saved_connections.len() {
            self.ldap.saved_connections[index] = connection;
            true
        } else {
            false
        }
    }

    /// Get the number of saved connections
    pub fn saved_connection_count(&self) -> usize {
        self.ldap.saved_connections.len()
    }
}

/// Active connection settings extracted from Config
///
/// This is used to abstract over inline config vs saved connections
#[derive(Debug, Clone, PartialEq)]
pub struct ConnectionSettings {
    pub host: String,
    pub port: u16,
    pub base_dn: String,
    pub use_ssl: bool,
    pub use_tls: bool,
    pub bind_user: String,
    pub bind_pass: String,
}

impl ConnectionSettings {
    /// Create from LdapConfig (inline configuration)
    pub fn from_ldap_config(config: &LdapConfig) -> Self {
        Self {
            host: config.host.clone(),
            port: config.port,
            base_dn: config.base_dn.clone(),
            use_ssl: config.use_ssl,
            use_tls: config.use_tls,
            bind_user: config.bind_user.clone(),
            bind_pass: config.bind_pass.clone(),
        }
    }

    /// Create from SavedConnection
    pub fn from_saved(saved: &SavedConnection) -> Self {
        Self {
            host: saved.host.clone(),
            port: saved.port,
            base_dn: saved.base_dn.clone(),
            use_ssl: saved.use_ssl,
            use_tls: saved.use_tls,
            bind_user: saved.bind_user.clone(),
            bind_pass: saved.bind_pass.clone(),
        }
    }

    /// Get the LDAP URL (ldap:// or ldaps://)
    pub fn get_url(&self) -> String {
        let protocol = if self.use_ssl { "ldaps" } else { "ldap" };
        format!("{}://{}:{}", protocol, self.host, self.port)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::NamedTempFile;

    #[test]
    fn test_default_config() {
        let config = Config::default();
        assert_eq!(config.ldap.host, "localhost");
        assert_eq!(config.ldap.port, 389);
        assert_eq!(config.pagination.page_size, 50);
        assert!(config.retry.enabled);
        assert_eq!(config.retry.max_attempts, 3);
    }

    #[test]
    fn test_config_serialization() {
        let config = Config::default();
        let yaml = serde_yaml::to_string(&config).unwrap();
        let deserialized: Config = serde_yaml::from_str(&yaml).unwrap();
        assert_eq!(config, deserialized);
    }

    #[test]
    fn test_config_save_and_load() {
        let config = Config::default();
        let temp_file = NamedTempFile::new().unwrap();
        let path = temp_file.path();

        // Save
        config.save(path).unwrap();

        // Load
        let loaded = Config::load(path).unwrap();
        assert_eq!(config, loaded);
    }

    #[test]
    fn test_saved_connections() {
        let mut config = Config::default();

        let conn1 = SavedConnection {
            name: "Production".to_string(),
            host: "ldap.prod.example.com".to_string(),
            port: 636,
            base_dn: "dc=prod,dc=example,dc=com".to_string(),
            use_ssl: true,
            use_tls: false,
            bind_user: "cn=admin,dc=prod,dc=example,dc=com".to_string(),
            bind_pass: "secret".to_string(),
        };

        let conn2 = SavedConnection {
            name: "Development".to_string(),
            host: "ldap.dev.example.com".to_string(),
            port: 389,
            base_dn: "dc=dev,dc=example,dc=com".to_string(),
            use_ssl: false,
            use_tls: true,
            bind_user: "cn=admin,dc=dev,dc=example,dc=com".to_string(),
            bind_pass: "secret".to_string(),
        };

        // Add connections
        config.add_saved_connection(conn1.clone());
        config.add_saved_connection(conn2.clone());
        assert_eq!(config.saved_connection_count(), 2);

        // Select first connection
        config.set_active_connection(0);
        let active = config.get_active_connection();
        assert_eq!(active.host, "ldap.prod.example.com");
        assert_eq!(active.port, 636);

        // Remove first connection
        let removed = config.remove_saved_connection(0).unwrap();
        assert_eq!(removed.name, "Production");
        assert_eq!(config.saved_connection_count(), 1);

        // Selected connection should be reset to -1
        assert_eq!(config.ldap.selected_connection, -1);
    }

    #[test]
    fn test_connection_settings() {
        let saved = SavedConnection {
            name: "Test".to_string(),
            host: "ldap.test.com".to_string(),
            port: 636,
            base_dn: "dc=test,dc=com".to_string(),
            use_ssl: true,
            use_tls: false,
            bind_user: "cn=admin,dc=test,dc=com".to_string(),
            bind_pass: "pass".to_string(),
        };

        let settings = ConnectionSettings::from_saved(&saved);
        assert_eq!(settings.get_url(), "ldaps://ldap.test.com:636");

        let mut config = Config::default();
        config.ldap.use_ssl = false;
        let settings = ConnectionSettings::from_ldap_config(&config.ldap);
        assert_eq!(settings.get_url(), "ldap://localhost:389");
    }

    #[test]
    fn test_default_path() {
        let path = Config::default_path();
        assert!(path.ends_with("config.yaml"));
        assert!(path.to_string_lossy().contains("moribito"));
    }
}
