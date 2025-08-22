# Installation

## Homebrew (Recommended for macOS/Linux)

### From Custom Tap
```bash
brew install ericschmar/tap/moribito
```

### From Formula URL (if tap not available)
```bash
brew install https://raw.githubusercontent.com/ericschmar/moribito/main/homebrew/moribito.rb
```

## From GitHub Releases

Download the latest pre-built binary from [GitHub Releases](https://github.com/ericschmar/moribito/releases):

```bash
# Linux x86_64
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-linux-amd64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# Linux ARM64
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-linux-arm64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# macOS Intel
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-darwin-amd64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/

# macOS Apple Silicon
curl -L https://github.com/ericschmar/moribito/releases/latest/download/moribito-darwin-arm64 -o moribito
chmod +x moribito
sudo mv moribito /usr/local/bin/
```

For Windows, download `moribito-windows-amd64.exe` from the releases page.

## From Source

```bash
git clone https://github.com/ericschmar/moribito
cd moribito
go build -o moribito cmd/moribito/main.go
```

## Verification

After installation, verify the installation by checking the version:

```bash
moribito --version
```