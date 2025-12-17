# Start Screen Technical Specification

## Technical context
- The UI is powered by the `moribito-rs` GPUI/GPUI-component stack, while LDAP connectivity comes from the `moribito-core` crate (which exposes `Config`, `ConnectionSettings`, and `LdapClient`).
- Global state is managed via `SharedAppState`, which already tracks saved connections, the active LDAP client, and the navigation state for the browser view.
- The current entry point only instantiates `BrowserView`, so we will need a small coordination layer that chooses between showing the start screen and the browser view depending on `SharedAppState::is_connected`.

## Implementation approach
1. Add a `StartView` that renders the requested hero layout: a left column with the "Moribito" title, description, and call-to-action, and a right column that surfaces the first three saved connections (pulled from `SharedAppState`).  Each connection tile will be clickable; when the user selects one we will build a `ConnectionSettings`, call `LdapClient::connect`/`bind`, update `SharedAppState` to hold the client, base DN, and selected connection, and display transient status or error text on the start view.
2. Introduce an `AppView` (or similar root view) which holds the shared state and both the start and browser views.  Its `render(...)` will inspect `SharedAppState::is_connected` and render `StartView` when disconnected and `BrowserView` when connected, lazily instantiating the browser view so the start screen is what the user sees first.
3. Update `main.rs` to build the new root view instead of directly constructing `BrowserView`, and adjust `views/mod.rs` to export the newly created modules.
4. Guard the tree initialization path in `BrowserView` so that we do not attempt to load a tree when the current base DN is empty (this is the source of the "Search failed: no such object. dn: \"\"" error seen when the app auto-runs a search on startup without a base DN).

## Source code structure changes
- `crates/gui/src/views/start_view.rs`: new file containing `StartView`, connection-tile rendering, error handling, and helpers to spin up the config window when needed.
- `crates/gui/src/views/app_view.rs`: new file rendering either the start screen or the browser view and owning the per-view `Entity` handles (also handles opening the config window from the start screen if required).
- `crates/gui/src/views/mod.rs`: export the new modules.
- `crates/gui/src/main.rs`: instantiate `AppView` instead of `BrowserView`/`Root`, ensuring the window boots on the start screen.
- `crates/gui/src/browser_view.rs`: add a base DN guard in `check_and_initialize` (or `initialize`) so tree loading is skipped when the base DN string is empty.
- `crates/gui/src/app_state.rs`: expose helper functions (or extend `SharedAppState`) to mark the selected connection, update the active client with its base DN, and surface error/status messages for the start screen to display.

## Data model / API changes
- `SharedAppState` will expose a small helper (e.g., `fn set_selected_connection(&mut self, name: impl Into<String>, client: LdapClient, base_dn: String)`) to centralize what happens once a connection is successful (client stored, `current_base_dn` set, `selected_connection` recorded, status message updated, `is_connected` flipped).
- The start view will own a short-lived error/status string plus a flag for whether a connection attempt is in progress so the UI can display feedback while the connection is being established.

## Verification approach
- Run `cargo fmt` at the repo root to ensure new files obey formatting rules.
- Run `cargo test --all` to make sure the core and gui crates still compile and their tests pass.
- Manually verify (through smoke testing once running, if possible) that the start screen shows first, clicking a saved connection attempts to connect and transitions to the browser view, and the previous "Search failed" message no longer appears when the base DN is empty.