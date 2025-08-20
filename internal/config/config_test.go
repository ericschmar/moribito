package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestPaginationConfig(t *testing.T) {
	// Test default configuration includes pagination
	cfg := Default()

	if cfg.Pagination.PageSize != 50 {
		t.Errorf("Expected default page size to be 50, got %d", cfg.Pagination.PageSize)
	}
}

func TestConfigLoadWithPagination(t *testing.T) {
	// Create a temporary config file
	tempDir, err := os.MkdirTemp("", "ldap-cli-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
  use_ssl: false
  use_tls: false
pagination:
  page_size: 25
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check pagination settings
	if cfg.Pagination.PageSize != 25 {
		t.Errorf("Expected page size to be 25, got %d", cfg.Pagination.PageSize)
	}

	// Check LDAP settings
	if cfg.LDAP.Host != "test.example.com" {
		t.Errorf("Expected host to be test.example.com, got %s", cfg.LDAP.Host)
	}
}

func TestConfigLoadWithoutPagination(t *testing.T) {
	// Create a temporary config file without pagination section
	tempDir, err := os.MkdirTemp("", "ldap-cli-test")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	configPath := filepath.Join(tempDir, "config.yaml")
	configContent := `ldap:
  host: test.example.com
  port: 389
  base_dn: dc=test,dc=com
`

	err = os.WriteFile(configPath, []byte(configContent), 0644)
	if err != nil {
		t.Fatalf("Failed to write config file: %v", err)
	}

	// Load the config
	cfg, err := Load(configPath)
	if err != nil {
		t.Fatalf("Failed to load config: %v", err)
	}

	// Check that default pagination is applied
	if cfg.Pagination.PageSize != 50 {
		t.Errorf("Expected default page size to be 50, got %d", cfg.Pagination.PageSize)
	}
}
