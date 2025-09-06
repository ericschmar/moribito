# Development Setup

This guide covers setting up the development environment for Moribito.

## Prerequisites

- Go 1.25.1 or later
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
