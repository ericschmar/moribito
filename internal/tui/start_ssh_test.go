package tui

import (
	"strconv"
	"testing"

	"github.com/ericschmar/moribito/internal/config"
)

func newTestStartView(cfg *config.Config) *StartView {
	return NewStartView(cfg)
}

// setAndSave simulates editing a field: sets editingField and textInput, then calls saveValue.
func setAndSave(sv *StartView, field int, value string) {
	sv.editingField = field
	sv.textInput.SetValue(value)
	sv.saveValue()
}

// ---- isFieldVisible tests ----

func TestIsFieldVisible_TunnelDisabled(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.SSHTunnel.Enabled = false
	sv := newTestStartView(cfg)

	alwaysHidden := []int{
		FieldSSHHost, FieldSSHPort, FieldSSHUser, FieldSSHAuthMethod,
		FieldSSHPassword, FieldSSHKeyFile, FieldSSHKeyPassphrase,
		FieldSSHIgnoreHostKey,
	}
	for _, f := range alwaysHidden {
		if sv.isFieldVisible(f) {
			t.Errorf("field %d should be hidden when tunnel disabled", f)
		}
	}
}

func TestIsFieldVisible_TunnelEnabled(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.SSHTunnel.Enabled = true
	sv := newTestStartView(cfg)

	sshFields := []int{
		FieldSSHHost, FieldSSHPort, FieldSSHUser, FieldSSHAuthMethod,
		FieldSSHPassword, FieldSSHKeyFile, FieldSSHKeyPassphrase,
		FieldSSHIgnoreHostKey,
	}
	for _, f := range sshFields {
		if !sv.isFieldVisible(f) {
			t.Errorf("field %d should be visible when tunnel enabled", f)
		}
	}
}

func TestIsFieldVisible_NonSSHFieldsAlwaysVisible(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.SSHTunnel.Enabled = false
	sv := newTestStartView(cfg)

	alwaysVisible := []int{
		FieldHost, FieldPort, FieldBaseDN, FieldUseSSL, FieldUseTLS,
		FieldBindUser, FieldBindPass, FieldPageSize,
		FieldSSHTunnelEnabled, // the toggle itself is always visible
		FieldConnect,
	}
	for _, f := range alwaysVisible {
		if !sv.isFieldVisible(f) {
			t.Errorf("field %d should always be visible", f)
		}
	}
}

// ---- getFieldValue / saveValue round-trip tests ----

func TestSSHFieldRoundTrip_StringFields(t *testing.T) {
	tests := []struct {
		field int
		value string
		get   func(*config.Config) string
	}{
		{FieldSSHHost, "bastion.example.com", func(c *config.Config) string { return c.LDAP.SSHTunnel.Host }},
		{FieldSSHUser, "ops", func(c *config.Config) string { return c.LDAP.SSHTunnel.User }},
		{FieldSSHAuthMethod, "key", func(c *config.Config) string { return c.LDAP.SSHTunnel.AuthMethod }},
		{FieldSSHPassword, "secret", func(c *config.Config) string { return c.LDAP.SSHTunnel.Password }},
		{FieldSSHKeyFile, "~/.ssh/id_ed25519", func(c *config.Config) string { return c.LDAP.SSHTunnel.KeyFile }},
		{FieldSSHKeyPassphrase, "phrase", func(c *config.Config) string { return c.LDAP.SSHTunnel.KeyPassphrase }},
	}

	for _, tc := range tests {
		t.Run(strconv.Itoa(tc.field), func(t *testing.T) {
			cfg := config.Default()
			sv := newTestStartView(cfg)

			setAndSave(sv, tc.field, tc.value)

			if got := tc.get(cfg); got != tc.value {
				t.Errorf("field %d: got %q, want %q", tc.field, got, tc.value)
			}

			if got := sv.getFieldValue(tc.field); got != tc.value {
				t.Errorf("field %d getFieldValue: got %q, want %q", tc.field, got, tc.value)
			}
		})
	}
}

func TestSSHFieldRoundTrip_PortField(t *testing.T) {
	cfg := config.Default()
	sv := newTestStartView(cfg)

	setAndSave(sv, FieldSSHPort, "2222")

	if cfg.LDAP.SSHTunnel.Port != 2222 {
		t.Errorf("FieldSSHPort: got %d, want 2222", cfg.LDAP.SSHTunnel.Port)
	}
	if got := sv.getFieldValue(FieldSSHPort); got != "2222" {
		t.Errorf("FieldSSHPort getFieldValue: got %q, want \"2222\"", got)
	}
}

func TestSSHFieldRoundTrip_PortZeroDefault(t *testing.T) {
	// When port is 0 (not set), getFieldValue should return empty string (not "0")
	cfg := config.Default()
	sv := newTestStartView(cfg)

	if got := sv.getFieldValue(FieldSSHPort); got != "" {
		t.Errorf("FieldSSHPort with zero value: got %q, want empty string", got)
	}
}

func TestSSHFieldRoundTrip_EnabledToggle(t *testing.T) {
	cfg := config.Default()
	sv := newTestStartView(cfg)

	setAndSave(sv, FieldSSHTunnelEnabled, "true")
	if !cfg.LDAP.SSHTunnel.Enabled {
		t.Error("expected Enabled=true after saving 'true'")
	}
	if got := sv.getFieldValue(FieldSSHTunnelEnabled); got != "true" {
		t.Errorf("getFieldValue: got %q, want 'true'", got)
	}

	setAndSave(sv, FieldSSHTunnelEnabled, "false")
	if cfg.LDAP.SSHTunnel.Enabled {
		t.Error("expected Enabled=false after saving 'false'")
	}
}

func TestSSHFieldRoundTrip_IgnoreHostKeyToggle(t *testing.T) {
	cfg := config.Default()
	sv := newTestStartView(cfg)

	setAndSave(sv, FieldSSHIgnoreHostKey, "true")
	if !cfg.LDAP.SSHTunnel.InsecureIgnoreHostKey {
		t.Error("expected InsecureIgnoreHostKey=true after saving 'true'")
	}
	if got := sv.getFieldValue(FieldSSHIgnoreHostKey); got != "true" {
		t.Errorf("getFieldValue: got %q, want 'true'", got)
	}

	setAndSave(sv, FieldSSHIgnoreHostKey, "false")
	if cfg.LDAP.SSHTunnel.InsecureIgnoreHostKey {
		t.Error("expected InsecureIgnoreHostKey=false after saving 'false'")
	}
}

func TestSSHFieldRoundTrip_InvalidPort(t *testing.T) {
	cfg := config.Default()
	cfg.LDAP.SSHTunnel.Port = 22
	sv := newTestStartView(cfg)

	// Invalid port values should not overwrite existing value
	setAndSave(sv, FieldSSHPort, "notaport")
	if cfg.LDAP.SSHTunnel.Port != 22 {
		t.Errorf("expected port to remain 22 after invalid input, got %d", cfg.LDAP.SSHTunnel.Port)
	}

	setAndSave(sv, FieldSSHPort, "0")
	if cfg.LDAP.SSHTunnel.Port != 22 {
		t.Errorf("expected port to remain 22 after port=0, got %d", cfg.LDAP.SSHTunnel.Port)
	}

	setAndSave(sv, FieldSSHPort, "99999")
	if cfg.LDAP.SSHTunnel.Port != 22 {
		t.Errorf("expected port to remain 22 after out-of-range port, got %d", cfg.LDAP.SSHTunnel.Port)
	}
}
