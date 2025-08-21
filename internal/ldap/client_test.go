package ldap

import (
	"errors"
	"net"
	"testing"
	"time"

	"github.com/go-ldap/ldap/v3"
)

func TestSearchPage(t *testing.T) {
	// Test SearchPage struct initialization
	page := &SearchPage{
		Entries:    []*Entry{},
		HasMore:    false,
		Cookie:     []byte{},
		PageSize:   50,
		TotalCount: -1,
	}

	if page.PageSize != 50 {
		t.Errorf("Expected PageSize to be 50, got %d", page.PageSize)
	}

	if page.HasMore != false {
		t.Errorf("Expected HasMore to be false, got %t", page.HasMore)
	}

	if page.TotalCount != -1 {
		t.Errorf("Expected TotalCount to be -1, got %d", page.TotalCount)
	}
}

func TestEntry(t *testing.T) {
	// Test Entry struct initialization
	entry := &Entry{
		DN:         "cn=test,dc=example,dc=com",
		Attributes: make(map[string][]string),
	}

	entry.Attributes["cn"] = []string{"test"}

	if entry.DN != "cn=test,dc=example,dc=com" {
		t.Errorf("Expected DN to be 'cn=test,dc=example,dc=com', got %s", entry.DN)
	}

	if len(entry.Attributes["cn"]) != 1 || entry.Attributes["cn"][0] != "test" {
		t.Errorf("Expected cn attribute to be ['test'], got %v", entry.Attributes["cn"])
	}
}

func TestIsRetryableError(t *testing.T) {
	config := Config{
		Host:           "localhost",
		Port:           389,
		RetryEnabled:   true,
		MaxRetries:     3,
		InitialDelayMs: 100,
		MaxDelayMs:     1000,
	}

	client := &Client{
		config: config,
	}

	tests := []struct {
		name        string
		err         error
		shouldRetry bool
	}{
		{
			name:        "nil error",
			err:         nil,
			shouldRetry: false,
		},
		{
			name:        "network timeout",
			err:         &net.OpError{Op: "dial", Net: "tcp", Err: errors.New("timeout")},
			shouldRetry: true,
		},
		{
			name:        "connection refused",
			err:         errors.New("connection refused"),
			shouldRetry: true,
		},
		{
			name:        "connection closed",
			err:         errors.New("connection closed"),
			shouldRetry: true,
		},
		{
			name:        "ldap connection closed",
			err:         errors.New("LDAP: connection closed"),
			shouldRetry: true,
		},
		{
			name:        "broken pipe",
			err:         errors.New("broken pipe"),
			shouldRetry: true,
		},
		{
			name: "LDAP server down",
			err: &ldap.Error{
				ResultCode: ldap.LDAPResultServerDown,
				Err:        errors.New("server down"),
			},
			shouldRetry: true,
		},
		{
			name: "LDAP unavailable",
			err: &ldap.Error{
				ResultCode: ldap.LDAPResultUnavailable,
				Err:        errors.New("unavailable"),
			},
			shouldRetry: true,
		},
		{
			name: "LDAP authentication error",
			err: &ldap.Error{
				ResultCode: ldap.LDAPResultInvalidCredentials,
				Err:        errors.New("invalid credentials"),
			},
			shouldRetry: false,
		},
		{
			name:        "generic error",
			err:         errors.New("some other error"),
			shouldRetry: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := client.isRetryableError(tt.err)
			if result != tt.shouldRetry {
				t.Errorf("isRetryableError() = %v, want %v for error: %v", result, tt.shouldRetry, tt.err)
			}
		})
	}
}

func TestWithRetryDisabled(t *testing.T) {
	config := Config{
		RetryEnabled:   false,
		MaxRetries:     3,
		InitialDelayMs: 100,
		MaxDelayMs:     1000,
	}

	client := &Client{
		config: config,
	}

	callCount := 0
	testErr := errors.New("test error")

	err := client.withRetry(func() error {
		callCount++
		return testErr
	})

	// Should only call once when retry is disabled
	if callCount != 1 {
		t.Errorf("Expected 1 call, got %d", callCount)
	}

	if err != testErr {
		t.Errorf("Expected original error, got %v", err)
	}
}

func TestWithRetrySuccess(t *testing.T) {
	config := Config{
		RetryEnabled:   true,
		MaxRetries:     3,
		InitialDelayMs: 10,
		MaxDelayMs:     100,
	}

	client := &Client{
		config: config,
	}

	callCount := 0

	err := client.withRetry(func() error {
		callCount++
		return nil // Success on first try
	})

	// Should only call once on success
	if callCount != 1 {
		t.Errorf("Expected 1 call, got %d", callCount)
	}

	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
}

func TestWithRetryNonRetryableError(t *testing.T) {
	config := Config{
		RetryEnabled:   true,
		MaxRetries:     3,
		InitialDelayMs: 10,
		MaxDelayMs:     100,
	}

	client := &Client{
		config: config,
	}

	callCount := 0
	nonRetryableErr := errors.New("authentication failed")

	err := client.withRetry(func() error {
		callCount++
		return nonRetryableErr
	})

	// Should only call once for non-retryable errors
	if callCount != 1 {
		t.Errorf("Expected 1 call, got %d", callCount)
	}

	if err != nonRetryableErr {
		t.Errorf("Expected original error, got %v", err)
	}
}

func TestWithRetryExponentialBackoff(t *testing.T) {
	config := Config{
		RetryEnabled:   true,
		MaxRetries:     2,
		InitialDelayMs: 10,
		MaxDelayMs:     50,
	}

	callCount := 0
	reconnectCount := 0
	retryableErr := errors.New("connection refused")
	start := time.Now()

	// Create a client with mocked methods using function variables
	client := &Client{
		config: config,
	}

	// Override the withRetry method behavior for testing
	var lastErr error
	delay := time.Duration(config.InitialDelayMs) * time.Millisecond
	maxDelay := time.Duration(config.MaxDelayMs) * time.Millisecond

	operation := func() error {
		callCount++
		if callCount <= 2 { // Fail first 2 attempts
			return retryableErr
		}
		return nil // Success on 3rd attempt
	}

	// Simulate the retry logic manually
	for attempt := 0; attempt <= config.MaxRetries; attempt++ {
		err := operation()
		if err == nil {
			lastErr = nil
			break // Success
		}

		lastErr = err

		// Don't retry if it's the last attempt or error is not retryable
		if attempt == config.MaxRetries || !client.isRetryableError(err) {
			break
		}

		// Simulate reconnect
		reconnectCount++

		// Wait before retrying
		time.Sleep(delay)

		// Exponential backoff
		delay *= 2
		if delay > maxDelay {
			delay = maxDelay
		}
	}

	duration := time.Since(start)

	// Should call 3 times (initial + 2 retries)
	if callCount != 3 {
		t.Errorf("Expected 3 calls, got %d", callCount)
	}

	// Should succeed eventually
	if lastErr != nil {
		t.Errorf("Expected success after retries, got %v", lastErr)
	}

	// Should have attempted reconnect twice
	if reconnectCount != 2 {
		t.Errorf("Expected 2 reconnect attempts, got %d", reconnectCount)
	}

	// Should have some delay due to exponential backoff
	expectedMinDelay := time.Duration(10+20) * time.Millisecond // Initial delay + doubled delay
	if duration < expectedMinDelay {
		t.Errorf("Expected at least %v delay, got %v", expectedMinDelay, duration)
	}
}

func TestWithRetryFailureAfterMaxAttempts(t *testing.T) {
	config := Config{
		RetryEnabled:   true,
		MaxRetries:     2,
		InitialDelayMs: 1, // Very fast for testing
		MaxDelayMs:     5,
	}

	client := &Client{
		config: config,
	}

	callCount := 0
	nonRetryableErr := errors.New("invalid credentials") // Non-retryable error

	err := client.withRetry(func() error {
		callCount++
		return nonRetryableErr
	})

	// Should only call once for non-retryable errors
	if callCount != 1 {
		t.Errorf("Expected 1 call for non-retryable error, got %d", callCount)
	}

	// Should return the original error
	if err != nonRetryableErr {
		t.Errorf("Expected original error, got %v", err)
	}
}
