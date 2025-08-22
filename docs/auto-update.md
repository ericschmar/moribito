# Auto-Update Feature

The moribito application includes an optional auto-update feature that checks for newer releases on GitHub and notifies you when updates are available.

## How it Works

When enabled, the application will:

1. Start the UI immediately without waiting
2. Check the GitHub releases API in the background after startup
3. Compare the current version with the latest available release
4. Display a notification in the status bar if an update is available
5. Gracefully handle network errors without disrupting the user experience

## Usage

To enable automatic update checking, use the `--check-updates` flag:

```bash
./moribito --check-updates
```

You can combine this with other flags:

```bash
./moribito --check-updates --host ldap.example.com --base-dn dc=example,dc=com
```

## Status Bar Notifications

When an update is available, you'll see a notification in the status bar:

```
🔄 Update available: v0.1.0
```

The update notification appears with bright yellow styling and takes priority over regular status messages.

## Privacy and Network Usage

The auto-update feature:

- Only makes HTTP requests to GitHub's public API (https://api.github.com)
- Does not send any personal or usage information
- Uses a 10-second timeout for network requests
- Silently ignores network errors to avoid disrupting the user experience
- Only runs the check once per application startup
- Runs as a background task to avoid blocking UI startup

## Technical Details

- Uses GitHub's releases API endpoint: `https://api.github.com/repos/ericschmar/moribito/releases/latest`
- Compares versions using simple string comparison (works with semantic versioning)
- Development versions (`dev`) always show update notifications
- HTTP client includes appropriate User-Agent header to avoid rate limiting
- Includes a 1.5 second delay before making the network request to ensure UI starts immediately

## Disabling Updates

The feature is disabled by default. Simply omit the `--check-updates` flag to disable update checking entirely.

## Troubleshooting

If you experience issues with update checking:

1. **Network connectivity**: Ensure you have internet access and can reach GitHub
2. **Rate limiting**: GitHub may rate-limit API requests; this is handled gracefully
3. **Firewall/proxy**: Corporate firewalls may block GitHub API access
4. **Version comparison**: Development builds always show updates available

The feature is designed to fail silently and never disrupt normal operation of the application.