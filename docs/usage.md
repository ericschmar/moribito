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

```yaml
# ~/.moribito.yaml or ./config.yaml
host: ldap.example.com
port: 389
base_dn: dc=example,dc=com
use_ssl: false
use_tls: true
bind_user: cn=admin,dc=example,dc=com
bind_password: password
page_size: 100
```

Then simply run:
```bash
moribito -config ~/.moribito.yaml
```

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