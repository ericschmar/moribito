# LDAP CLI Explorer

A terminal-based LDAP server explorer built with Go and BubbleTea, providing an interactive interface for browsing LDAP directory trees, viewing records, and executing custom queries.

## Features

- 🌲 **Interactive Tree Navigation**: Browse LDAP directory structure with keyboard/mouse
- 📄 **Record Viewer**: View detailed LDAP entry attributes
- 📋 **Clipboard Integration**: Copy attribute values to system clipboard
- 🔍 **Custom Query Interface**: Execute custom LDAP queries with real-time results
- 📖 **Paginated Results**: Efficient pagination for large result sets with automatic loading
- ⚙️ **Flexible Configuration**: Support for config files and command-line options
- 🔐 **Secure Authentication**: Support for SSL/TLS and various authentication methods
- 🎨 **Modern TUI**: Clean, intuitive interface built with BubbleTea

## Installation

### From GitHub Releases (Recommended)

Download the latest pre-built binary from [GitHub Releases](https://github.com/ericschmar/ldap-cli/releases):

```bash
# Linux x86_64
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-linux-amd64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# Linux ARM64
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-linux-arm64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# macOS Intel
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-darwin-amd64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# macOS Apple Silicon
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-darwin-arm64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/
```

For Windows, download `ldap-cli-windows-amd64.exe` from the releases page.

### From Source

```bash
git clone https://github.com/ericschmar/ldap-cli
cd ldap-cli
go build -o ldap-cli cmd/ldap-cli/main.go
```

## Usage

### Command Line Options

```bash
# Connect with command line options
ldap-cli -host ldap.example.com -base-dn "dc=example,dc=com" -user "cn=admin,dc=example,dc=com"

# Use a configuration file
ldap-cli -config /path/to/config.yaml

# Get help
ldap-cli -help
```

### Configuration File

Create a configuration file in one of these locations:
- `./config.yaml` (current directory)
- `~/.ldap-cli.yaml` (home directory) 
- `~/.config/ldap-cli/config.yaml` (XDG config directory)

```yaml
ldap:
  host: "ldap.example.com"
  port: 389
  base_dn: "dc=example,dc=com"
  use_ssl: false
  use_tls: false
  bind_user: "cn=admin,dc=example,dc=com"
  bind_pass: "your-password"
pagination:
  page_size: 50  # Number of entries per page
retry:
  enabled: true  # Connection retries (default: true)
  max_attempts: 3  # Retry attempts (default: 3)
  initial_delay_ms: 500  # Initial delay (default: 500)
  max_delay_ms: 5000  # Max delay cap (default: 5000)
```

## Navigation

### General Controls
- **Tab** - Switch between views (Tree → Record → Query → Tree)
- **1/2/3** - Jump directly to Tree/Record/Query view
- **q** - Quit application

### Tree View
- **↑/↓** or **k/j** - Navigate up/down
- **Page Up/Down** - Navigate by page
- **Home/End** - Jump to top/bottom
- **→** or **l** - Expand node (load children)
- **←** or **h** - Collapse node
- **Enter** - View record details

### Record View
- **↑/↓** or **k/j** - Scroll up/down
- **Page Up/Down** - Scroll by page
- **Home/End** - Jump to top/bottom
- **c** - Copy current attribute value to clipboard

### Query View
- **/** or **Escape** - Focus query input
- **Ctrl+Enter** or **Ctrl+J** - Execute query
- **Ctrl+F** - Format query with proper indentation
- **Escape** - Clear query
- **Ctrl+V** - Paste from clipboard
- **↑/↓** - Navigate results (when not in input mode)
- **Page Up/Down** - Navigate by page (automatically loads more results)
- **Enter** - View selected record

> **Note**: The Query View uses automatic pagination to efficiently handle large result sets. When you scroll near the end of loaded results, the next page is automatically fetched from the LDAP server.

#### Query Formatting
The **Ctrl+F** key combination formats complex LDAP queries with proper indentation for better readability:

```
# Before formatting:
(&(objectClass=person)(|(cn=john*)(sn=smith*))(department=engineering))

# After formatting (Ctrl+F):
(&
  (objectClass=person)
  (|
    (cn=john*)
    (sn=smith*)
  )
  (department=engineering)
)
```

## Authentication Methods

The tool supports various LDAP authentication methods:

### Simple Bind
```yaml
bind_user: "cn=admin,dc=example,dc=com"
bind_pass: "password"
```

### OU-based Authentication
```yaml
bind_user: "uid=john,ou=users,dc=example,dc=com"
bind_pass: "password"
```

### Active Directory Style
```yaml
bind_user: "john@example.com"
bind_pass: "password"
```

### Anonymous Bind
```yaml
# Leave bind_user and bind_pass empty or omit them
```

## Security Options

### SSL/LDAPS (Port 636)
```yaml
ldap:
  host: "ldaps.example.com"
  port: 636
  use_ssl: true
```

### StartTLS (Port 389)
```yaml
ldap:
  host: "ldap.example.com"
  port: 389
  use_tls: true
```

## Query Examples

In the Query view, you can execute custom LDAP filters:

- `(objectClass=*)` - All objects
- `(objectClass=person)` - All person objects
- `(cn=john*)` - Objects with cn starting with "john"
- `(&(objectClass=person)(mail=*@example.com))` - People with example.com emails
- `(|(cn=admin)(uid=admin))` - Objects with cn=admin OR uid=admin

### Complex Query Formatting

For complex nested queries, use **Ctrl+F** to automatically format them for better readability:

**Simple queries remain unchanged:**
```
(objectClass=person)
```

**Complex queries are formatted with proper indentation:**
```
# Original
(&(objectClass=person)(|(cn=john*)(sn=smith*))(department=engineering))

# After Ctrl+F
(&
  (objectClass=person)
  (|
    (cn=john*)
    (sn=smith*)
  )
  (department=engineering)
)
```

## Performance & Pagination

LDAP CLI uses intelligent pagination to provide optimal performance when working with large directories:

### Automatic Pagination
- **Default Page Size**: 50 entries per page
- **Configurable**: Adjust via config file or `--page-size` flag
- **On-Demand Loading**: Next pages load automatically as you scroll
- **Memory Efficient**: Only loaded entries are kept in memory

### Configuration Examples
```bash
# Command line override
ldap-cli --page-size 100 --host ldap.example.com

# Configuration file
pagination:
  page_size: 25  # Smaller pages for slower networks
```

### Performance Tips
- **Smaller page sizes** (10-25) for slower networks or limited LDAP servers
- **Larger page sizes** (100-200) for fast networks and powerful LDAP servers
- **Use specific queries** to reduce result sets instead of browsing all entries

## Connection Reliability & Retries

LDAP CLI includes automatic retry functionality to handle connection failures gracefully:

### Automatic Retries
- **Default**: Enabled with 3 retry attempts
- **Exponential Backoff**: Delay doubles between attempts (500ms → 1s → 2s → ...)
- **Connection Recovery**: Automatically re-establishes broken connections
- **Smart Detection**: Only retries connection-related errors, not authentication failures

### Configuration Examples
```bash
# Default retry settings (automatically applied)
# No configuration needed - retries work out of the box
```

```yaml
# Custom retry configuration
retry:
  enabled: true
  max_attempts: 5           # Maximum retry attempts (default: 3)
  initial_delay_ms: 1000    # Initial delay in milliseconds (default: 500)
  max_delay_ms: 10000       # Maximum delay cap (default: 5000)
```

```yaml
# Disable retries if needed
retry:
  enabled: false
```

### Retryable Conditions
The system automatically retries for:
- **Network timeouts** and connection drops
- **Connection refused** errors
- **Server unavailable** responses  
- **Connection reset** by peer
- **LDAP server down** errors

Authentication errors, invalid queries, and permission issues are **not** retried.

## Development

### Building
```bash
# Build for current platform
make build

# Build for all platforms
make build-all

# Clean build artifacts
make clean
```

### Code Quality
```bash
# Format code
make fmt

# Run linter
make lint

# Run tests
make test

# Run all CI checks (format, lint, test, build)
make ci
```

### Testing
```bash
go test ./...
```

### Continuous Integration
This project uses GitHub Actions for CI/CD:

- **CI Workflow**: Runs on every push and pull request to `main` and `develop` branches
  - Code formatting verification
  - Linting (with warnings)
  - Testing
  - Building for current platform
  - Multi-platform build artifacts (on main branch pushes)

- **Release Workflow**: Triggered by version tags (e.g., `v1.0.0`)
  - Runs full CI checks
  - Builds for all platforms (Linux amd64/arm64, macOS amd64/arm64, Windows amd64)
  - Creates GitHub releases with binaries and checksums
  - Generates installation instructions

### Dependencies
- [BubbleTea](https://github.com/charmbracelet/bubbletea) - TUI framework
- [Lipgloss](https://github.com/charmbracelet/lipgloss) - Styling
- [go-ldap](https://github.com/go-ldap/ldap) - LDAP client
- [golang.org/x/term](https://golang.org/x/term) - Terminal utilities

### Versioning

This project follows [Semantic Versioning](https://semver.org/). See [docs/versioning.md](docs/versioning.md) for details on the release process.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.