# Moribito Project Architecture

Project-specific patterns, architecture, and conventions for the Moribito LDAP Browser.

---

## Project Structure

```
moribito-rs/
├── crates/
│   └── gui/
│       ├── src/
│       │   ├── main.rs              # Application entry point
│       │   ├── app.rs               # Main app state
│       │   ├── views/               # Main view components
│       │   │   ├── start_view.rs    # Start/connection screen
│       │   │   └── main_view.rs     # Main browser view
│       │   └── components/          # Reusable UI components
│       ├── assets/
│       │   └── themes/              # Theme JSON files
│       │       └── gruvbox-dark.json
│       └── Cargo.toml
```

---

## Application State

### SharedAppState Pattern

The app uses `Arc<RwLock<AppState>>` for global state:

```rust
use std::sync::{Arc, RwLock};

pub type SharedAppState = Arc<RwLock<AppState>>;

#[derive(Default)]
pub struct AppState {
    pub connection_status: ConnectionStatus,
    pub current_user: Option<String>,
    pub ldap_connection: Option<LdapConnection>,
    // ... other shared state
}

impl AppState {
    pub fn new() -> Self {
        Self::default()
    }
}
```

**Usage Pattern**:
```rust
// Create shared state
let app_state = Arc::new(RwLock::new(AppState::new()));

// Pass to views
let view = cx.new(|cx| {
    StartView::new(app_state.clone(), window, cx)
});

// Read state
let state = app_state.read().unwrap();
if state.connection_status == ConnectionStatus::Connected {
    // ...
}

// Update state
let mut state = app_state.write().unwrap();
state.current_user = Some("admin".to_string());
```

**Why Arc<RwLock> instead of Entity?**:
- Shares state across GPUI boundary
- Works with external libraries (LDAP client)
- Thread-safe for async operations
- Familiar Rust pattern

---

## View Architecture

### Main Views

**StartView** - Connection/Login Screen:
```rust
pub struct StartView {
    app_state: SharedAppState,
    host_input: Entity<InputState>,
    port_input: Entity<InputState>,
    username_input: Entity<InputState>,
    password_input: Entity<InputState>,
}

impl StartView {
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        // Initialize inputs...
        Self {
            app_state,
            host_input,
            port_input,
            username_input,
            password_input,
        }
    }
    
    fn connect(&mut self, window: &mut Window, cx: &mut App) {
        // Get values
        let host = self.host_input.read(cx).text();
        let port = self.port_input.read(cx).text();
        let username = self.username_input.read(cx).text();
        let password = self.password_input.read(cx).text();
        
        // Update app state
        let mut state = self.app_state.write().unwrap();
        state.connection_status = ConnectionStatus::Connecting;
        
        // Perform connection (spawn task)
        // ...
    }
}
```

**MainView** - Browser/Explorer Screen:
```rust
pub struct MainView {
    app_state: SharedAppState,
    tree_state: Entity<TreeState>,
    table_state: Entity<TableState>,
}

impl MainView {
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        // Initialize tree and table...
        Self {
            app_state,
            tree_state,
            table_state,
        }
    }
}
```

### View Transitions

**Pattern for switching views**:
```rust
// In StartView after successful connection
fn on_connect_success(&mut self, window: &mut Window, cx: &mut App) {
    // Update state
    let mut state = self.app_state.write().unwrap();
    state.connection_status = ConnectionStatus::Connected;
    drop(state); // Release lock
    
    // Open new window with MainView
    cx.open_window(Default::default(), |window, cx| {
        cx.new(|cx| MainView::new(self.app_state.clone(), window, cx))
    });
    
    // Close current window
    window.close();
}
```

---

## Component Patterns

### Reusable Components

Store reusable components in `src/components/`:

```rust
// src/components/connection_status.rs
pub struct ConnectionStatus {
    app_state: SharedAppState,
}

impl ConnectionStatus {
    pub fn new(app_state: SharedAppState) -> Self {
        Self { app_state }
    }
}

impl Render for ConnectionStatus {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();
        let status = self.app_state.read().unwrap().connection_status.clone();
        
        let (icon, color, text) = match status {
            ConnectionStatus::Connected => (IconName::Check, theme.success, "Connected"),
            ConnectionStatus::Disconnected => (IconName::X, theme.danger, "Disconnected"),
            ConnectionStatus::Connecting => (IconName::Loader, theme.warning, "Connecting..."),
        };
        
        h_flex()
            .gap_2()
            .items_center()
            .child(Icon::new(icon).text_color(color))
            .child(text::text(text).text_color(color))
    }
}
```

---

## Theme Management

### Loading Custom Themes

```rust
use gpui_component::theme::Theme;
use serde::Deserialize;

#[derive(Deserialize)]
struct ThemeFile {
    name: String,
    background: String,
    foreground: String,
    // ... other colors
}

fn load_theme(path: &str) -> Result<Theme, Box<dyn std::error::Error>> {
    let content = std::fs::read_to_string(path)?;
    let theme_file: ThemeFile = serde_json::from_str(&content)?;
    
    // Convert to Theme
    Ok(Theme {
        name: theme_file.name,
        background: parse_color(&theme_file.background),
        foreground: parse_color(&theme_file.foreground),
        // ... other fields
    })
}

fn parse_color(hex: &str) -> Rgba {
    // Parse hex color string
    let hex = hex.trim_start_matches('#');
    let r = u8::from_str_radix(&hex[0..2], 16).unwrap();
    let g = u8::from_str_radix(&hex[2..4], 16).unwrap();
    let b = u8::from_str_radix(&hex[4..6], 16).unwrap();
    rgba(r, g, b, 1.0)
}
```

**Gruvbox Dark Theme** (Current):
```json
{
  "name": "Gruvbox Dark",
  "background": "#1d2021",
  "foreground": "#ebdbb2",
  "primary": "#458588",
  "secondary": "#504945",
  "success": "#98971a",
  "warning": "#d79921",
  "danger": "#cc241d",
  "info": "#689d6a",
  "border": "#504945",
  "ring": "#458588",
  "muted": "#282828",
  "muted_foreground": "#a89984"
}
```

---

## LDAP Integration

### Connection Management

```rust
use ldap3::{LdapConn, LdapError};
use std::sync::Arc;

pub struct LdapConnection {
    conn: Arc<LdapConn>,
    base_dn: String,
}

impl LdapConnection {
    pub async fn connect(
        url: &str,
        bind_dn: &str,
        password: &str,
    ) -> Result<Self, LdapError> {
        let conn = LdapConn::new(url)?;
        conn.simple_bind(bind_dn, password)?.success()?;
        
        Ok(Self {
            conn: Arc::new(conn),
            base_dn: extract_base_dn(bind_dn),
        })
    }
    
    pub async fn search(
        &self,
        filter: &str,
        attrs: &[&str],
    ) -> Result<Vec<LdapEntry>, LdapError> {
        let (rs, _res) = self.conn
            .search(&self.base_dn, Scope::Subtree, filter, attrs)?
            .success()?;
        
        Ok(rs)
    }
}
```

### Async LDAP Operations

```rust
use tokio::runtime::Runtime;

impl StartView {
    fn connect(&mut self, window: &mut Window, cx: &mut App) {
        let host = self.host_input.read(cx).text();
        let username = self.username_input.read(cx).text();
        let password = self.password_input.read(cx).text();
        
        let app_state = self.app_state.clone();
        
        // Update state to "connecting"
        {
            let mut state = app_state.write().unwrap();
            state.connection_status = ConnectionStatus::Connecting;
        }
        
        // Spawn background task
        tokio::spawn(async move {
            match LdapConnection::connect(&host, &username, &password).await {
                Ok(conn) => {
                    let mut state = app_state.write().unwrap();
                    state.connection_status = ConnectionStatus::Connected;
                    state.ldap_connection = Some(conn);
                }
                Err(err) => {
                    let mut state = app_state.write().unwrap();
                    state.connection_status = ConnectionStatus::Error(err.to_string());
                }
            }
        });
    }
}
```

---

## Error Handling

### Application-Level Errors

```rust
use thiserror::Error;

#[derive(Error, Debug, Clone)]
pub enum AppError {
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Authentication failed")]
    AuthenticationFailed,
    
    #[error("LDAP query error: {0}")]
    QueryError(String),
    
    #[error("Invalid configuration: {0}")]
    ConfigError(String),
}

// Convert from LDAP errors
impl From<LdapError> for AppError {
    fn from(err: LdapError) -> Self {
        AppError::QueryError(err.to_string())
    }
}
```

### Error Display in UI

```rust
pub struct ErrorBanner {
    error: Option<AppError>,
}

impl Render for ErrorBanner {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();
        
        div()
            .when_some(self.error.as_ref(), |this, error| {
                this.child(
                    h_flex()
                        .gap_2()
                        .p_3()
                        .bg(theme.danger)
                        .text_color(rgb(0xffffff))
                        .items_center()
                        .child(Icon::new(IconName::AlertCircle))
                        .child(text::text(error.to_string()))
                        .child(
                            Button::new("dismiss-error")
                                .icon(IconName::X)
                                .ghost()
                                .on_click(|_, _, _| {
                                    // Dismiss error
                                })
                        )
                )
            })
    }
}
```

---

## Data Flow

### Typical Data Flow Pattern

1. **User Action** → Button click, input change
2. **Update State** → Modify `SharedAppState` or `Entity` state
3. **Async Operation** (if needed) → Spawn tokio task for LDAP/network
4. **Update UI State** → Update reactive entities
5. **Re-render** → GPUI automatically re-renders affected components

```rust
// Example: Load LDAP entries
impl MainView {
    fn load_entries(&mut self, dn: &str, cx: &mut App) {
        let app_state = self.app_state.clone();
        let table_state = self.table_state.clone();
        let dn = dn.to_string();
        
        // 1. User Action (method called from button click)
        
        // 2. Update State (show loading)
        {
            let mut state = app_state.write().unwrap();
            state.loading = true;
        }
        
        // 3. Async Operation
        tokio::spawn(async move {
            let entries = {
                let state = app_state.read().unwrap();
                let conn = state.ldap_connection.as_ref().unwrap();
                conn.search(&dn, "(objectClass=*)", &["*"]).await
            };
            
            match entries {
                Ok(data) => {
                    // 4. Update UI State
                    let delegate = LdapTableDelegate::new(data);
                    table_state.update(cx, |state, cx| {
                        *state = TableState::new(delegate, window, cx);
                    });
                    
                    let mut state = app_state.write().unwrap();
                    state.loading = false;
                }
                Err(err) => {
                    let mut state = app_state.write().unwrap();
                    state.loading = false;
                    state.error = Some(err.into());
                }
            }
        });
        
        // 5. Re-render happens automatically
    }
}
```

---

## Common Imports

### Standard View Imports

```rust
use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    button::Button,
    h_flex, v_flex,
    input::{Input, InputState},
    text,
    theme::ActiveTheme,
    Icon, IconName,
    Sizable,
};
use std::sync::{Arc, RwLock};

use crate::app::{AppState, SharedAppState};
```

### Component-Specific Imports

```rust
// For tables
use gpui_component::table::{Column, Table, TableDelegate, TableState};
use once_cell::sync::Lazy;

// For trees
use gpui_component::tree::{tree, TreeItem, TreeState};
use gpui_component::list::ListItem;

// For LDAP
use ldap3::{LdapConn, LdapError, Scope, SearchEntry};
```

---

## Conventions

### Naming Conventions

- **Views**: `StartView`, `MainView`, `SettingsView` (PascalCase + View suffix)
- **Components**: `ConnectionStatus`, `ErrorBanner` (PascalCase, no suffix)
- **State**: `AppState`, `ViewState` (PascalCase + State suffix)
- **Entities**: `table_state`, `tree_state`, `input_state` (snake_case + _state suffix)

### File Organization

- **One view per file**: `views/start_view.rs`, `views/main_view.rs`
- **Group related components**: `components/ldap/`, `components/ui/`
- **Shared types**: `types.rs` or `models.rs`
- **State management**: `app.rs` or `state.rs`

### Code Style

- **Use derive macros**: `#[derive(Debug, Clone)]` when possible
- **Prefer `impl` over trait objects**: More performance, better errors
- **Document public APIs**: Use `///` doc comments
- **Handle errors explicitly**: No silent `.unwrap()` in production code
- **Clone entities cheaply**: Entity clone is just a reference

---

## Testing Patterns

### Unit Tests

```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_app_state_default() {
        let state = AppState::default();
        assert_eq!(state.connection_status, ConnectionStatus::Disconnected);
        assert!(state.current_user.is_none());
    }
    
    #[test]
    fn test_shared_state() {
        let shared = Arc::new(RwLock::new(AppState::default()));
        
        {
            let mut state = shared.write().unwrap();
            state.current_user = Some("test".to_string());
        }
        
        let state = shared.read().unwrap();
        assert_eq!(state.current_user, Some("test".to_string()));
    }
}
```

### Integration Tests

```rust
// tests/ldap_integration.rs
use moribito::ldap::LdapConnection;

#[tokio::test]
async fn test_ldap_connection() {
    // Requires running LDAP server
    let result = LdapConnection::connect(
        "ldap://localhost:389",
        "cn=admin,dc=example,dc=com",
        "password"
    ).await;
    
    assert!(result.is_ok());
}
```

---

## Dependencies

### Current Dependencies

```toml
[dependencies]
gpui = "0.2.2"
gpui-component = "0.4.0"
gpui-component-assets = "0.4.0"
once_cell = "1.19"

# LDAP
ldap3 = "0.11"

# Async runtime
tokio = { version = "1.0", features = ["full"] }

# Error handling
thiserror = "1.0"
anyhow = "1.0"

# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
```

---

## Performance Considerations

### State Updates

- **Minimize lock duration**: Acquire lock, update, release quickly
- **Avoid nested locks**: Can cause deadlocks
- **Clone data if holding lock long**: Don't do heavy work while locked

```rust
// Bad: Heavy work while holding lock
let state = app_state.read().unwrap();
let processed = heavy_processing(&state.data); // Lock held during processing!

// Good: Clone data, release lock, then process
let data = {
    let state = app_state.read().unwrap();
    state.data.clone()
}; // Lock released here
let processed = heavy_processing(&data);
```

### Rendering

- **Cache computed values**: Don't recompute on every render
- **Minimize entity reads**: Read once, use multiple times
- **Use references when possible**: Avoid unnecessary clones

### Async Operations

- **Spawn long-running tasks**: Don't block UI
- **Use channels for updates**: Communicate back to main thread
- **Handle cancellation**: Clean up if user navigates away

---

## Future Architecture Considerations

### Potential Improvements

1. **Separate business logic from UI**
   - Create `services/` directory for LDAP, auth, etc.
   - Keep views focused on rendering

2. **Event bus pattern**
   - Central event dispatcher
   - Decouple components

3. **State machine for connection**
   - Explicit states and transitions
   - Better error handling

4. **Configuration management**
   - Save/load connection profiles
   - User preferences

5. **Logging and telemetry**
   - Structured logging
   - Error reporting
