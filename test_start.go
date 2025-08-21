package main

import (
	"fmt"
	"github.com/ericschmar/ldap-cli/internal/config"
	"github.com/ericschmar/ldap-cli/internal/tui"
)

func main() {
	// Create a config with some test values
	cfg := &config.Config{
		LDAP: config.LDAPConfig{
			Host:     "ldap.example.com",
			Port:     636,
			BaseDN:   "dc=example,dc=com",
			UseSSL:   true,
			UseTLS:   false,
			BindUser: "cn=admin,dc=example,dc=com",
			BindPass: "secretpassword",
		},
		Pagination: config.PaginationConfig{
			PageSize: 100,
		},
	}
	
	// Create the start view
	sv := tui.NewStartView(cfg)
	sv.SetSize(120, 25) // Set a reasonable size
	
	// Render the view
	fmt.Println("=== Start Page Rendering Test (120x25) ===")
	output := sv.View()
	fmt.Print(output)
	fmt.Printf("\n=== End of Output ===\n")
}