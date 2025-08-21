package ldap

import (
	"crypto/tls"
	"errors"
	"fmt"
	"net"
	"strings"
	"time"

	"github.com/go-ldap/ldap/v3"
)

// Client wraps the LDAP connection and provides higher-level operations
type Client struct {
	conn   *ldap.Conn
	baseDN string
	config Config // Store the configuration for reconnection
}

// Config contains LDAP connection parameters
type Config struct {
	Host           string
	Port           int
	BaseDN         string
	UseSSL         bool
	UseTLS         bool
	BindUser       string
	BindPass       string
	RetryEnabled   bool
	MaxRetries     int
	InitialDelayMs int
	MaxDelayMs     int
}

// Entry represents an LDAP entry with its attributes
type Entry struct {
	DN         string
	Attributes map[string][]string
}

// SearchPage represents a page of search results with pagination info
type SearchPage struct {
	Entries    []*Entry
	HasMore    bool
	Cookie     []byte
	PageSize   uint32
	TotalCount int // -1 if unknown
}

// TreeNode represents a node in the LDAP tree
type TreeNode struct {
	DN       string
	Name     string
	Children []*TreeNode
	IsLoaded bool
}

// NewClient creates a new LDAP client
func NewClient(config Config) (*Client, error) {
	var conn *ldap.Conn
	var err error

	address := fmt.Sprintf("%s:%d", config.Host, config.Port)

	if config.UseSSL {
		conn, err = ldap.DialTLS("tcp", address, &tls.Config{InsecureSkipVerify: true})
	} else {
		conn, err = ldap.Dial("tcp", address)
	}

	if err != nil {
		return nil, fmt.Errorf("failed to connect to LDAP server: %w", err)
	}

	if config.UseTLS && !config.UseSSL {
		err = conn.StartTLS(&tls.Config{InsecureSkipVerify: true})
		if err != nil {
			conn.Close()
			return nil, fmt.Errorf("failed to start TLS: %w", err)
		}
	}

	client := &Client{
		conn:   conn,
		baseDN: config.BaseDN,
		config: config, // Store config for reconnection
	}

	// Bind with provided credentials
	if config.BindUser != "" {
		err = conn.Bind(config.BindUser, config.BindPass)
		if err != nil {
			conn.Close()
			return nil, fmt.Errorf("failed to bind: %w", err)
		}
	}

	return client, nil
}

// isRetryableError checks if an error is retryable (connection-related)
func (c *Client) isRetryableError(err error) bool {
	if err == nil {
		return false
	}

	// Check for common connection errors
	var netErr net.Error
	if errors.As(err, &netErr) {
		return true // Network errors are retryable
	}

	// Check for LDAP-specific connection errors
	if ldapErr, ok := err.(*ldap.Error); ok {
		switch ldapErr.ResultCode {
		case ldap.LDAPResultServerDown,
			ldap.LDAPResultConnectError,
			ldap.LDAPResultUnavailable,
			ldap.LDAPResultUnwillingToPerform:
			return true
		}
	}

	// Check for common error strings that indicate connection issues
	errStr := strings.ToLower(err.Error())
	retryableErrors := []string{
		"connection closed",
		"connection reset",
		"broken pipe",
		"connection refused",
		"network is unreachable",
		"timeout",
		"server down",
		"ldap: connection closed",
	}

	for _, retryErr := range retryableErrors {
		if strings.Contains(errStr, retryErr) {
			return true
		}
	}

	return false
}

// reconnect attempts to re-establish the LDAP connection
func (c *Client) reconnect() error {
	// Close existing connection if any
	if c.conn != nil {
		c.conn.Close()
	}

	// Re-establish connection using stored config
	var conn *ldap.Conn
	var err error

	address := fmt.Sprintf("%s:%d", c.config.Host, c.config.Port)

	if c.config.UseSSL {
		conn, err = ldap.DialTLS("tcp", address, &tls.Config{InsecureSkipVerify: true})
	} else {
		conn, err = ldap.Dial("tcp", address)
	}

	if err != nil {
		return fmt.Errorf("failed to reconnect to LDAP server: %w", err)
	}

	if c.config.UseTLS && !c.config.UseSSL {
		err = conn.StartTLS(&tls.Config{InsecureSkipVerify: true})
		if err != nil {
			conn.Close()
			return fmt.Errorf("failed to start TLS on reconnect: %w", err)
		}
	}

	// Re-bind with credentials if needed
	if c.config.BindUser != "" {
		err = conn.Bind(c.config.BindUser, c.config.BindPass)
		if err != nil {
			conn.Close()
			return fmt.Errorf("failed to bind on reconnect: %w", err)
		}
	}

	c.conn = conn
	return nil
}

// withRetry executes an operation with retry logic
func (c *Client) withRetry(operation func() error) error {
	if !c.config.RetryEnabled {
		return operation()
	}

	var lastErr error
	delay := time.Duration(c.config.InitialDelayMs) * time.Millisecond
	maxDelay := time.Duration(c.config.MaxDelayMs) * time.Millisecond

	for attempt := 0; attempt <= c.config.MaxRetries; attempt++ {
		err := operation()
		if err == nil {
			return nil // Success
		}

		lastErr = err

		// Don't retry if it's the last attempt or error is not retryable
		if attempt == c.config.MaxRetries || !c.isRetryableError(err) {
			break
		}

		// Try to reconnect for retryable errors
		if reconnectErr := c.reconnect(); reconnectErr != nil {
			// If reconnection fails, continue with the original error
			// but don't attempt more retries
			break
		}

		// Wait before retrying
		time.Sleep(delay)

		// Exponential backoff with jitter (double the delay, cap at maxDelay)
		delay *= 2
		if delay > maxDelay {
			delay = maxDelay
		}
	}

	return lastErr
}

// Close closes the LDAP connection
func (c *Client) Close() {
	if c.conn != nil {
		c.conn.Close()
	}
}

// Search performs an LDAP search
func (c *Client) Search(baseDN, filter string, scope int, attributes []string) ([]*Entry, error) {
	var result *ldap.SearchResult
	var entries []*Entry

	err := c.withRetry(func() error {
		searchRequest := ldap.NewSearchRequest(
			baseDN,
			scope,
			ldap.NeverDerefAliases,
			0, // No size limit
			0, // No time limit
			false,
			filter,
			attributes,
			nil,
		)

		var err error
		result, err = c.conn.Search(searchRequest)
		if err != nil {
			return fmt.Errorf("search failed: %w", err)
		}

		// Process results inside the retry function to ensure consistency
		entries = make([]*Entry, 0, len(result.Entries))
		for _, entry := range result.Entries {
			e := &Entry{
				DN:         entry.DN,
				Attributes: make(map[string][]string),
			}

			for _, attr := range entry.Attributes {
				e.Attributes[attr.Name] = attr.Values
			}

			entries = append(entries, e)
		}

		return nil
	})

	return entries, err
}

// SearchPaged performs a paginated LDAP search
func (c *Client) SearchPaged(baseDN, filter string, scope int, attributes []string, pageSize uint32, cookie []byte) (*SearchPage, error) {
	var result *ldap.SearchResult
	var searchPage *SearchPage

	err := c.withRetry(func() error {
		// Create paging control
		pagingControl := ldap.NewControlPaging(pageSize)
		if cookie != nil {
			pagingControl.SetCookie(cookie)
		}

		searchRequest := ldap.NewSearchRequest(
			baseDN,
			scope,
			ldap.NeverDerefAliases,
			0, // No size limit - controlled by paging
			0, // No time limit
			false,
			filter,
			attributes,
			[]ldap.Control{pagingControl},
		)

		var err error
		result, err = c.conn.Search(searchRequest)
		if err != nil {
			return fmt.Errorf("paged search failed: %w", err)
		}

		// Parse entries inside the retry function
		entries := make([]*Entry, 0, len(result.Entries))
		for _, entry := range result.Entries {
			e := &Entry{
				DN:         entry.DN,
				Attributes: make(map[string][]string),
			}

			for _, attr := range entry.Attributes {
				e.Attributes[attr.Name] = attr.Values
			}

			entries = append(entries, e)
		}

		// Extract paging control from response
		var nextCookie []byte
		hasMore := false

		for _, control := range result.Controls {
			if control.GetControlType() == ldap.ControlTypePaging {
				if pagingResult, ok := control.(*ldap.ControlPaging); ok {
					nextCookie = pagingResult.Cookie
					hasMore = len(nextCookie) > 0
				}
				break
			}
		}

		searchPage = &SearchPage{
			Entries:    entries,
			HasMore:    hasMore,
			Cookie:     nextCookie,
			PageSize:   pageSize,
			TotalCount: -1, // LDAP doesn't provide total count
		}

		return nil
	})

	return searchPage, err
}

// GetChildren returns immediate children of a DN
func (c *Client) GetChildren(dn string) ([]*TreeNode, error) {
	searchDN := dn
	if searchDN == "" {
		searchDN = c.baseDN
	}

	entries, err := c.Search(searchDN, "(objectClass=*)", ldap.ScopeSingleLevel, []string{"dn"})
	if err != nil {
		return nil, err
	}

	nodes := make([]*TreeNode, 0, len(entries))
	for _, entry := range entries {
		name := extractName(entry.DN, searchDN)
		node := &TreeNode{
			DN:       entry.DN,
			Name:     name,
			Children: nil,
			IsLoaded: false,
		}
		nodes = append(nodes, node)
	}

	return nodes, nil
}

// GetEntry retrieves a specific LDAP entry with all its attributes
func (c *Client) GetEntry(dn string) (*Entry, error) {
	entries, err := c.Search(dn, "(objectClass=*)", ldap.ScopeBaseObject, []string{"*", "+"})
	if err != nil {
		return nil, err
	}

	if len(entries) == 0 {
		return nil, fmt.Errorf("entry not found: %s", dn)
	}

	return entries[0], nil
}

// BuildTree builds the complete LDAP tree starting from baseDN
func (c *Client) BuildTree() (*TreeNode, error) {
	root := &TreeNode{
		DN:       c.baseDN,
		Name:     extractName(c.baseDN, ""),
		Children: nil,
		IsLoaded: false,
	}

	return root, nil
}

// LoadChildren loads children for a tree node if not already loaded
func (c *Client) LoadChildren(node *TreeNode) error {
	if node.IsLoaded {
		return nil
	}

	children, err := c.GetChildren(node.DN)
	if err != nil {
		return err
	}

	node.Children = children
	node.IsLoaded = true
	return nil
}

// CustomSearch performs a custom LDAP search with user-provided filter
func (c *Client) CustomSearch(filter string) ([]*Entry, error) {
	return c.Search(c.baseDN, filter, ldap.ScopeWholeSubtree, []string{"*"})
}

// CustomSearchPaged performs a paginated custom LDAP search with user-provided filter
func (c *Client) CustomSearchPaged(filter string, pageSize uint32, cookie []byte) (*SearchPage, error) {
	return c.SearchPaged(c.baseDN, filter, ldap.ScopeWholeSubtree, []string{"*"}, pageSize, cookie)
}

// extractName extracts the relative name from a DN
func extractName(dn, baseDN string) string {
	if baseDN != "" && strings.HasSuffix(dn, baseDN) {
		relativeDN := strings.TrimSuffix(dn, ","+baseDN)
		if relativeDN == baseDN {
			return dn // This is the base DN itself
		}
		// Extract the first component
		parts := strings.Split(relativeDN, ",")
		if len(parts) > 0 {
			return strings.TrimSpace(parts[0])
		}
	}

	// If we can't extract relative name, use the first component of the DN
	parts := strings.Split(dn, ",")
	if len(parts) > 0 {
		return strings.TrimSpace(parts[0])
	}

	return dn
}
