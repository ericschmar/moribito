# LDAP CLI Explorer

A terminal-based LDAP server explorer built with Go and BubbleTea, providing an interactive interface for browsing LDAP directory trees, viewing records, and executing custom queries.

## Features

- 🌲 **Interactive Tree Navigation**: Browse LDAP directory structure with keyboard/mouse
- 📄 **Record Viewer**: View detailed LDAP entry attributes
- 🔍 **Custom Query Interface**: Execute custom LDAP queries with real-time results
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

### Query View
- **/** or **Escape** - Focus query input
- **Enter** - Execute query
- **Escape** - Clear query
- **↑/↓** - Navigate results (when not in input mode)
- **Enter** - View selected record

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

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.