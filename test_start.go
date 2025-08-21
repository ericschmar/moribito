package main

import (
	"fmt"
	"github.com/ericschmar/ldap-cli/internal/config"
	"github.com/ericschmar/ldap-cli/internal/tui"
)

func main() {
	// Create a default config
	cfg := config.Default()
	
	// Create the start view
	sv := tui.NewStartView(cfg)
	sv.SetSize(120, 40) // Set a reasonable size
	
	// Render the view
	output := sv.View()
	fmt.Print(output)
	fmt.Printf("\n=== Width: %d, Height: %d ===\n", 120, 40)
}