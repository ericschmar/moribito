# Development Setup

This guide covers setting up the development environment for LDAP CLI Explorer.

## Prerequisites

- Go 1.24.6 or later
- Git
- Make (for build automation)

## Building

```bash
# Build for current platform
make build

# Build for all platforms
make build-all

# Clean build artifacts
make clean
```

## Code Quality

```bash
# Format code
make fmt

# Run linter
make lint

# Run tests
make test

# Run all CI checks (format, lint, test, build)
make ci
```

## Testing

```bash
# Run all tests
go test ./...

# Run tests with verbose output
make test
```

## Homebrew Distribution

This project supports distribution via Homebrew. The `homebrew/` directory in the project root contains:

- Homebrew formula files
- Formula generation and maintenance scripts
- Documentation for setting up custom taps
- Instructions for submitting to homebrew-core

### Creating a Release with Homebrew Support

1. **Create the GitHub release** (this triggers the automated process):
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

The release workflow will automatically:
- Build binaries for all platforms
- Create GitHub release with binaries and checksums  
- Generate and update the Homebrew formula
- Update the Homebrew tap repository

2. **Manual testing** (optional, for verification):
   ```bash
   # Test the updated formula locally
   brew install --formula ./homebrew/moribito.rb
   brew test moribito
   moribito --version
   brew uninstall moribito
   
   # Or test from the tap
   brew install ericschmar/tap/moribito
   moribito --version
   brew uninstall moribito
   ```

### Manual Homebrew Updates (if needed)

If you need to manually update the homebrew formula outside of a release:

1. **Generate/update the Homebrew formula**:
   ```bash
   ./homebrew/generate-formula.sh -v 1.0.0 -f
   ```

2. **Update your Homebrew tap**:
   ```bash
   ./homebrew/setup-tap.sh
   ```

## Continuous Integration

This project uses GitHub Actions for CI/CD:

- **CI Workflow**: Runs on every push and pull request to `main` and `develop` branches
  - Code formatting verification
  - Linting (with warnings)
  - Testing
  - Building for current platform
  - Multi-platform build artifacts (on main branch pushes)

- **Release Workflow**: Triggered by version tags (e.g., `v1.0.0`)
  - Runs full CI checks
  - Builds for all platforms (Linux amd64/arm64, macOS amd64/arm64, Windows amd64)
  - Creates GitHub releases with binaries and checksums
  - **Automatically generates and updates Homebrew formula**
  - **Automatically updates the Homebrew tap repository**
  - Generates installation instructions

## Dependencies

- [BubbleTea](https://github.com/charmbracelet/bubbletea) - TUI framework
- [Lipgloss](https://github.com/charmbracelet/lipgloss) - Styling
- [go-ldap](https://github.com/go-ldap/ldap) - LDAP client
- [golang.org/x/term](https://golang.org/x/term) - Terminal utilities

## Documentation

This project uses DocPress for documentation generation. To build and serve the documentation locally:

```bash
# Build documentation
make docs

# Serve documentation locally
make docs-serve
```

### Automatic Deployment

Documentation is automatically built and deployed to GitHub Pages when:

- Changes are pushed to the `main` branch that affect documentation files
- Changes are made to `docs/**`, `docpress.json`, or `package.json`
- The workflow can also be triggered manually from the Actions tab

The deployed documentation is available at: https://ericschmar.github.io/moribito
