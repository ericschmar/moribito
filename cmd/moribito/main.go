package main

import (
	"flag"
	"fmt"
	"log"

	"github.com/charmbracelet/bubbletea"

	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/ldap"
	"github.com/ericschmar/moribito/internal/tui"
	"github.com/ericschmar/moribito/internal/version"
)

func main() {
	var (
		configPath   = flag.String("config", "", "Path to configuration file")
		host         = flag.String("host", "", "LDAP server host")
		port         = flag.Int("port", 0, "LDAP server port")
		baseDN       = flag.String("base-dn", "", "Base DN for LDAP operations")
		useSSL       = flag.Bool("ssl", false, "Use SSL/LDAPS")
		useTLS       = flag.Bool("tls", false, "Use StartTLS")
		bindUser     = flag.String("user", "", "Bind user DN")
		bindPass     = flag.String("password", "", "Bind password")
		pageSize     = flag.Uint("page-size", 0, "Number of entries per page (0 for default)")
		help         = flag.Bool("help", false, "Show help")
		showVersion  = flag.Bool("version", false, "Show version information")
		checkUpdates = flag.Bool("check-updates", false, "Enable automatic update checking")
		createConfig = flag.Bool("create-config", false, "Create default configuration file in OS-appropriate location")
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

	if *createConfig {
		if err := config.CreateDefaultConfig(); err != nil {
			log.Fatalf("Failed to create config: %v", err)
		}
		fmt.Printf("Configuration file created at: %s\n", config.GetDefaultConfigPath())
		fmt.Println("Please edit the file with your LDAP server details.")
		return
	}

	// Load configuration
	var cfg *config.Config
	var err error
	var actualConfigPath string

	if *configPath != "" || (*host == "" && *baseDN == "") {
		// Try to load from config file
		cfg, err = config.Load(*configPath)
		if err != nil {
			if *configPath != "" {
				log.Fatalf("Failed to load config file: %v", err)
			}
			// No config file specified and none found, use defaults
			cfg = config.Default()
			actualConfigPath = config.GetDefaultConfigPath()
		} else {
			// Successfully loaded config, determine actual path used
			if *configPath != "" {
				actualConfigPath = *configPath
			} else {
				// Config was found by searching, need to determine which path was used
				actualConfigPath = config.GetDefaultConfigPath()
			}
		}
	} else {
		// Use command line arguments
		cfg = config.Default()
		actualConfigPath = config.GetDefaultConfigPath()
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

	// Get the active connection for validation display
	activeConn := cfg.GetActiveConnection()

	// Note: Password prompting is now handled in the start view when connecting

	// Validate configuration (but allow for start page testing)
	if activeConn.Host == "" || activeConn.BaseDN == "" {
		fmt.Println("Warning: LDAP host and/or Base DN not configured.")
		fmt.Println("You can configure these in the start page.")
	}

	// Skip immediate LDAP connection - user will connect from start view
	var client *ldap.Client = nil
	fmt.Println("Starting in configuration mode - use the start screen to connect to LDAP...")

	// Create and run the TUI
	model := tui.NewModelWithUpdateCheckAndConfigPath(client, cfg, *checkUpdates, actualConfigPath)
	program := tea.NewProgram(model, tea.WithAltScreen(), tea.WithMouseCellMotion())

	if _, err := program.Run(); err != nil {
		log.Fatalf("Error running program: %v", err)
	}
}

func printHelp() {
	fmt.Println("Moribito - Interactive LDAP Explorer")
	fmt.Println()
	fmt.Println("Usage:")
	fmt.Println("  moribito [options]")
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
	fmt.Println("  -check-updates     Enable automatic update checking")
	fmt.Println("  -create-config     Create default configuration file in OS-appropriate location")
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
	fmt.Println("  retry:")
	fmt.Println("    enabled: true")
	fmt.Println("    max_attempts: 3")
	fmt.Println("    initial_delay_ms: 500")
	fmt.Println("    max_delay_ms: 5000")
	fmt.Println()
	fmt.Println("Navigation:")
	fmt.Println("  Tab        - Switch between views")
	fmt.Println("  1/2/3      - Switch directly to Tree/Record/Query view")
	fmt.Println("  ↑/↓        - Navigate up/down")
	fmt.Println("  →/←        - Expand/collapse in tree view")
	fmt.Println("  Enter      - Select/view record")
	fmt.Println("  q          - Quit")
}
