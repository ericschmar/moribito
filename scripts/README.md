# Installation Scripts

This directory contains platform-specific installation scripts for Moribito.

## Scripts

### `install.sh` - Linux/Unix Installation
Installs Moribito on Linux and other Unix-like systems following XDG Base Directory Specification.

**Usage:**
```bash
curl -sSL https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install.sh | bash

# Or for local installation:
./scripts/install.sh --local
```

**Features:**
- Downloads appropriate binary for system architecture (amd64/arm64)
- Installs to `/usr/local/bin` (configurable via `INSTALL_DIR`)
- Creates XDG-compliant config directory: `~/.config/moribito/`
- Generates sample configuration file

### `install-macos.sh` - macOS Installation
Installs Moribito on macOS with macOS-specific conventions.

**Usage:**
```bash
curl -sSL https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install-macos.sh | bash

# Or for local installation:
./scripts/install-macos.sh --local
```

**Features:**
- Downloads appropriate binary for system architecture (Intel/Apple Silicon)
- Installs to `/usr/local/bin` (configurable via `INSTALL_DIR`)
- Creates macOS-style config directory: `~/.moribito/`
- Provides Homebrew installation suggestions
- Generates sample configuration file

### `install.ps1` - Windows Installation
Installs Moribito on Windows using PowerShell.

**Usage:**
```powershell
# Run as Administrator
irm https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install.ps1 | iex

# Or for local installation:
.\scripts\install.ps1 -Local
```

**Features:**
- Downloads Windows binary (amd64)
- Installs to `%ProgramFiles%\moribito` (configurable via `-InstallDir`)
- Adds installation directory to system PATH
- Creates Windows-style config directory: `%APPDATA%\moribito\`
- Generates sample configuration file

## Configuration Locations

After installation, configuration files are created at:

| Platform | Location |
|----------|----------|
| Linux    | `~/.config/moribito/config.yaml` |
| macOS    | `~/.moribito/config.yaml` |
| Windows  | `%APPDATA%\moribito\config.yaml` |

## Manual Configuration

You can also create configuration files manually using:
```bash
moribito --create-config
```

This will create the appropriate configuration file for your operating system.