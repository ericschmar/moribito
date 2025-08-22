package tui

import (
	"strings"
	"testing"
	"time"

	"github.com/ericschmar/moribito/internal/ldap"
)

func TestTreeView_LoadingTimer(t *testing.T) {
	// Create a new tree view
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	// Test initial state - not loading
	if tv.loading {
		t.Error("TreeView should not be loading initially")
	}

	// Test timer message when not loading - should not continue timer
	tickMsg := LoadingTimerTickMsg{Time: time.Now()}
	_, cmd := tv.Update(tickMsg)
	if cmd != nil {
		t.Error("Timer tick should return nil command when not loading")
	}
}

func TestTreeView_LoadingTimerDisplay(t *testing.T) {
	// Create a new tree view
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	// Set loading state manually to test display
	tv.loading = true
	tv.loadingStartTime = time.Now().Add(-2 * time.Second) // 2 seconds ago
	tv.loadingElapsed = 2 * time.Second

	// Get the view and check if it contains elapsed time
	view := tv.View()
	t.Logf("Loading view output:\n%s", view)

	if !strings.Contains(view, "Loading LDAP tree...") {
		t.Error("Loading view should contain loading message")
	}
	if !strings.Contains(view, "2.0s") {
		t.Errorf("Loading view should contain elapsed time '2.0s', got: %s", view)
	}
}

func TestTreeView_TimerTickMessage(t *testing.T) {
	// Create a new tree view
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	// Set loading state
	tv.loading = true
	startTime := time.Now().Add(-1 * time.Second) // 1 second ago
	tv.loadingStartTime = startTime

	// Send a timer tick message
	tickTime := time.Now()
	tickMsg := LoadingTimerTickMsg{Time: tickTime}

	_, cmd := tv.Update(tickMsg)

	// Should continue the timer when loading
	if cmd == nil {
		t.Error("Timer tick should return a command to continue ticking when loading")
	}

	// Check that elapsed time was updated
	expectedElapsed := tickTime.Sub(startTime)
	if tv.loadingElapsed < expectedElapsed-10*time.Millisecond ||
		tv.loadingElapsed > expectedElapsed+10*time.Millisecond {
		t.Errorf("Elapsed time should be approximately %v, got %v", expectedElapsed, tv.loadingElapsed)
	}
}

func TestTreeView_LoadingTimerDisplayFormats(t *testing.T) {
	// Create a new tree view
	var client *ldap.Client
	tv := NewTreeView(client)
	tv.SetSize(80, 24)

	testCases := []struct {
		name     string
		elapsed  time.Duration
		expected string
	}{
		{"0.0 seconds", 0 * time.Millisecond, "(0.0s)"},
		{"0.1 seconds", 100 * time.Millisecond, "(0.1s)"},
		{"0.5 seconds", 500 * time.Millisecond, "(0.5s)"},
		{"1.0 seconds", 1000 * time.Millisecond, "(1.0s)"},
		{"1.2 seconds", 1200 * time.Millisecond, "(1.2s)"},
		{"10.0 seconds", 10000 * time.Millisecond, "(10.0s)"},
		{"59.9 seconds", 59900 * time.Millisecond, "(59.9s)"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			tv.loading = true
			tv.loadingStartTime = time.Now().Add(-tc.elapsed)
			tv.loadingElapsed = tc.elapsed

			view := tv.View()
			if !strings.Contains(view, tc.expected) {
				t.Errorf("Expected view to contain %s, got: %s", tc.expected, view)
			}

			t.Logf("Display for %s: 'Loading LDAP tree... %s'", tc.name, tc.expected)
		})
	}
}
