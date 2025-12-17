---
description: Repository Information Overview
alwaysApply: true
---

# Moribito Repository Information

## Summary

**Moribito** (森人 - forest-person) is a multi-language LDAP server explorer
project providing interactive terminal-based and GUI interfaces for browsing
LDAP directories, viewing records, and executing custom queries. The repository
contains a primary Go implementation with a concurrent Rust/GPUI-based
implementation.

## Repository Structure

- **`cmd/moribito/`**: Main Go application entry point
- **`internal/`**: Go packages including LDAP client, TUI components,
  configuration, and utilities
- **`moribito-rs/`**: Rust implementation workspace with GPUI-based GUI
- **`config/`**: Configuration templates and examples
- **`docs/`**: Documentation files (usage, installation, development,
  contributing)
- **`scripts/`**: Platform-specific installation scripts (Linux, macOS, Windows)
- **`lib/`**: Shared Go libraries for core functionality (if applicable)
- **`testing/`**: Test helpers and fixtures

## Projects

### Go Project (Primary CLI)

**Entry Point**: `cmd/moribito/main.go`

#### Language & Runtime

**Language**: Go\
**Version**: 1.24.6\
**Build System**: Make\
**Package Manager**: Go Modules

#### Main Dependencies

- **TUI Framework**: charmbracelet (bubbles, bubbletea, lipgloss)
- **LDAP**: go-ldap/ldap v3.4.11
- **UI Enhancements**: bubblezone, muesli gamut (color), lucasb-eyer go-colorful
- **Configuration**: gopkg.in/yaml.v3
- **Utilities**: atotto/clipboard, google/uuid

#### Build & Installation

```bash
# Build
make build

# Format code
make fmt

# Run tests
make test

# Lint code
make lint

# Full CI check
make ci

# Run with example config
make run

# Install to GOPATH/bin
make install

# Build for multiple platforms
make build-all
```

#### Testing

**Framework**: Go testing (standard library)\
**Test Location**: `internal/**/*_test.go`\
**Naming Convention**: `*_test.go`\
**Test Files**: 24 test files covering version, updater, LDAP client, TUI
components, configuration, and error handling

**Run Command**:

```bash
go test -v ./...
```

#### Configuration

**Config File Locations**:

- Linux/Unix: `~/.config/moribito/config.yaml`, `~/.moribito/config.yaml`,
  `~/.moribito.yaml`
- macOS: `~/.moribito/config.yaml`,
  `~/Library/Application Support/moribito/config.yaml`, `~/.moribito.yaml`
- Windows: `%APPDATA%\moribito\config.yaml`, `%USERPROFILE%\.moribito.yaml`
- All platforms: `./config.yaml` (current directory)

**Configuration Options**:

- LDAP connection settings (host, port, base DN, SSL/TLS)
- Authentication (bind user/pass, anonymous bind, AD-style)
- Pagination (page size for result sets)
- Retry settings (enabled, max attempts, delays)
- Multiple saved connection profiles

---

### Rust Project (moribito-rs)

**Workspace Location**: `moribito-rs/`

#### Language & Runtime

**Language**: Rust\
**Edition**: 2021\
**Build System**: Cargo\
**Package Manager**: Cargo

#### Workspace Structure

- **`crates/core`** (moribito-core v0.1.0): Core LDAP operations library
  - Key Dependencies: ldap3 0.11, serde, serde_yaml, tokio, thiserror, dirs
  - Dev Dependencies: tempfile

- **`crates/gui`** (moribito-gui v0.1.0): GPUI-based graphical interface
  - Binary Name: moribito
  - Key Dependencies: gpui 0.2.2, gpui-component 0.5.0, ldap3, tokio, anyhow,
    serde, schemarc, parking_lot, env_logger
  - Shared Dependencies: serde, tokio (via workspace)

#### Build & Installation

```bash
cd moribito-rs

# Build core and GUI
cargo build

# Build GUI only
cargo build -p moribito-gui

# Run GUI
cargo run -p moribito-gui

# Run tests
cargo test
```

---

### Documentation (Node.js Setup)

**Configuration**: `package.json`\
**Package Manager**: npm\
**Dev Dependencies**: docpress 0.8.2

#### Build & Serving

```bash
# Build documentation website
make docs

# Serve documentation locally
make docs-serve

# Clean documentation artifacts
make docs-clean
```

---

## Key Features

- **Interactive Tree Navigation**: Keyboard/mouse browsing of LDAP directories
- **Record Viewer**: Detailed attribute inspection with clipboard integration
- **Custom Query Interface**: Execute and format LDAP filters with pagination
- **Multiple Connections**: Save and switch between LDAP server configurations
- **Security**: SSL/TLS and NTLM/StartTLS support
- **Auto-Update Notifications**: Check for newer releases from GitHub
- **Pagination**: Automatic on-demand loading for large result sets
- **Retry Mechanism**: Connection reliability with exponential backoff

## Development Guidelines

- Run `make fmt` before committing (enforces gofmt)
- Use table-driven unit tests for Go
- Follow Go best practices and idiomatic patterns
- Update `ruby/lib/billing-platform/version.rb` if updating Ruby components (if
  applicable)
- Execute `make proto` after updating protocol buffer definitions (if
  applicable)
- Review `.github/copilot-instructions.md` for contribution standards
