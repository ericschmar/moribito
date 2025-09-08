# Installation

## Homebrew (Recommended for macOS/Linux)

```bash
brew install moribito
```

## Windows

### From Source

```bash
git clone https://github.com/ericschmar/moribito
cd moribito
go build -o moribito.exe cmd/moribito/main.go
```

## Verification

After installation, verify the installation by checking the version:

```bash
moribito --version
```

## Configuration Setup

After installation, create your configuration file:

```bash
moribito --create-config
```

This will create a configuration file in the appropriate OS-specific location:
- **Linux**: `~/.config/moribito/config.yaml`
- **macOS**: `~/.moribito/config.yaml`
- **Windows**: `%APPDATA%\moribito\config.yaml`

Edit the configuration file with your LDAP server details and run:

```bash
moribito
```
