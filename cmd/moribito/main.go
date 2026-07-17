package main

import (
	"flag"
	"fmt"
	"log"

	"github.com/charmbracelet/bubbletea"

	"github.com/ericschmar/moribito/internal/config"
	"github.com/ericschmar/moribito/internal/debug"
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
		debugLog     = flag.String("debug", "", "Write debug log to this file path")

		// SSH tunnel flags
		sshHost          = flag.String("ssh-host", "", "SSH tunnel host")
		sshPort          = flag.Int("ssh-port", 0, "SSH tunnel port (default: 22)")
		sshUser          = flag.String("ssh-user", "", "SSH tunnel user")
		sshAuthMethod    = flag.String("ssh-auth", "", "SSH auth method: password, key, or agent")
		sshPassword      = flag.String("ssh-password", "", "SSH tunnel password")
		sshKeyFile       = flag.String("ssh-key", "", "SSH private key file path")
		sshKeyPassphrase = flag.String("ssh-key-passphrase", "", "SSH private key passphrase")

		sshIgnoreHostKey = flag.Bool("ssh-ignore-host-key", false, "Skip SSH host key verification (insecure)")
	)

	flag.Parse()

	if *debugLog != "" {
		if err := debug.Enable(*debugLog); err != nil {
			log.Fatalf("Failed to open debug log %s: %v", *debugLog, err)
		}
		debug.Log("moribito starting")
	}

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
		cfg, actualConfigPath, err = config.Load(*configPath)
		if err != nil {
			if *configPath != "" {
				log.Fatalf("Failed to load config file: %v", err)
			}
			// No config file specified and none found, use defaults
			cfg = config.Default()
			actualConfigPath = config.GetDefaultConfigPath()
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

	// Apply SSH tunnel overrides. A non-empty --ssh-host implicitly enables the tunnel.
	if *sshHost != "" {
		cfg.LDAP.SSHTunnel.Enabled = true
		cfg.LDAP.SSHTunnel.Host = *sshHost
	}
	if *sshPort != 0 {
		cfg.LDAP.SSHTunnel.Port = *sshPort
	}
	if *sshUser != "" {
		cfg.LDAP.SSHTunnel.User = *sshUser
	}
	if *sshAuthMethod != "" {
		cfg.LDAP.SSHTunnel.AuthMethod = *sshAuthMethod
	}
	if *sshPassword != "" {
		cfg.LDAP.SSHTunnel.Password = *sshPassword
	}
	if *sshKeyFile != "" {
		cfg.LDAP.SSHTunnel.KeyFile = *sshKeyFile
	}
	if *sshKeyPassphrase != "" {
		cfg.LDAP.SSHTunnel.KeyPassphrase = *sshKeyPassphrase
	}
	if *sshIgnoreHostKey {
		cfg.LDAP.SSHTunnel.InsecureIgnoreHostKey = true
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
	fmt.Println("  -debug string      Write debug log to this file (e.g. -debug /tmp/moribito.log)")
	fmt.Println("  -version           Show version information")
	fmt.Println("  -help              Show this help message")
	fmt.Println()
	fmt.Println("SSH Tunnel Options:")
	fmt.Println("  -ssh-host string           SSH tunnel host (also enables the tunnel)")
	fmt.Println("  -ssh-port int              SSH tunnel port (default: 22)")
	fmt.Println("  -ssh-user string           SSH tunnel user")
	fmt.Println("  -ssh-auth string           SSH auth method: password, key, or agent")
	fmt.Println("  -ssh-password string       SSH tunnel password")
	fmt.Println("  -ssh-key string            SSH private key file path")
	fmt.Println("  -ssh-key-passphrase string SSH private key passphrase")
	fmt.Println("  -ssh-ignore-host-key       Skip SSH host key verification (insecure)")
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
