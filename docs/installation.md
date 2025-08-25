# Installation

## Homebrew (Recommended for macOS/Linux)

### From Custom Tap
```bash
brew install ericschmar/tap/moribito
```

### From Formula URL (if tap not available)
```bash
brew install https://raw.githubusercontent.com/ericschmar/moribito/main/homebrew/moribito.rb
```

## From GitHub Releases

### Option 1: Quick Install Scripts (Recommended)

The easiest way to install Moribito is using our platform-specific install scripts:

**Linux/Unix:**
```bash
curl -sSL https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install.sh | bash
```

**macOS:**
```bash
curl -sSL https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install-macos.sh | bash
```

**Windows (PowerShell as Administrator):**
```powershell
irm https://raw.githubusercontent.com/ericschmar/moribito/main/scripts/install.ps1 | iex
```

The install scripts will:
- Download the appropriate binary for your platform and architecture
- Install it to the system PATH
- Create OS-specific configuration directories
- Generate sample configuration files in the appropriate locations

### Option 2: Manual Download

Download the latest pre-built binary from [GitHub Releases](https://github.com/ericschmar/moribito/releases):

```bash
# Linux x86_64
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-linux-amd64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# Linux ARM64
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-linux-arm64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# macOS Intel
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-darwin-amd64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# macOS Apple Silicon
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-darwin-arm64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/
```

For Windows, download `moribito-windows-amd64.exe` from the releases page.

## From Source

```bash
git clone https://github.com/ericschmar/moribito
cd moribito
go build -o moribito cmd/moribito/main.go
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