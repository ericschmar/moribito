# Usage Guide

## Command Line Options

```bash
# Connect with command line options
moribito --host ldap.example.com --port 389 --base-dn "dc=example,dc=com"

# Use SSL/TLS
moribito --host ldap.example.com --ssl --base-dn "dc=example,dc=com"

# With authentication
moribito --host ldap.example.com --bind-user "cn=admin,dc=example,dc=com" --bind-password "password" --base-dn "dc=example,dc=com"

# Enable auto-update checking
moribito --check-updates

# Combine options
moribito --host ldap.example.com --ssl --check-updates --base-dn "dc=example,dc=com"
```

## Configuration File

Create a YAML configuration file to avoid typing connection details repeatedly:

### Basic Configuration (Single Connection)

```yaml
# ~/.moribito.yaml or ./config.yaml
ldap:
  host: ldap.example.com
  port: 389
  base_dn: dc=example,dc=com
  use_ssl: false
  use_tls: true
  bind_user: cn=admin,dc=example,dc=com
  bind_pass: password
pagination:
  page_size: 100
```

### Advanced Configuration (Multiple Saved Connections)

For environments with multiple LDAP servers, you can save multiple connection profiles:

```yaml
ldap:
  # Default connection settings (used when no saved connections exist)
  host: ldap.example.com
  port: 389
  base_dn: dc=example,dc=com
  use_ssl: false
  use_tls: true
  bind_user: cn=admin,dc=example,dc=com
  bind_pass: password
  
  # Multiple saved connections
  selected_connection: 0  # Index of currently active connection (-1 for default)
  saved_connections:
    - name: "Production"
      host: ldap.prod.example.com
      port: 636
      base_dn: dc=prod,dc=example,dc=com
      use_ssl: true
      use_tls: false
      bind_user: cn=admin,dc=prod,dc=example,dc=com
      bind_pass: prod-password
    
    - name: "Development"  
      host: ldap.dev.example.com
      port: 389
      base_dn: dc=dev,dc=example,dc=com
      use_ssl: false
      use_tls: true
      bind_user: cn=admin,dc=dev,dc=example,dc=com
      bind_pass: ""  # Will prompt for password

pagination:
  page_size: 50
retry:
  enabled: true
  max_attempts: 3
```

Then simply run:
```bash
moribito -config ~/.moribito.yaml
```

### Managing Multiple Connections

When using multiple saved connections:

1. **In the Start View**: Navigate to the "Saved Connections" section to:
   - Switch between saved connections using ←/→ arrow keys
   - Add new connections with "Add New Connection"
   - Delete connections with "Delete Connection"
   - Save current settings as a new connection

2. **Connection Selection**: The `selected_connection` field determines which saved connection is active:
   - `-1` or omitted: Use default connection settings
   - `0`, `1`, `2`, etc.: Use the corresponding saved connection by index

3. **Backward Compatibility**: Old configuration files without saved connections continue to work exactly as before.

## Features Overview

- 🌲 **Interactive Tree Navigation**: Browse LDAP directory structure with keyboard/mouse
- 📄 **Record Viewer**: View detailed LDAP entry attributes
- 📋 **Clipboard Integration**: Copy attribute values to system clipboard
- 🔍 **Custom Query Interface**: Execute custom LDAP queries with real-time results
- 📖 **Paginated Results**: Efficient pagination for large result sets with automatic loading
- ⚙️ **Flexible Configuration**: Support for config files and command-line options
- 🔐 **Secure Authentication**: Support for SSL/TLS and various authentication methods
- 🔄 **Auto-Update Notifications**: Optional checking for newer releases from GitHub
- 🎨 **Modern TUI**: Clean, intuitive interface built with BubbleTea
- 🔀 **Multiple Connections**: Save and switch between multiple LDAP server configurations