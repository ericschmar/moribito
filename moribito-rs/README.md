# Moribito Rust - LDAP Explorer GUI

A modern LDAP directory explorer with a GPU-accelerated graphical interface built using Rust and GPUI.

## Architecture

This project is organized as a Cargo workspace with two crates:

- **`core`**: Business logic library for LDAP operations, configuration management, and error handling
- **`gui`**: GPUI-based graphical user interface

## Features

### Current (v0.1.0)
- Configuration screen for LDAP connection settings
- Test LDAP connections before saving
- Manage multiple saved connections
- Persistent configuration (YAML)
- SSL/TLS support
- Cross-platform (macOS, Linux)

### Planned
- Interactive directory tree navigation
- Entry detail view with attribute display
- Custom LDAP search/query interface
- Clipboard integration
- Export functionality (CSV, JSON)

## Building

```bash
# Build the entire workspace
cargo build --release

# Run the GUI application
cargo run --release -p moribito-gui

# Run tests
cargo test --all
```

## Requirements

- Rust 1.75+ (latest stable recommended)
- macOS or Linux (Windows support planned)
- LDAP server for testing

## Configuration

Configuration is stored in platform-specific locations:

- **macOS**: `~/.moribito/config.yaml`
- **Linux**: `$XDG_CONFIG_HOME/moribito/config.yaml` or `~/.config/moribito/config.yaml`
- **Windows**: `%APPDATA%\moribito\config.yaml`

Example configuration:

```yaml
ldap:
  host: "ldap.example.com"
  port: 389
  base_dn: "dc=example,dc=com"
  use_ssl: false
  use_tls: false
  bind_user: "cn=admin,dc=example,dc=com"
  bind_pass: "password"
  saved_connections: []
  selected_connection: -1

pagination:
  page_size: 50

retry:
  enabled: true
  max_attempts: 3
  initial_delay_ms: 500
  max_delay_ms: 5000
```

## Technology Stack

- **LDAP**: [ldap3](https://github.com/inejge/ldap3) - Pure Rust LDAP client
- **UI Framework**: [GPUI](https://www.gpui.rs/) - GPU-accelerated UI from Zed
- **Serialization**: serde + serde_yaml
- **Error Handling**: thiserror (core), anyhow (gui)
- **Async Runtime**: tokio

## License

See LICENSE.md in the root of the repository.

## Contributing

Contributions are welcome! Please see the main project repository for guidelines.
