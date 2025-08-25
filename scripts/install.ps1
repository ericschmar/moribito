# Moribito Installation Script for Windows
# This script sets up the configuration directory using Windows conventions

param(
    [switch]$Local,
    [switch]$Help,
    [string]$InstallDir = "$env:ProgramFiles\moribito"
)

$ProgramName = "moribito"
$ConfigDir = "$env:APPDATA\moribito"
$ConfigFile = "$ConfigDir\config.yaml"
$BinaryName = "moribito.exe"

# Colors for output (if supported)
function Write-Info($Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Blue
}

function Write-Success($Message) {
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Warning($Message) {
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Write-Error($Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Check if running as administrator
function Test-Administrator {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# Get latest release version from GitHub
function Get-LatestVersion {
    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/repos/ericschmar/moribito/releases/latest"
        return $response.tag_name
    }
    catch {
        Write-Error "Failed to get latest version: $_"
        exit 1
    }
}

# Download and install binary
function Install-Binary($Version) {
    $binaryUrl = "https://github.com/ericschmar/moribito/releases/download/$Version/moribito-windows-amd64.exe"
    $tempFile = "$env:TEMP\$BinaryName"
    
    Write-Info "Downloading $ProgramName $Version for Windows..."
    
    try {
        Invoke-WebRequest -Uri $binaryUrl -OutFile $tempFile
    }
    catch {
        Write-Error "Failed to download binary: $_"
        exit 1
    }
    
    Write-Info "Installing binary to $InstallDir..."
    
    # Create install directory if it doesn't exist
    if (!(Test-Path $InstallDir)) {
        try {
            New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
        }
        catch {
            Write-Error "Failed to create install directory: $_"
            exit 1
        }
    }
    
    # Copy binary to install location
    $destinationPath = "$InstallDir\$BinaryName"
    try {
        Copy-Item $tempFile $destinationPath -Force
        Remove-Item $tempFile
    }
    catch {
        Write-Error "Failed to install binary: $_"
        exit 1
    }
    
    Write-Success "Binary installed to $destinationPath"
    
    # Add to PATH if not already there
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
    if ($currentPath -notlike "*$InstallDir*") {
        try {
            Write-Info "Adding $InstallDir to system PATH..."
            [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$InstallDir", "Machine")
            Write-Success "Added to system PATH. Restart your terminal to use 'moribito' command."
        }
        catch {
            Write-Warning "Failed to add to system PATH. You may need to add $InstallDir manually."
        }
    }
}

# Create configuration directory and sample config
function Setup-Config {
    Write-Info "Setting up configuration directory at $ConfigDir..."
    
    # Create config directory
    if (!(Test-Path $ConfigDir)) {
        New-Item -ItemType Directory -Path $ConfigDir -Force | Out-Null
    }
    
    # Create sample config file if it doesn't exist
    if (!(Test-Path $ConfigFile)) {
        $configContent = @"
# Moribito Configuration for Windows
# Located in %APPDATA%\moribito\ following Windows conventions

ldap:
  # LDAP server connection settings
  host: "ldap.example.com"
  port: 389  # Use 636 for LDAPS
  base_dn: "dc=example,dc=com"
  
  # Security settings
  use_ssl: false    # Use LDAPS (port 636)
  use_tls: false    # Use StartTLS (recommended for port 389)
  
  # Authentication (leave empty for anonymous bind)
  bind_user: "cn=admin,dc=example,dc=com"
  bind_pass: "your-password-here"

# Pagination settings for query results
pagination:
  # Number of entries to load per page (default: 50)
  page_size: 50

# Retry settings for LDAP operations
retry:
  enabled: true
  max_attempts: 3
  initial_delay_ms: 500
  max_delay_ms: 5000
"@
        
        Set-Content -Path $ConfigFile -Value $configContent
        Write-Success "Sample configuration created at $ConfigFile"
        Write-Info "Please edit $ConfigFile with your LDAP server details"
    }
    else {
        Write-Warning "Configuration file already exists at $ConfigFile"
    }
}

# Main installation function
function Install-Moribito {
    Write-Info "Installing $ProgramName for Windows..."
    
    # Check if we need administrator privileges for system-wide installation
    if ($InstallDir -like "$env:ProgramFiles*" -and !(Test-Administrator)) {
        Write-Error "Administrator privileges required for system-wide installation."
        Write-Info "Please run PowerShell as Administrator, or use a user directory:"
        Write-Info "  ./install.ps1 -InstallDir `"$env:LOCALAPPDATA\Programs\moribito`""
        exit 1
    }
    
    # Check if we're installing from a local binary or downloading
    if ($Local -and (Test-Path ".\bin\$BinaryName")) {
        Write-Info "Installing from local binary..."
        
        # Create install directory if it doesn't exist
        if (!(Test-Path $InstallDir)) {
            New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
        }
        
        Copy-Item ".\bin\$BinaryName" "$InstallDir\$BinaryName" -Force
        Write-Success "Local binary installed to $InstallDir\$BinaryName"
    }
    else {
        # Download and install from GitHub releases
        $version = Get-LatestVersion
        Install-Binary $version
    }
    
    # Setup configuration
    Setup-Config
    
    Write-Success "Installation completed successfully!"
    Write-Info ""
    Write-Info "Next steps:"
    Write-Info "1. Edit your configuration: $ConfigFile"
    Write-Info "2. Run the application: moribito"
    Write-Info "3. Or run with specific config: moribito -config `"$ConfigFile`""
    Write-Info ""
    Write-Info "Configuration will be automatically detected from:"
    Write-Info "  - $ConfigFile"
    Write-Info "  - $env:USERPROFILE\.moribito.yaml"
    Write-Info "  - .\config.yaml"
}

# Show help
function Show-Help {
    Write-Host @"
Moribito Installation Script for Windows

Usage:
  .\install.ps1 [options]

Options:
  -Local          Install from local binary (.\bin\moribito.exe)
  -InstallDir     Installation directory (default: $env:ProgramFiles\moribito)
  -Help           Show this help message

Examples:
  .\install.ps1                                              # Download and install latest release
  .\install.ps1 -Local                                      # Install from local build
  .\install.ps1 -InstallDir "$env:LOCALAPPDATA\Programs\moribito"  # Install to user directory
"@
}

# Main execution
if ($Help) {
    Show-Help
    exit 0
}

Install-Moribito