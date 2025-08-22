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