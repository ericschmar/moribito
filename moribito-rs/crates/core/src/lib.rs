//! Moribito Core - LDAP operations library
//!
//! This crate provides the core business logic for LDAP operations,
//! configuration management, and error handling.

pub mod client;
pub mod config;
pub mod error;
pub mod retry;
pub mod types;

// Re-export commonly used types
pub use client::LdapClient;
pub use config::{
    Config, ConnectionSettings, LdapConfig, PaginationConfig, RetryConfig, SavedConnection,
};
pub use error::{LdapError, Result};
pub use types::{Entry, SearchPage, TreeNode};
// TODO: Re-export this once implemented in subsequent step
// pub use retry::RetryPolicy;
