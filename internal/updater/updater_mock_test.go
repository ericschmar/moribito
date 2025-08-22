package updater

import (
	"context"
	"io"
	"net/http"
	"strings"
	"testing"
)

// mockHTTPClient is a mock HTTP client for testing
type mockHTTPClient struct {
	response string
	status   int
	err      error
}

func (m *mockHTTPClient) Do(req *http.Request) (*http.Response, error) {
	if m.err != nil {
		return nil, m.err
	}

	return &http.Response{
		StatusCode: m.status,
		Body:       io.NopCloser(strings.NewReader(m.response)),
	}, nil
}

func TestCheckForUpdate_MockSuccess(t *testing.T) {
	// Mock a successful GitHub API response
	mockResponse := `{
		"tag_name": "v0.1.0",
		"name": "Release v0.1.0",
		"html_url": "https://github.com/ericschmar/moribito/releases/tag/v0.1.0"
	}`

	checker := &Checker{
		owner: "ericschmar",
		repo:  "moribito",
		client: &http.Client{
			Transport: &mockTransport{
				response: mockResponse,
				status:   200,
			},
		},
	}

	ctx := context.Background()
	release, err := checker.CheckForUpdate(ctx, "dev")

	if err != nil {
		t.Fatalf("Expected no error, got: %v", err)
	}

	if release == nil {
		t.Fatal("Expected update available for dev version")
	}

	if release.TagName != "v0.1.0" {
		t.Errorf("Expected tag_name 'v0.1.0', got '%s'", release.TagName)
	}

	if release.Name != "Release v0.1.0" {
		t.Errorf("Expected name 'Release v0.1.0', got '%s'", release.Name)
	}

	if !strings.Contains(release.URL, "releases/tag/v0.1.0") {
		t.Errorf("Expected URL to contain 'releases/tag/v0.1.0', got '%s'", release.URL)
	}
}

func TestCheckForUpdate_NoUpdateNeeded(t *testing.T) {
	// Mock a successful GitHub API response with same version
	mockResponse := `{
		"tag_name": "v0.1.0",
		"name": "Release v0.1.0",
		"html_url": "https://github.com/ericschmar/moribito/releases/tag/v0.1.0"
	}`

	checker := &Checker{
		owner: "ericschmar",
		repo:  "moribito",
		client: &http.Client{
			Transport: &mockTransport{
				response: mockResponse,
				status:   200,
			},
		},
	}

	ctx := context.Background()
	release, err := checker.CheckForUpdate(ctx, "v0.1.0")

	if err != nil {
		t.Fatalf("Expected no error, got: %v", err)
	}

	if release != nil {
		t.Error("Expected no update available for same version")
	}
}

// mockTransport implements http.RoundTripper for testing
type mockTransport struct {
	response string
	status   int
	err      error
}

func (m *mockTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if m.err != nil {
		return nil, m.err
	}

	return &http.Response{
		StatusCode: m.status,
		Body:       io.NopCloser(strings.NewReader(m.response)),
		Request:    req,
	}, nil
}
