#!/bin/bash

# Homebrew Formula Generator for moribito
# This script helps generate and maintain the Homebrew formula

set -e

VERSION=""
FORCE=false

usage() {
    echo "Usage: $0 -v VERSION [-f]"
    echo "  -v VERSION  Version to generate formula for (e.g., 0.0.2)"
    echo "  -f          Force overwrite existing formula"
    echo ""
    echo "Examples:"
    echo "  $0 -v 0.0.2"
    echo "  $0 -v 1.0.0 -f"
    exit 1
}

while getopts "v:fh" opt; do
    case $opt in
        v)
            VERSION="$OPTARG"
            ;;
        f)
            FORCE=true
            ;;
        h)
            usage
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            usage
            ;;
    esac
done

if [ -z "$VERSION" ]; then
    echo "Error: Version is required"
    usage
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOMEBREW_DIR="$REPO_ROOT/homebrew"
FORMULA_FILE="$HOMEBREW_DIR/moribito.rb"

echo "Generating Homebrew formula for version $VERSION..."

# Check if version exists as a GitHub release
echo "Checking if release v$VERSION exists..."
RELEASE_URL="https://api.github.com/repos/ericschmar/moribito/releases/tags/v$VERSION"
RELEASE_EXISTS=false
if curl -s -f "$RELEASE_URL" > /dev/null; then
    RELEASE_EXISTS=true
    echo "Release v$VERSION found."
else
    echo "Warning: Release v$VERSION not found on GitHub. Make sure to create the release first."
fi

# Function to get SHA256 for a file URL
get_sha256() {
    local url="$1"
    echo "Fetching SHA256 for $url..." >&2
    if curl -sL "$url" >/dev/null 2>&1; then
        local sha256=$(curl -sL "$url" | sha256sum | cut -d' ' -f1)
        echo "$sha256"
    else
        echo "Error: Could not fetch $url" >&2
        return 1
    fi
}

# Get SHA256 checksums for all platforms
echo "Fetching SHA256 checksums..."
DARWIN_AMD64_SHA256=""
DARWIN_ARM64_SHA256=""
LINUX_AMD64_SHA256=""  
LINUX_ARM64_SHA256=""

# Only fetch SHA256 if release exists
if [ "$RELEASE_EXISTS" = true ]; then
    # Try new naming convention first (moribito-*), then fall back to old (ldap-cli-*)
    echo "Attempting to fetch SHA256s for current binary names..."
    
    DARWIN_AMD64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/moribito-darwin-amd64" 2>/dev/null)
    if [ -z "$DARWIN_AMD64_SHA256" ]; then
        DARWIN_AMD64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/ldap-cli-darwin-amd64" 2>/dev/null)
    fi
    if [ -z "$DARWIN_AMD64_SHA256" ]; then
        echo "Warning: Could not fetch darwin-amd64 binary"
    fi
    
    DARWIN_ARM64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/moribito-darwin-arm64" 2>/dev/null)
    if [ -z "$DARWIN_ARM64_SHA256" ]; then
        DARWIN_ARM64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/ldap-cli-darwin-arm64" 2>/dev/null)
    fi
    if [ -z "$DARWIN_ARM64_SHA256" ]; then
        echo "Warning: Could not fetch darwin-arm64 binary"
    fi
    
    LINUX_AMD64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/moribito-linux-amd64" 2>/dev/null)
    if [ -z "$LINUX_AMD64_SHA256" ]; then
        LINUX_AMD64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/ldap-cli-linux-amd64" 2>/dev/null)
    fi
    if [ -z "$LINUX_AMD64_SHA256" ]; then
        echo "Warning: Could not fetch linux-amd64 binary"
    fi
    
    LINUX_ARM64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/moribito-linux-arm64" 2>/dev/null)
    if [ -z "$LINUX_ARM64_SHA256" ]; then
        LINUX_ARM64_SHA256=$(get_sha256 "https://github.com/ericschmar/moribito/releases/download/v$VERSION/ldap-cli-linux-arm64" 2>/dev/null)
    fi
    if [ -z "$LINUX_ARM64_SHA256" ]; then
        echo "Warning: Could not fetch linux-arm64 binary"
    fi
else
    echo "Skipping SHA256 fetch - release not found"
fi

# Generate the formula using template
echo "Generating formula file: $FORMULA_FILE"

if [ -f "$FORMULA_FILE" ] && [ "$FORCE" != true ]; then
    echo "Formula file already exists. Use -f to force overwrite."
    exit 1
fi

mkdir -p "$HOMEBREW_DIR"

# Use template if available, otherwise create from scratch
TEMPLATE_FILE="$HOMEBREW_DIR/moribito-template.rb"
if [ -f "$TEMPLATE_FILE" ]; then
    echo "Using template file: $TEMPLATE_FILE"
    cp "$TEMPLATE_FILE" "$FORMULA_FILE"
    
    # Replace placeholders in the template
    # Use a different delimiter to avoid issues with forward slashes and special characters
    # Use portable sed commands that work on both Linux and macOS
    if [ "$(uname)" = "Darwin" ]; then
        # macOS version
        sed -i '' "s|REPLACE_VERSION|$VERSION|g" "$FORMULA_FILE"
        sed -i '' "s|REPLACE_DARWIN_AMD64_SHA256|${DARWIN_AMD64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i '' "s|REPLACE_DARWIN_ARM64_SHA256|${DARWIN_ARM64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i '' "s|REPLACE_LINUX_AMD64_SHA256|${LINUX_AMD64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i '' "s|REPLACE_LINUX_ARM64_SHA256|${LINUX_ARM64_SHA256:-}|g" "$FORMULA_FILE"
    else
        # Linux version
        sed -i "s|REPLACE_VERSION|$VERSION|g" "$FORMULA_FILE"
        sed -i "s|REPLACE_DARWIN_AMD64_SHA256|${DARWIN_AMD64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i "s|REPLACE_DARWIN_ARM64_SHA256|${DARWIN_ARM64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i "s|REPLACE_LINUX_AMD64_SHA256|${LINUX_AMD64_SHA256:-}|g" "$FORMULA_FILE"
        sed -i "s|REPLACE_LINUX_ARM64_SHA256|${LINUX_ARM64_SHA256:-}|g" "$FORMULA_FILE"
    fi
else
    echo "Template not found, generating from scratch"
    cat > "$FORMULA_FILE" << EOF
class Moribito < Formula
  desc "LDAP CLI Explorer - Interactive terminal-based LDAP client with TUI"
  homepage "https://github.com/ericschmar/moribito"
  version "$VERSION"
  license "MIT"

  on_macos do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-amd64"
      sha256 "$DARWIN_AMD64_SHA256"
    end
    if Hardware::CPU.arm?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-darwin-arm64"  
      sha256 "$DARWIN_ARM64_SHA256"
    end
  end

  on_linux do
    if Hardware::CPU.intel?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-amd64"
      sha256 "$LINUX_AMD64_SHA256"
    end
    if Hardware::CPU.arm? && Hardware::CPU.is_64_bit?
      url "https://github.com/ericschmar/moribito/releases/download/v#{version}/moribito-linux-arm64"
      sha256 "$LINUX_ARM64_SHA256"
    end
  end

  def install
    bin.install "moribito-#{OS.kernel_name.downcase}-#{Hardware::CPU.arch}" => "moribito"
  end

  test do
    # Test version output
    output = shell_output("#{bin}/moribito --version")
    assert_match "moribito version #{version}", output
    
    # Test help output  
    output = shell_output("#{bin}/moribito --help")
    assert_match "LDAP CLI Explorer", output
  end
end
EOF
fi

echo "Formula generated successfully at $FORMULA_FILE"
echo ""
echo "To test the formula locally:"
echo "  brew install --formula $FORMULA_FILE"
echo ""
echo "To create a tap and publish:"
echo "  1. Create a new repository: ericschmar/homebrew-tap"
echo "  2. Copy this formula to Formula/moribito.rb"
echo "  3. Users can then install with: brew install ericschmar/tap/moribito"
echo ""
echo "To submit to homebrew-core:"
echo "  1. Fork https://github.com/Homebrew/homebrew-core"
echo "  2. Add this formula to Formula/m/moribito.rb"
echo "  3. Submit a pull request"