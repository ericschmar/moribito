# Versioning Guide

This project uses [Semantic Versioning](https://semver.org/) for version management.

## Version Format

Versions follow the format `MAJOR.MINOR.PATCH` where:

- **MAJOR**: Incompatible API changes
- **MINOR**: New functionality in a backwards compatible manner  
- **PATCH**: Backwards compatible bug fixes

## Release Process

### 1. Update CHANGELOG.md

Before creating a release, update `CHANGELOG.md`:

1. Move items from `[Unreleased]` section to a new release section
2. Use the format: `## [1.0.0] - 2024-01-01`
3. Include sections as appropriate: Added, Changed, Deprecated, Removed, Fixed, Security

### 2. Create and Push Version Tag

```bash
# Example for version 1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### 3. Automated Release

The GitHub Actions workflow will automatically:

1. Run CI checks (format, lint, test)
2. Build binaries for all platforms with embedded version info
3. Create GitHub release with binaries and checksums
4. **Generate and update Homebrew formula**
5. **Update Homebrew tap repository for easy installation**
6. Generate installation instructions

## Version Information

The application embeds version information at build time:

```bash
# Check version
./moribito --version

# Build with specific version (done automatically in CI)
make build VERSION=1.0.0
```

## Development Versions

- Development builds use version "dev"
- Local builds include git commit hash and build date
- Released binaries include exact version, commit, and build date

## Version in Code

Version information is managed in `internal/version/version.go`:

```go
import "github.com/ericschmar/moribito/internal/version"

info := version.Get()
fmt.Println(info.String()) // Full version info
fmt.Println(info.Version)  // Just the version number
```