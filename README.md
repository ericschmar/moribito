# LDAP CLI Explorer

A terminal-based LDAP server explorer built with Go and BubbleTea, providing an interactive interface for browsing LDAP directory trees, viewing records, and executing custom queries.

## Features

- 🌲 **Interactive Tree Navigation**: Browse LDAP directory structure with keyboard/mouse
- 📄 **Record Viewer**: View detailed LDAP entry attributes
- 🔍 **Custom Query Interface**: Execute custom LDAP queries with real-time results
- 📖 **Paginated Results**: Efficient pagination for large result sets with automatic loading
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
pagination:
  page_size: 50  # Number of entries per page
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
- **Page Up/Down** - Navigate by page (automatically loads more results)
- **Enter** - View selected record

> **Note**: The Query View uses automatic pagination to efficiently handle large result sets. When you scroll near the end of loaded results, the next page is automatically fetched from the LDAP server.

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