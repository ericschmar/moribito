.PHONY: build test fmt lint clean run help install build-all docs docs-serve docs-clean

# Version information
VERSION ?= dev
COMMIT ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
DATE ?= $(shell date -u '+%Y-%m-%d_%H:%M:%S_UTC' 2>/dev/null || echo "unknown")

# Build flags for version injection
LDFLAGS = -ldflags "-X github.com/ericschmar/ldap-cli/internal/version.Version=$(VERSION) -X github.com/ericschmar/ldap-cli/internal/version.Commit=$(COMMIT) -X github.com/ericschmar/ldap-cli/internal/version.Date=$(DATE)"

# Build the application
build: bin
	go build $(LDFLAGS) -o bin/ldap-cli cmd/ldap-cli/main.go

# Run tests
test:
	go test -v ./...

# Format code
fmt:
	gofmt -s -w .
	go mod tidy

# Lint code (install golangci-lint if not present)
lint:
	@which golangci-lint > /dev/null || (echo "Installing golangci-lint..." && go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest)
	golangci-lint run

# Clean build artifacts
clean:
	rm -rf bin/
	go clean

# Clean documentation build artifacts  
docs-clean:
	rm -rf _docpress/

# Run the application with example config
run:
	go run cmd/ldap-cli/main.go -config config/example.yaml

# Install the application to GOPATH/bin
install:
	go install $(LDFLAGS) cmd/ldap-cli/main.go

# Run full CI checks
ci: fmt lint test build

# Create binary directory
bin:
	mkdir -p bin

# Build for multiple platforms
build-all: bin
	GOOS=linux GOARCH=amd64 go build $(LDFLAGS) -o bin/ldap-cli-linux-amd64 cmd/ldap-cli/main.go
	GOOS=linux GOARCH=arm64 go build $(LDFLAGS) -o bin/ldap-cli-linux-arm64 cmd/ldap-cli/main.go
	GOOS=darwin GOARCH=amd64 go build $(LDFLAGS) -o bin/ldap-cli-darwin-amd64 cmd/ldap-cli/main.go
	GOOS=darwin GOARCH=arm64 go build $(LDFLAGS) -o bin/ldap-cli-darwin-arm64 cmd/ldap-cli/main.go
	GOOS=windows GOARCH=amd64 go build $(LDFLAGS) -o bin/ldap-cli-windows-amd64.exe cmd/ldap-cli/main.go

# Build documentation website
docs:
	@which npm > /dev/null || (echo "npm is required for documentation generation" && exit 1)
	@[ -d node_modules ] || npm install
	./node_modules/.bin/docpress build

# Serve documentation locally
docs-serve:
	@which npm > /dev/null || (echo "npm is required for documentation generation" && exit 1)
	@[ -d node_modules ] || npm install
	./node_modules/.bin/docpress serve

# Show help
help:
	@echo "Available commands:"
	@echo "  build      - Build the application"
	@echo "  test       - Run tests"
	@echo "  fmt        - Format code"
	@echo "  lint       - Lint code"
	@echo "  clean      - Clean build artifacts"
	@echo "  run        - Run with example config"
	@echo "  install    - Install to GOPATH/bin"
	@echo "  ci         - Run full CI checks (fmt, lint, test, build)"
	@echo "  build-all  - Build for multiple platforms"
	@echo "  docs       - Build documentation website"
	@echo "  docs-serve - Serve documentation locally"
	@echo "  docs-clean - Clean documentation build artifacts"
	@echo "  help       - Show this help"