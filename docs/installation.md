# Installation

## From GitHub Releases (Recommended)

Download the latest pre-built binary from [GitHub Releases](https://github.com/ericschmar/ldap-cli/releases):

```bash
# Linux x86_64
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-linux-amd64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# Linux ARM64
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-linux-arm64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# macOS Intel
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-darwin-amd64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/

# macOS Apple Silicon
curl -L https://github.com/ericschmar/ldap-cli/releases/latest/download/ldap-cli-darwin-arm64 -o ldap-cli
chmod +x ldap-cli
sudo mv ldap-cli /usr/local/bin/
```

For Windows, download `ldap-cli-windows-amd64.exe` from the releases page.

## From Source

```bash
git clone https://github.com/ericschmar/ldap-cli
cd ldap-cli
go build -o ldap-cli cmd/ldap-cli/main.go
```

## Verification

After installation, verify the installation by checking the version:

```bash
ldap-cli --version
```