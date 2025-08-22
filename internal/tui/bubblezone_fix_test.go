package tui

import (
	"testing"

	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
)

func TestModel_ViewWithoutPanic(t *testing.T) {
	// This test specifically verifies that the bubblezone manager initialization
	// prevents the panic reported in the issue: "manager not initialized"

	// Create a model without an LDAP client (simulating the failure case)
	var client *ldap.Client
	cfg := config.Default()
	model := NewModelWithPageSize(client, cfg)

	// Initialize the model (this should initialize bubblezone)
	model.Init()

	// This call should not panic with "manager not initialized"
	// Previously this would fail on the zone.Clear("") call in View()
	view := model.View()

	// The view should contain some content (not empty)
	if view == "" {
		t.Error("View should return some content, got empty string")
	}

	// The view should not contain panic or error messages
	if view == "Goodbye!\n" {
		t.Error("View should not show goodbye message immediately")
	}
}

func TestModel_BubblezoneManagerInitialized(t *testing.T) {
	// Test that bubblezone manager is properly initialized in Init()
	var client *ldap.Client
	cfg := config.Default()
	model := NewModelWithPageSize(client, cfg)

	// Initialize the model
	model.Init()

	// Try multiple View() calls to ensure bubblezone is stable
	for i := 0; i < 3; i++ {
		view := model.View()
		if view == "" {
			t.Errorf("View call %d returned empty string", i+1)
		}
	}
}
