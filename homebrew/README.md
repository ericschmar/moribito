# Homebrew Distribution for Moribito

This directory contains the infrastructure for distributing moribito via Homebrew, a package manager for macOS and Linux.

## Quick Start

### For Users

#### Option 1: Install from custom tap (recommended)
```bash
brew install ericschmar/tap/moribito
```

#### Option 2: Install from formula URL (if tap not available)
```bash
brew install https://raw.githubusercontent.com/ericschmar/moribito/main/homebrew/moribito.rb
```

#### Option 3: Install from homebrew-core (if accepted)
```bash
brew install moribito
```

### For Maintainers

## Files in this Directory

- `moribito.rb` - Source-based Homebrew formula (builds from source)
- `moribito-binary.rb` - Binary-based formula template (installs pre-built binaries)
- `generate-formula.sh` - Script to generate/update the binary formula with SHA256 checksums
- `README.md` - This documentation file

## Creating a New Release Formula

When creating a new release, update the Homebrew formula:

1. Create the GitHub release first (this is done automatically by CI when you tag)
2. Run the formula generator:
   ```bash
   ./homebrew/generate-formula.sh -v 1.0.0
   ```
3. Test the formula locally:
   ```bash
   brew install --formula ./homebrew/moribito.rb
   brew test moribito
   ```

## Setting Up a Custom Tap

To distribute via a custom Homebrew tap:

### 1. Create the Tap Repository

Create a new GitHub repository named `homebrew-tap` under your user/organization:
```bash
# Repository should be named: ericschmar/homebrew-tap
```

### 2. Add the Formula

Copy the generated formula to the tap repository:
```bash
# In the homebrew-tap repository
mkdir -p Formula
cp /path/to/moribito/homebrew/moribito.rb Formula/moribito.rb
git add Formula/moribito.rb
git commit -m "Add moribito formula"
git push
```

### 3. Users Can Install

Once the tap is set up, users can install with:
```bash
brew tap ericschmar/tap
brew install moribito
```

Or in one command:
```bash
brew install ericschmar/tap/moribito
```

## Submitting to homebrew-core

To submit to the official Homebrew repository:

### Requirements

1. **Popularity**: The software should have some popularity/usage
2. **License**: Must have an open-source license (✅ we have MIT)
3. **Stability**: Should be stable and actively maintained
4. **No duplication**: No existing formula for the same software

### Process

1. Fork the [homebrew-core](https://github.com/Homebrew/homebrew-core) repository
2. Create the formula in the correct location:
   ```bash
   # Formula should be at: Formula/m/moribito.rb
   ```
3. Test the formula extensively:
   ```bash
   brew install --formula ./Formula/m/moribito.rb
   brew test moribito
   brew audit --strict moribito
   ```
4. Submit a pull request with:
   - Clear description of what the software does
   - Link to the homepage/repository
   - Evidence of popularity (stars, downloads, etc.)

### Formula Requirements for homebrew-core

- Must build from source (use `moribito.rb`, not the binary version)
- Must include proper license
- Must include comprehensive test
- Must follow Homebrew style guidelines
- Should include completion scripts if available

## Testing the Formula

### Basic Testing
```bash
# Install the formula
brew install --formula ./homebrew/moribito.rb

# Test it works
moribito --version
moribito --help

# Run the built-in formula tests
brew test moribito

# Uninstall when done testing
brew uninstall moribito
```

### Advanced Testing
```bash
# Audit the formula for style and correctness
brew audit --strict moribito

# Test on multiple architectures (if available)
# macOS Intel
arch -x86_64 brew install moribito

# macOS Apple Silicon  
arch -arm64 brew install moribito
```

## Maintenance

### Updating for New Releases

1. Update the version in the formula
2. Update the URL to point to the new release
3. Update the SHA256 checksums (use `generate-formula.sh`)
4. Test the updated formula
5. Commit and push changes

### Automated Updates

Consider setting up a GitHub Action in the tap repository to automatically update the formula when new releases are created.

## Troubleshooting

### Common Issues

1. **SHA256 mismatch**: Re-run `generate-formula.sh` to get fresh checksums
2. **Binary not found**: Ensure the release assets have the correct names
3. **Test failures**: Update the test block to match current CLI interface

### Getting Help

- [Homebrew Formula Cookbook](https://docs.brew.sh/Formula-Cookbook)
- [Homebrew Package Manager Docs](https://docs.brew.sh/)
- [Adding Software to Homebrew](https://docs.brew.sh/Adding-Software-to-Homebrew)

## Security Considerations

- Always verify SHA256 checksums match the actual release binaries
- Only install formulas from trusted sources
- Review formula contents before installation when using URL-based installation