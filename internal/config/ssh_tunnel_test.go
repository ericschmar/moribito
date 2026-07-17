package config

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"gopkg.in/yaml.v3"
)

func TestSSHTunnelConfig_YAMLRoundTrip(t *testing.T) {
	original := SavedConnection{
		Name:     "test",
		Host:     "ldap.internal",
		Port:     636,
		BaseDN:   "dc=internal,dc=corp",
		UseSSL:   true,
		BindUser: "cn=reader,dc=internal,dc=corp",
		BindPass: "secret",
		SSHTunnel: SSHTunnelConfig{
			Enabled:       true,
			Host:          "bastion.example.com",
			Port:          22,
			User:          "ops",
			AuthMethod:    "key",
			KeyFile:       "~/.ssh/id_ed25519",
			KeyPassphrase: "keypass",
		},
	}

	data, err := yaml.Marshal(original)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	var got SavedConnection
	if err := yaml.Unmarshal(data, &got); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}

	ssh := got.SSHTunnel
	if !ssh.Enabled {
		t.Error("Enabled not preserved")
	}
	if ssh.Host != "bastion.example.com" {
		t.Errorf("Host: got %q", ssh.Host)
	}
	if ssh.Port != 22 {
		t.Errorf("Port: got %d", ssh.Port)
	}
	if ssh.User != "ops" {
		t.Errorf("User: got %q", ssh.User)
	}
	if ssh.AuthMethod != "key" {
		t.Errorf("AuthMethod: got %q", ssh.AuthMethod)
	}
	if ssh.KeyFile != "~/.ssh/id_ed25519" {
		t.Errorf("KeyFile: got %q", ssh.KeyFile)
	}
	if ssh.KeyPassphrase != "keypass" {
		t.Errorf("KeyPassphrase: got %q", ssh.KeyPassphrase)
	}
}

func TestSSHTunnelConfig_OmittedWhenEmpty(t *testing.T) {
	conn := SavedConnection{Name: "plain", Host: "ldap.example.com", Port: 389}

	data, err := yaml.Marshal(conn)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	// ssh_tunnel block should not appear at all when zero-value
	if strings.Contains(string(data), "ssh_tunnel") {
		t.Error("expected ssh_tunnel to be omitted when empty")
	}
}

func TestSSHTunnelConfig_LoadFromYAML(t *testing.T) {
	tempDir := t.TempDir()
	configPath := filepath.Join(tempDir, "config.yaml")

	content := `ldap:
  host: ldap.example.com
  port: 389
  base_dn: dc=example,dc=com
  ssh_tunnel:
    enabled: true
    host: bastion.example.com
    port: 22
    user: ops
    auth_method: key
    key_file: ~/.ssh/id_rsa
  saved_connections:
    - name: "Via bastion"
      host: ldap.internal
      port: 636
      base_dn: dc=internal,dc=corp
      use_ssl: true
      ssh_tunnel:
        enabled: true
        host: bastion.internal
        port: 22
        user: admin
        auth_method: agent
`
	if err := os.WriteFile(configPath, []byte(content), 0644); err != nil {
		t.Fatalf("write: %v", err)
	}

	cfg, _, err := Load(configPath)
	if err != nil {
		t.Fatalf("Load: %v", err)
	}

	// Default/working SSH tunnel
	if !cfg.LDAP.SSHTunnel.Enabled {
		t.Error("expected working SSH tunnel to be enabled")
	}
	if cfg.LDAP.SSHTunnel.Host != "bastion.example.com" {
		t.Errorf("Host: got %q", cfg.LDAP.SSHTunnel.Host)
	}
	if cfg.LDAP.SSHTunnel.AuthMethod != "key" {
		t.Errorf("AuthMethod: got %q", cfg.LDAP.SSHTunnel.AuthMethod)
	}

	// Saved connection SSH tunnel
	if len(cfg.LDAP.SavedConnections) != 1 {
		t.Fatalf("expected 1 saved connection, got %d", len(cfg.LDAP.SavedConnections))
	}
	sc := cfg.LDAP.SavedConnections[0]
	if !sc.SSHTunnel.Enabled {
		t.Error("expected saved connection SSH tunnel to be enabled")
	}
	if sc.SSHTunnel.AuthMethod != "agent" {
		t.Errorf("saved AuthMethod: got %q", sc.SSHTunnel.AuthMethod)
	}
}

func TestGetActiveConnection_IncludesSSHTunnel(t *testing.T) {
	tunnel := SSHTunnelConfig{
		Enabled:    true,
		Host:       "bastion.example.com",
		Port:       22,
		User:       "ops",
		AuthMethod: "password",
		Password:   "pass",
	}

	t.Run("default connection", func(t *testing.T) {
		cfg := Default()
		cfg.LDAP.SSHTunnel = tunnel

		conn := cfg.GetActiveConnection()
		if !conn.SSHTunnel.Enabled {
			t.Error("expected SSH tunnel in default connection")
		}
		if conn.SSHTunnel.Host != "bastion.example.com" {
			t.Errorf("Host: got %q", conn.SSHTunnel.Host)
		}
	})

	t.Run("saved connection", func(t *testing.T) {
		cfg := Default()
		cfg.LDAP.SavedConnections = []SavedConnection{
			{Name: "test", Host: "ldap.example.com", Port: 389, SSHTunnel: tunnel},
		}
		cfg.LDAP.SelectedConnection = 0

		conn := cfg.GetActiveConnection()
		if !conn.SSHTunnel.Enabled {
			t.Error("expected SSH tunnel in saved connection")
		}
		if conn.SSHTunnel.Password != "pass" {
			t.Errorf("Password: got %q", conn.SSHTunnel.Password)
		}
	})
}

func TestSetActiveConnection_CopiesSSHTunnel(t *testing.T) {
	tunnel := SSHTunnelConfig{
		Enabled:    true,
		Host:       "bastion.example.com",
		Port:       22,
		User:       "ops",
		AuthMethod: "key",
		KeyFile:    "~/.ssh/id_ed25519",
	}

	cfg := Default()
	cfg.LDAP.SavedConnections = []SavedConnection{
		{Name: "test", Host: "ldap.example.com", Port: 389, SSHTunnel: tunnel},
	}

	cfg.SetActiveConnection(0)

	if !cfg.LDAP.SSHTunnel.Enabled {
		t.Error("expected SSH tunnel to be copied to working config")
	}
	if cfg.LDAP.SSHTunnel.Host != "bastion.example.com" {
		t.Errorf("Host: got %q", cfg.LDAP.SSHTunnel.Host)
	}
	if cfg.LDAP.SSHTunnel.KeyFile != "~/.ssh/id_ed25519" {
		t.Errorf("KeyFile: got %q", cfg.LDAP.SSHTunnel.KeyFile)
	}
}

func TestSSHTunnelConfig_SaveAndReload(t *testing.T) {
	tempDir := t.TempDir()
	configPath := filepath.Join(tempDir, "config.yaml")

	cfg := Default()
	cfg.LDAP.Host = "ldap.example.com"
	cfg.LDAP.Port = 389
	cfg.LDAP.BaseDN = "dc=example,dc=com"
	cfg.LDAP.SSHTunnel = SSHTunnelConfig{
		Enabled:    true,
		Host:       "bastion.example.com",
		Port:       22,
		User:       "ops",
		AuthMethod: "key",
		KeyFile:    "~/.ssh/id_rsa",
	}

	if err := cfg.Save(configPath); err != nil {
		t.Fatalf("Save: %v", err)
	}

	loaded, _, err := Load(configPath)
	if err != nil {
		t.Fatalf("Load: %v", err)
	}

	if !loaded.LDAP.SSHTunnel.Enabled {
		t.Error("Enabled not preserved through save/load")
	}
	if loaded.LDAP.SSHTunnel.Host != "bastion.example.com" {
		t.Errorf("Host: got %q", loaded.LDAP.SSHTunnel.Host)
	}
	if loaded.LDAP.SSHTunnel.KeyFile != "~/.ssh/id_rsa" {
		t.Errorf("KeyFile: got %q", loaded.LDAP.SSHTunnel.KeyFile)
	}
}
