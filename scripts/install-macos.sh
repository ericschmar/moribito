#!/bin/bash

# Moribito Installation Script for macOS
# This script sets up the configuration directory using macOS conventions

set -e

PROGRAM_NAME="moribito"
CONFIG_DIR="$HOME/.moribito"
CONFIG_FILE="$CONFIG_DIR/config.yaml"
BINARY_NAME="moribito"
INSTALL_DIR="${INSTALL_DIR:-/usr/local/bin}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Detect architecture
detect_arch() {
    local arch
    arch=$(uname -m)
    case $arch in
        x86_64|amd64) echo "amd64" ;;
        arm64) echo "arm64" ;;
        *) 
            print_error "Unsupported architecture: $arch"
            exit 1
            ;;
    esac
}

# Get latest release version from GitHub
get_latest_version() {
    local version
    version=$(curl -s https://api.github.com/repos/ericschmar/moribito/releases/latest | grep '"tag_name":' | sed -E 's/.*"tag_name": "([^"]+)".*/\1/')
    if [ -z "$version" ]; then
        print_error "Failed to get latest version"
        exit 1
    fi
    echo "$version"
}

# Download and install binary
install_binary() {
    local version="$1"
    local arch="$2"
    local binary_url="https://github.com/ericschmar/moribito/releases/download/$version/${BINARY_NAME}-darwin-$arch"
    local temp_file="/tmp/${BINARY_NAME}-$arch"

    print_info "Downloading $PROGRAM_NAME $version for darwin-$arch..."
    
    if ! curl -L "$binary_url" -o "$temp_file"; then
        print_error "Failed to download binary"
        exit 1
    fi

    chmod +x "$temp_file"
    
    print_info "Installing binary to $INSTALL_DIR..."
    
    # Check if we need sudo for installation
    if [ ! -w "$INSTALL_DIR" ]; then
        if command -v sudo >/dev/null 2>&1; then
            sudo mv "$temp_file" "$INSTALL_DIR/$BINARY_NAME"
        else
            print_error "Need write permission to $INSTALL_DIR and sudo not available"
            exit 1
        fi
    else
        mv "$temp_file" "$INSTALL_DIR/$BINARY_NAME"
    fi

    print_success "Binary installed to $INSTALL_DIR/$BINARY_NAME"
}

# Create configuration directory and sample config
setup_config() {
    print_info "Setting up configuration directory at $CONFIG_DIR..."
    
    # Create config directory
    mkdir -p "$CONFIG_DIR"
    
    # Create sample config file if it doesn't exist
    if [ ! -f "$CONFIG_FILE" ]; then
        cat > "$CONFIG_FILE" << 'EOF'
# Moribito Configuration for macOS
# Located in ~/.moribito/ following macOS conventions

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

  # Multiple saved connections (optional)
  # Uncomment and configure to save multiple connection profiles:
  # selected_connection: 0
  # saved_connections:
  #   - name: "Production"
  #     host: "ldap.prod.example.com"
  #     port: 636
  #     base_dn: "dc=prod,dc=example,dc=com"
  #     use_ssl: true
  #     bind_user: "cn=admin,dc=prod,dc=example,dc=com"
  #     bind_pass: "prod-password"

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
EOF
        print_success "Sample configuration created at $CONFIG_FILE"
        print_info "Please edit $CONFIG_FILE with your LDAP server details"
    else
        print_warning "Configuration file already exists at $CONFIG_FILE"
    fi
}

# Check and install Homebrew if requested
install_via_homebrew() {
    if command -v brew >/dev/null 2>&1; then
        print_info "Homebrew detected. You can also install via:"
        print_info "  brew tap ericschmar/moribito"
        print_info "  brew install moribito"
    else
        print_info "Consider installing Homebrew for easier package management:"
        print_info "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
    fi
}

# Main installation function
main() {
    print_info "Installing $PROGRAM_NAME for macOS..."
    
    # Check if we're on macOS
    if [ "$(uname)" != "Darwin" ]; then
        print_error "This script is designed for macOS. Use install.sh for Linux/Unix."
        exit 1
    fi
    
    # Check if we're installing from a local binary or downloading
    if [ "$1" = "--local" ] && [ -f "./bin/${BINARY_NAME}" ]; then
        print_info "Installing from local binary..."
        if [ ! -w "$INSTALL_DIR" ]; then
            sudo cp "./bin/${BINARY_NAME}" "$INSTALL_DIR/${BINARY_NAME}"
        else
            cp "./bin/${BINARY_NAME}" "$INSTALL_DIR/${BINARY_NAME}"
        fi
        chmod +x "$INSTALL_DIR/${BINARY_NAME}"
        print_success "Local binary installed to $INSTALL_DIR/$BINARY_NAME"
    else
        # Download and install from GitHub releases
        local arch
        local version
        
        arch=$(detect_arch)
        version=$(get_latest_version)
        
        install_binary "$version" "$arch"
    fi
    
    # Setup configuration
    setup_config
    
    # Show Homebrew info
    install_via_homebrew
    
    print_success "Installation completed successfully!"
    print_info ""
    print_info "Next steps:"
    print_info "1. Edit your configuration: $CONFIG_FILE"
    print_info "2. Run the application: $BINARY_NAME"
    print_info "3. Or run with specific config: $BINARY_NAME -config $CONFIG_FILE"
    print_info ""
    print_info "Configuration will be automatically detected from:"
    print_info "  - $CONFIG_FILE"
    print_info "  - ~/.moribito.yaml"
    print_info "  - ~/.config/moribito/config.yaml"
    print_info "  - ./config.yaml"
}

# Show help
show_help() {
    cat << EOF
Moribito Installation Script for macOS

Usage:
  $0 [options]

Options:
  --local         Install from local binary (./bin/moribito)
  --help          Show this help message

Environment Variables:
  INSTALL_DIR     Installation directory (default: /usr/local/bin)

Examples:
  $0                    # Download and install latest release
  $0 --local           # Install from local build
  INSTALL_DIR=~/bin $0 # Install to custom directory
EOF
}

# Parse command line arguments
case "${1:-}" in
    --help|-h)
        show_help
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac