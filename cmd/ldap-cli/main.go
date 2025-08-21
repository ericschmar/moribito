package main

import (
	"flag"
	"fmt"
	"log"
	"syscall"

	"github.com/charmbracelet/bubbletea"
	"golang.org/x/term"

	"github.com/ericschmar/ldap-cli/internal/config"
	"github.com/ericschmar/ldap-cli/internal/ldap"
	"github.com/ericschmar/ldap-cli/internal/tui"
	"github.com/ericschmar/ldap-cli/internal/version"
)

func main() {
	var (
		configPath  = flag.String("config", "", "Path to configuration file")
		host        = flag.String("host", "", "LDAP server host")
		port        = flag.Int("port", 0, "LDAP server port")
		baseDN      = flag.String("base-dn", "", "Base DN for LDAP operations")
		useSSL      = flag.Bool("ssl", false, "Use SSL/LDAPS")
		useTLS      = flag.Bool("tls", false, "Use StartTLS")
		bindUser    = flag.String("user", "", "Bind user DN")
		bindPass    = flag.String("password", "", "Bind password")
		pageSize    = flag.Uint("page-size", 0, "Number of entries per page (0 for default)")
		help        = flag.Bool("help", false, "Show help")
		showVersion = flag.Bool("version", false, "Show version information")
	)

	flag.Parse()

	if *showVersion {
		fmt.Println(version.Get().String())
		return
	}

	if *help {
		printHelp()
		return
	}

	// Load configuration
	var cfg *config.Config
	var err error

	if *configPath != "" || (*host == "" && *baseDN == "") {
		// Try to load from config file
		cfg, err = config.Load(*configPath)
		if err != nil {
			if *configPath != "" {
				log.Fatalf("Failed to load config file: %v", err)
			}
			// No config file specified and none found, use defaults
			cfg = config.Default()
		}
	} else {
		// Use command line arguments
		cfg = config.Default()
	}

	// Override config with command line arguments if provided
	if *host != "" {
		cfg.LDAP.Host = *host
	}
	if *port != 0 {
		cfg.LDAP.Port = *port
	}
	if *baseDN != "" {
		cfg.LDAP.BaseDN = *baseDN
	}
	if *useSSL {
		cfg.LDAP.UseSSL = true
	}
	if *useTLS {
		cfg.LDAP.UseTLS = true
	}
	if *bindUser != "" {
		cfg.LDAP.BindUser = *bindUser
	}
	if *bindPass != "" {
		cfg.LDAP.BindPass = *bindPass
	}
	if *pageSize != 0 {
		cfg.Pagination.PageSize = uint32(*pageSize)
	}

	// Prompt for password if not provided and user is specified
	if cfg.LDAP.BindUser != "" && cfg.LDAP.BindPass == "" {
		fmt.Print("Enter password: ")
		password, err := term.ReadPassword(int(syscall.Stdin))
		if err != nil {
			log.Fatalf("Failed to read password: %v", err)
		}
		fmt.Println() // Add newline after password input
		cfg.LDAP.BindPass = string(password)
	}

	// Validate configuration (but allow for start page testing)
	if cfg.LDAP.Host == "" || cfg.LDAP.BaseDN == "" {
		fmt.Println("Warning: LDAP host and/or Base DN not configured.")
		fmt.Println("You can configure these in the start page.")
	}

	// Try to create LDAP client
	ldapConfig := ldap.Config{
		Host:     cfg.LDAP.Host,
		Port:     cfg.LDAP.Port,
		BaseDN:   cfg.LDAP.BaseDN,
		UseSSL:   cfg.LDAP.UseSSL,
		UseTLS:   cfg.LDAP.UseTLS,
		BindUser: cfg.LDAP.BindUser,
		BindPass: cfg.LDAP.BindPass,
	}

	client, err := ldap.NewClient(ldapConfig)
	if err != nil {
		// Don't fail - allow the app to start with just the start view
		fmt.Printf("Warning: Failed to connect to LDAP server: %v\n", err)
		fmt.Println("Starting in configuration mode...")
	}
	if client != nil {
		defer client.Close()
	}

	// Create and run the TUI
	model := tui.NewModelWithPageSize(client, cfg)
	program := tea.NewProgram(model, tea.WithAltScreen(), tea.WithMouseCellMotion())

	if _, err := program.Run(); err != nil {
		log.Fatalf("Error running program: %v", err)
	}
}

func printHelp() {
	fmt.Println("LDAP CLI - Interactive LDAP Explorer")
	fmt.Println()
	fmt.Println("Usage:")
	fmt.Println("  ldap-cli [options]")
	fmt.Println()
	fmt.Println("Options:")
	fmt.Println("  -config string     Path to configuration file")
	fmt.Println("  -host string       LDAP server host")
	fmt.Println("  -port int          LDAP server port (default: 389 for LDAP, 636 for LDAPS)")
	fmt.Println("  -base-dn string    Base DN for LDAP operations")
	fmt.Println("  -ssl               Use SSL/LDAPS")
	fmt.Println("  -tls               Use StartTLS")
	fmt.Println("  -user string       Bind user DN")
	fmt.Println("  -password string   Bind password (will prompt if user provided but password not)")
	fmt.Println("  -page-size int     Number of entries per page for paginated queries (default: 50)")
	fmt.Println("  -version           Show version information")
	fmt.Println("  -help              Show this help message")
	fmt.Println()
	fmt.Println("Configuration file example:")
	fmt.Println("  ldap:")
	fmt.Println("    host: ldap.example.com")
	fmt.Println("    port: 389")
	fmt.Println("    base_dn: dc=example,dc=com")
	fmt.Println("    use_ssl: false")
	fmt.Println("    use_tls: false")
	fmt.Println("    bind_user: cn=admin,dc=example,dc=com")
	fmt.Println("    bind_pass: password")
	fmt.Println("  pagination:")
	fmt.Println("    page_size: 50")
	fmt.Println()
	fmt.Println("Navigation:")
	fmt.Println("  Tab        - Switch between views")
	fmt.Println("  1/2/3      - Switch directly to Tree/Record/Query view")
	fmt.Println("  ↑/↓        - Navigate up/down")
	fmt.Println("  →/←        - Expand/collapse in tree view")
	fmt.Println("  Enter      - Select/view record")
	fmt.Println("  q          - Quit")
}
