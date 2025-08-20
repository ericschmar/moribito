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
go build -o ldap-cli cmd/ldap-cli/main.go
```

### Testing
```bash
go test ./...
```

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