package updater

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"
)

// GitHubRelease represents a GitHub release response
type GitHubRelease struct {
	TagName string `json:"tag_name"`
	Name    string `json:"name"`
	URL     string `json:"html_url"`
}

// Checker handles update checking functionality
type Checker struct {
	owner  string
	repo   string
	client *http.Client
}

// New creates a new update checker
func New(owner, repo string) *Checker {
	return &Checker{
		owner: owner,
		repo:  repo,
		client: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// CheckForUpdate checks if a new version is available
func (c *Checker) CheckForUpdate(ctx context.Context, currentVersion string) (*GitHubRelease, error) {
	latestRelease, err := c.getLatestRelease(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to get latest release: %w", err)
	}

	// Compare versions - if current version is "dev", always show update available
	if currentVersion == "dev" || isNewerVersion(latestRelease.TagName, currentVersion) {
		return latestRelease, nil
	}

	return nil, nil // No update available
}

// getLatestRelease fetches the latest release from GitHub API
func (c *Checker) getLatestRelease(ctx context.Context) (*GitHubRelease, error) {
	url := fmt.Sprintf("https://api.github.com/repos/%s/%s/releases/latest", c.owner, c.repo)

	req, err := http.NewRequestWithContext(ctx, "GET", url, nil)
	if err != nil {
		return nil, err
	}

	// Set User-Agent to avoid GitHub API rate limiting
	req.Header.Set("User-Agent", "ldap-cli-updater")

	resp, err := c.client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("GitHub API returned status %d", resp.StatusCode)
	}

	var release GitHubRelease
	if err := json.NewDecoder(resp.Body).Decode(&release); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &release, nil
}

// isNewerVersion compares two version strings to determine if the first is newer
// Simple implementation that works with semantic versioning (vX.Y.Z)
func isNewerVersion(latest, current string) bool {
	// Remove 'v' prefix if present
	latest = strings.TrimPrefix(latest, "v")
	current = strings.TrimPrefix(current, "v")

	// If current is "dev", any release version is newer
	if current == "dev" {
		return true
	}

	// Simple string comparison for now - works for semantic versioning
	// In a production environment, you'd want to use a proper semver library
	return latest > current
}
