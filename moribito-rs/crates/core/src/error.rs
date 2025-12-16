//! Error types for LDAP operations
//!
//! This module defines all error types that can occur during LDAP operations,
//! configuration management, and connection handling.

use thiserror::Error;

/// Result type alias for LDAP operations
pub type Result<T> = std::result::Result<T, LdapError>;

/// Error types for LDAP operations
#[derive(Error, Debug)]
pub enum LdapError {
    /// Failed to establish connection to LDAP server
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),

    /// Authentication failed (invalid credentials or bind error)
    #[error("Authentication failed: {0}")]
    AuthFailed(String),

    /// Search operation failed
    #[error("Search failed: {0}")]
    SearchFailed(String),

    /// Configuration error (loading, parsing, or validation)
    #[error("Configuration error: {0}")]
    ConfigError(String),

    /// Retry limit exceeded after multiple attempts
    #[error("Retry limit exceeded after multiple attempts")]
    RetryLimitExceeded,

    /// Invalid DN (Distinguished Name) format
    #[error("Invalid DN format: {0}")]
    InvalidDN(String),

    /// Operation timed out
    #[error("Operation timed out")]
    Timeout,

    /// LDAP protocol error from ldap3 library
    #[error("LDAP protocol error: {0}")]
    LdapProtocol(#[from] ldap3::LdapError),

    /// I/O error (file operations, network)
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),

    /// YAML serialization/deserialization error
    #[error("YAML error: {0}")]
    Yaml(#[from] serde_yaml::Error),
}

impl LdapError {
    /// Check if this error is retryable (network/connection issues)
    ///
    /// Returns true for transient errors that might succeed on retry,
    /// false for permanent errors like authentication failures.
    pub fn is_retryable(&self) -> bool {
        match self {
            // Network and connection errors are retryable
            LdapError::ConnectionFailed(_) => true,
            LdapError::Timeout => true,
            LdapError::Io(_) => true,

            // Authentication and config errors are not retryable
            LdapError::AuthFailed(_) => false,
            LdapError::ConfigError(_) => false,
            LdapError::InvalidDN(_) => false,

            // Search failures might be retryable (depends on cause)
            LdapError::SearchFailed(_) => true,

            // Already exceeded retry limit
            LdapError::RetryLimitExceeded => false,

            // Check underlying ldap3 error
            LdapError::LdapProtocol(e) => is_ldap_error_retryable(e),

            // YAML errors are not retryable
            LdapError::Yaml(_) => false,
        }
    }

    /// Create a connection failed error
    pub fn connection_failed(msg: impl Into<String>) -> Self {
        LdapError::ConnectionFailed(msg.into())
    }

    /// Create an authentication failed error
    pub fn auth_failed(msg: impl Into<String>) -> Self {
        LdapError::AuthFailed(msg.into())
    }

    /// Create a search failed error
    pub fn search_failed(msg: impl Into<String>) -> Self {
        LdapError::SearchFailed(msg.into())
    }

    /// Create a configuration error
    pub fn config_error(msg: impl Into<String>) -> Self {
        LdapError::ConfigError(msg.into())
    }

    /// Create an invalid DN error
    pub fn invalid_dn(msg: impl Into<String>) -> Self {
        LdapError::InvalidDN(msg.into())
    }
}

/// Helper function to determine if an ldap3 error is retryable
fn is_ldap_error_retryable(error: &ldap3::LdapError) -> bool {
    // Convert error to string to check for common retryable patterns
    let error_str = error.to_string().to_lowercase();

    // Network/connection errors are retryable
    error_str.contains("connection")
        || error_str.contains("timeout")
        || error_str.contains("unavailable")
        || error_str.contains("busy")
        || error_str.contains("end of stream")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_error_creation() {
        let err = LdapError::connection_failed("host unreachable");
        assert!(matches!(err, LdapError::ConnectionFailed(_)));

        let err = LdapError::auth_failed("invalid password");
        assert!(matches!(err, LdapError::AuthFailed(_)));
    }

    #[test]
    fn test_is_retryable() {
        assert!(LdapError::ConnectionFailed("test".to_string()).is_retryable());
        assert!(LdapError::Timeout.is_retryable());
        assert!(!LdapError::AuthFailed("test".to_string()).is_retryable());
        assert!(!LdapError::ConfigError("test".to_string()).is_retryable());
        assert!(!LdapError::RetryLimitExceeded.is_retryable());
    }

    #[test]
    fn test_error_display() {
        let err = LdapError::connection_failed("connection refused");
        assert_eq!(err.to_string(), "Connection failed: connection refused");

        let err = LdapError::RetryLimitExceeded;
        assert_eq!(
            err.to_string(),
            "Retry limit exceeded after multiple attempts"
        );
    }
}
