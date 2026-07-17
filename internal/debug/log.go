package debug

import (
	"io"
	"log"
	"os"
)

var logger = log.New(io.Discard, "", 0)

// Enable opens path for appending and routes all debug output there.
// Call once from main before starting the TUI.
func Enable(path string) error {
	f, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
	if err != nil {
		return err
	}
	logger = log.New(f, "", log.Ltime|log.Lmicroseconds)
	return nil
}

// Log writes a formatted message to the debug log (no-op when not enabled).
func Log(format string, args ...any) {
	logger.Printf(format, args...)
}
