# Release Process Migration

This project has been migrated from a manual tag-based release process to an automated semantic release process using GoReleaser and Semantic Release.

## What Changed

### Before (Manual Process)

-   Releases were triggered by manually creating Git tags (e.g., `v0.2.3`)
-   Manual build process using custom scripts
-   Manual Homebrew formula generation and tap management
-   Manual release notes creation

### After (Automated Process)

-   Releases are triggered automatically on pushes to the `main` branch
-   Semantic Release analyzes commit messages and automatically creates appropriate version tags
-   GoReleaser handles building, packaging, and releasing binaries
-   Homebrew formula is automatically updated in the tap repository
-   Release notes are generated automatically from commit messages

## How It Works

1. **Commit to Main**: When you push commits to the `main` branch, the release workflow runs
2. **Semantic Analysis**: Semantic Release analyzes your commit messages to determine the next version number
3. **Automatic Tagging**: If a release is warranted, a new Git tag is created automatically
4. **Build & Release**: GoReleaser builds binaries for all platforms and creates a GitHub release
5. **Homebrew Update**: The Homebrew tap is automatically updated with the new formula

## Commit Message Format

To trigger releases, use [Conventional Commits](https://www.conventionalcommits.org/) format:

-   `feat: add new feature` → Minor version bump (e.g., 1.0.0 → 1.1.0)
-   `fix: fix a bug` → Patch version bump (e.g., 1.0.0 → 1.0.1)
-   `feat!: breaking change` → Major version bump (e.g., 1.0.0 → 2.0.0)
-   `docs: update readme` → No release (documentation only)
-   `chore: update dependencies` → No release (maintenance only)

## Prerequisites

### 1. Homebrew Tap Repository

You need to create a separate repository for the Homebrew tap:

-   Repository name: `homebrew-moribito`
-   Owner: `ericschmar`
-   Must be public
-   URL: `https://github.com/ericschmar/homebrew-moribito`

### 2. GitHub Token (Optional Enhancement)

For enhanced security, you can create a fine-grained personal access token:

1. Go to GitHub Settings → Developer Settings → Personal Access Tokens → Fine-grained tokens
2. Create a token with access to both repositories
3. Grant these permissions:
    - Actions: Read and write
    - Commit statuses: Read and write
    - Contents: Read and write
    - Deployments: Read and write
    - Environments: Read-only
    - Secrets: Read-only
    - Variables: Read-only
4. Add the token as `GH_TOKEN` in repository secrets

**Note**: Currently using the built-in `GITHUB_TOKEN` which should work for most use cases.

## Files Added/Modified

### New Files

-   `.goreleaser.yaml` - GoReleaser configuration
-   `.releaserc.json` - Semantic Release configuration
-   `docs/RELEASE_PROCESS.md` - This documentation

### Modified Files

-   `.github/workflows/release.yml` - Updated to use semantic release workflow

### Deprecated Files

The following files are no longer needed but kept for reference:

-   `homebrew/generate-formula.sh` - Replaced by GoReleaser
-   `homebrew/setup-tap.sh` - Replaced by GoReleaser
-   `homebrew/moribito-template.rb` - Replaced by GoReleaser

## Installation for Users

Users can now install moribito using:

```bash
# Add the tap
brew tap ericschmar/moribito

# Install
brew install moribito
```

Or in one command:

```bash
brew install ericschmar/moribito/moribito
```

## Development Workflow

1. Make changes and commit using conventional commit format
2. Push to `main` branch
3. The release workflow will automatically:
    - Analyze commits
    - Create a release if needed
    - Build and upload binaries
    - Update the Homebrew tap
    - Generate release notes

## Troubleshooting

### No Release Created

-   Check that your commit messages follow conventional commit format
-   Commits like `docs:`, `chore:`, `ci:`, `test:` don't trigger releases
-   Use `feat:` for new features or `fix:` for bug fixes

### Homebrew Tap Issues

-   Ensure the `homebrew-moribito` repository exists and is public
-   Check that the GitHub token has access to both repositories
-   Verify the repository owner/name in `.goreleaser.yaml`

### Build Failures

-   Check the GitHub Actions logs for detailed error messages
-   Ensure all tests pass with `make test`
-   Verify Go modules are clean with `go mod tidy`
