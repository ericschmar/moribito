#!/bin/bash

# Homebrew Tap Setup Helper for moribito
# This script helps set up a Homebrew tap repository

set -e

OWNER="ericschmar"
REPO_NAME="homebrew-tap"
TAP_REPO="$OWNER/$REPO_NAME"

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "This script helps you set up a Homebrew tap for moribito."
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Prerequisites:"
    echo "  1. You must have a GitHub repository named: $TAP_REPO"
    echo "  2. The repository should be public"
    echo "  3. You should have the 'gh' CLI tool installed and authenticated"
    echo ""
    echo "What this script does:"
    echo "  1. Clones your tap repository (or creates it if it doesn't exist)"
    echo "  2. Sets up the proper directory structure"
    echo "  3. Copies the current formula"
    echo "  4. Commits and pushes the changes"
    echo ""
    echo "After running this script, users can install moribito with:"
    echo "  brew install $OWNER/tap/moribito"
    echo ""
    exit 1
}

check_prerequisites() {
    # Check if formula exists
    if [ ! -f "homebrew/moribito.rb" ]; then
        echo "Error: Formula file not found at homebrew/moribito.rb"
        echo "Please run ./homebrew/generate-formula.sh first"
        exit 1
    fi

    # If running in GitHub Actions, we don't need gh CLI
    if [ -n "$GITHUB_TOKEN" ]; then
        echo "Running in GitHub Actions environment with token authentication"
        return 0
    fi

    # Check if gh CLI is available (for local usage)
    if ! command -v gh >/dev/null 2>&1; then
        echo "Error: GitHub CLI (gh) is not installed."
        echo "Please install it from: https://cli.github.com/"
        echo "Or set GITHUB_TOKEN environment variable for token-based authentication"
        exit 1
    fi

    # Check if authenticated (for local usage)
    if ! gh auth status >/dev/null 2>&1; then
        echo "Error: GitHub CLI is not authenticated."
        echo "Please run: gh auth login"
        echo "Or set GITHUB_TOKEN environment variable for token-based authentication"
        exit 1
    fi
}

create_or_update_tap() {
    local temp_dir=$(mktemp -d)
    local original_dir=$(pwd)
    echo "Working in temporary directory: $temp_dir"
    cd "$temp_dir"

    # Try to clone existing repository
    echo "Checking if tap repository exists..."
    
    if [ -n "$GITHUB_TOKEN" ]; then
        # Use token-based authentication (GitHub Actions)
        echo "Using GitHub token authentication..."
        if git clone "https://${GITHUB_TOKEN}@github.com/${TAP_REPO}.git" "$REPO_NAME" 2>/dev/null; then
            echo "Found existing tap repository"
            cd "$REPO_NAME"
        else
            echo "Tap repository not found. You'll need to create it manually."
            echo "Please create repository: https://github.com/$TAP_REPO"
            echo "Then re-run this script."
            exit 1
        fi
    else
        # Use gh CLI (local development)
        if gh repo clone "$TAP_REPO" 2>/dev/null; then
            echo "Found existing tap repository"
            cd "$REPO_NAME"
        else
            echo "Tap repository not found. Creating new repository..."
            
            # Create the repository
            gh repo create "$TAP_REPO" --public --description "Homebrew tap for $OWNER's packages" --confirm
            
            # Clone the newly created repository
            gh repo clone "$TAP_REPO"
            cd "$REPO_NAME"
            
            # Create initial README
            cat > README.md << 'EOF'
# Homebrew Tap

This is a Homebrew tap for various packages.

## Usage

```bash
# Add the tap
brew tap ericschmar/tap

# Install packages
brew install moribito
```

## Available Packages

- **moribito** - LDAP CLI Explorer with TUI
EOF
            git add README.md
            git commit -m "Initial commit: Add README"
            git push origin main
        fi
    fi

    # Create Formula directory if it doesn't exist
    mkdir -p Formula

    # Copy the formula
    echo "Copying formula to tap repository..."
    cp "$original_dir/homebrew/moribito.rb" Formula/moribito.rb

    # Commit and push
    git add Formula/moribito.rb
    if git diff --staged --quiet; then
        echo "No changes to commit - formula is up to date"
    else
        echo "Committing formula changes..."
        git commit -m "Update moribito formula"
        git push origin main
        echo "Formula pushed successfully!"
    fi

    echo ""
    echo "✅ Tap setup complete!"
    echo ""
    echo "Your tap is now available at: https://github.com/$TAP_REPO"
    echo ""
    echo "Users can now install moribito with:"
    echo "  brew tap $OWNER/tap"
    echo "  brew install moribito"
    echo ""
    echo "Or in one command:"
    echo "  brew install $TAP_REPO/moribito"

    # Cleanup
    cd "$original_dir"
    rm -rf "$temp_dir"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

echo "🍺 Homebrew Tap Setup for moribito"
echo "=================================="
echo ""

check_prerequisites
create_or_update_tap