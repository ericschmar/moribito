package tui

import (
	"testing"
	"time"

	tea "github.com/charmbracelet/bubbletea"
)

// TestCheckForUpdatesCmdDelay verifies that the update check includes a delay
// to prevent blocking UI startup
func TestCheckForUpdatesCmdDelay(t *testing.T) {
	// Create the update check command
	cmd := checkForUpdatesCmd()

	// Measure how long it takes for the command function to return
	start := time.Now()

	// Execute the command in a goroutine since it will block
	done := make(chan tea.Msg, 1)
	go func() {
		msg := cmd()
		done <- msg
	}()

	// Wait for the command to complete with a timeout
	select {
	case <-done:
		elapsed := time.Since(start)
		// The command should take at least 1.5 seconds due to the delay
		// Allow some margin for execution time
		if elapsed < 1400*time.Millisecond {
			t.Errorf("Update check returned too quickly (%v), expected at least 1400ms delay for background execution", elapsed)
		}
		// But shouldn't take too long (network call has 10s timeout, but usually much faster)
		if elapsed > 15*time.Second {
			t.Errorf("Update check took too long (%v), possible network issues or excessive delay", elapsed)
		}
	case <-time.After(20 * time.Second):
		t.Fatal("Update check timed out - this suggests the delay or network call is taking too long")
	}
}

// TestCheckForUpdatesCmdReturnsMessage verifies that the command returns a proper message
func TestCheckForUpdatesCmdReturnsMessage(t *testing.T) {
	// Create the update check command
	cmd := checkForUpdatesCmd()

	// Execute the command in a goroutine
	done := make(chan tea.Msg, 1)
	go func() {
		msg := cmd()
		done <- msg
	}()

	// Wait for result
	select {
	case msg := <-done:
		// Should return an updateCheckMsg
		updateMsg, ok := msg.(updateCheckMsg)
		if !ok {
			t.Errorf("Expected updateCheckMsg, got %T", msg)
			return
		}

		// The message should have some content (either error or update info)
		// We can't predict the exact outcome since it depends on network/GitHub state
		// But we can verify the structure is correct
		t.Logf("Update check result: available=%v, version=%s, error=%v",
			updateMsg.available, updateMsg.version, updateMsg.err)

		// This is mainly a smoke test - as long as we get a proper updateCheckMsg, it's working

	case <-time.After(20 * time.Second):
		t.Fatal("Update check timed out")
	}
}
