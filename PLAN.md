# Implementation Plan: Moribito LDAP Browser - Main Features

## 📊 Implementation Status Summary

### ✅ COMPLETED FEATURES (3/4)

1. **Connection Configuration Saving** ✅ 100% COMPLETE
   - File persistence to OS-specific paths
   - CRUD operations on saved connections
   - Auto-save on changes
   - Connect and close window on success

2. **Config View in Popup Window** ✅ 100% COMPLETE
   - ConfigView opens in separate window
   - SharedAppState integration
   - Window closes after successful connection
   - Main window shows BrowserView by default

3. **macOS Menu Bar** ✅ 95% COMPLETE
   - Native menu bar with all menus
   - Moribito, File, Edit, View, Connection, Help menus
   - All actions defined and wired
   - Action handlers in BrowserView
   - ⏳ About dialog not yet implemented

### 🟡 IN PROGRESS (1/4)

4. **Main LDAP Navigation/Viewing Interface** 🟡 60% COMPLETE
   - ✅ All components created (BrowserView, TreeView, SearchBar, EntryDetailsTable, StatusBar)
   - ✅ AppState updated with browser fields
   - ✅ Resizable split layout implemented
   - ✅ Component structure and composition complete
   - ⏳ Tree lazy loading not implemented
   - ⏳ Search functionality not implemented
   - ⏳ Entry selection and details display not wired up
   - ⏳ LDAP operations (expand, search) need implementation

---

## Overview

This plan implements 4 major features for the moribito-rs LDAP GUI application:

1. **Connection Configuration Saving** ✅ COMPLETE
2. **Main LDAP Navigation/Viewing Interface** 🟡 PARTIALLY COMPLETE
3. **Config View in Popup Window** ✅ COMPLETE
4. **macOS Menu Bar** ✅ COMPLETE

## Architecture Overview

### Current State
- Single window application with only `ConfigView`
- Config auto-saves to disk (YAML format, platform-specific paths)
- LDAP client fully implemented in core module
- Using GPUI 0.2.2 + gpui-component 0.4.0

### Target State
```
┌─────────────────────────────────────────┐
│ File Edit View Connection Help          │  ← Menu Bar
├─────────────────────────────────────────┤
│ Main Window: BrowserView                │
│  ├─ Search Bar                           │
│  ├─ Resizable Split                      │
│  │   ├─ Left: TreeView (DN hierarchy)    │
│  │   └─ Right: EntryDetailsTable         │
│  └─ Status Bar                           │
└─────────────────────────────────────────┘
                    ↓
              [⚙️ Settings]
                    ↓
        ┌─────────────────────┐
        │ Config Window/Modal │  ← Popup window
        │   ConfigView        │
        └─────────────────────┘
```

### View Navigation Strategy

**Window Types:**
- **Main Window**: BrowserView (LDAP browsing interface)
- **Config Window**: Modal/separate window with ConfigView

**View Switching:**
- App launches with BrowserView (assuming saved connection exists)
- Settings button/menu opens ConfigView in popup window
- After saving connection in config, optionally auto-connect and show BrowserView

### State Management Updates

**AppState additions:**
```rust
pub struct AppState {
    // Existing fields
    pub config: Config,
    pub selected_connection: Option<String>,
    pub saved_connections: Vec<SavedConnection>,
    pub is_connected: bool,
    pub status_message: Option<String>,
    
    // New fields for browsing
    pub active_client: Option<LdapClient>,  // Active LDAP connection
    pub current_base_dn: Option<String>,    // Current search base
    pub tree_state: TreeState,              // Expanded nodes, loaded children
    pub selected_entry: Option<Entry>,      // Currently selected entry in tree
    pub search_results: Option<Vec<Entry>>, // Search results (replaces tree when active)
    pub search_filter: String,              // Current search filter
}

pub struct TreeState {
    pub expanded_nodes: HashSet<String>,    // DNs that are expanded
    pub node_children: HashMap<String, Vec<Entry>>, // Cached children by DN
    pub loading_nodes: HashSet<String>,     // DNs currently loading
}
```

## Feature 1: Connection Configuration Saving ✅

**Status**: ALREADY IMPLEMENTED

**Current Implementation:**
- Location: `moribito-rs/crates/core/src/config.rs`
- Platform-specific paths:
  - macOS: `~/.moribito/config.yaml`
  - Linux: `$XDG_CONFIG_HOME/moribito/config.yaml`
  - Windows: `%APPDATA%\moribito\config.yaml`
- Auto-save on connection CRUD operations
- Format: YAML via serde_yaml

**Future Enhancements (Not in this plan):**
- Password encryption via OS keychain
- Config file watching for live reload
- Backup/restore functionality

**No Implementation Required** - Just ensure it continues working when refactoring views.

---

## Feature 2: Main LDAP Navigation/Viewing Interface

### Design Specification

**Layout (approved by user):**

Note: Menus appear in native macOS system menu bar (see Feature 4), NOT inside the application window.

```
macOS System Menu Bar (top of screen):
 [Apple] [Moribito] [File] [Edit] [View] [Connection] [Window] [Help]

Application Window:
┌─────────────────────────────────────────────────────────────────┐
│ Moribito                                           [⚙️ Settings] │ ← Title bar
├─────────────────────────────────────────────────────────────────┤
│ Search Bar:                                                      │
│   🔍 Filter: [________________] Base: [dc=example,dc=com ▼]     │
│   [Search] [Clear]                                               │
├──────────────────────┬──────────────────────────────────────────┤
│ DN Tree (30%)        │ Entry Details (70%)                       │
│ ┌──────────────────┐ │ ┌────────────────────────────────────┐   │
│ │ ▼ dc=example...  │ │ │ DN: cn=John,ou=Users,dc=...        │   │
│ │   ▼ ou=Users     │ │ │                                    │   │
│ │     • cn=John    │◄┼─┤ Attribute          | Value         │   │
│ │     • cn=Jane    │ │ │ ─────────────────────────────────  │   │
│ │   ▼ ou=Groups    │ │ │ cn                 | John Doe      │   │
│ │     • cn=Admins  │ │ │ objectClass        | inetOrgPerson │   │
│ │                  │ │ │                    | person        │   │ ← Multi-value
│ │                  │ │ │                    | top           │   │
│ └──────────────────┘ │ │ mail               | john@ex.com   │   │
│                      │ │                                    │   │
│                      │ │ [Edit] [Delete] [Refresh] [Export] │   │
│                      │ └────────────────────────────────────┘   │
└──────────────────────┴──────────────────────────────────────────┘
│ ● Connected: ldaps://ldap.example.com:636 | Entries: 1/1       │
└─────────────────────────────────────────────────────────────────┘
```

**Behavior:**
- Single click entry → show details in right panel
- Double click container → expand/collapse
- Search results replace tree view (not separate panel)
- Multi-valued attributes: each value on separate row in table
- Resizable split using gpui-component's `Resizable`
- Table format: 2 columns (Attribute | Value), no header

### Components to Create

#### 1. **BrowserView** (Main View)
**File**: `moribito-rs/crates/gui/src/views/browser_view.rs`

**Responsibilities:**
- Root view component for LDAP browsing
- Compose SearchBar, TreeView, EntryDetailsTable, StatusBar
- Manage layout (resizable split)
- Handle view-level state (which entry is selected)

**Structure:**
```rust
pub struct BrowserView {
    app_state: SharedAppState,
    search_query: String,
    search_base_dn: String,
}

impl BrowserView {
    pub fn new(cx: &mut Context) -> Self { ... }
    
    fn render_search_bar(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn render_tree_panel(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn render_details_panel(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn render_status_bar(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    
    fn handle_search(&mut self, cx: &mut ViewContext<Self>) { ... }
    fn handle_clear_search(&mut self, cx: &mut ViewContext<Self>) { ... }
}
```

#### 2. **TreeView** (DN Hierarchy)
**File**: `moribito-rs/crates/gui/src/components/tree_view.rs`

**Responsibilities:**
- Display hierarchical DN tree
- Lazy loading (fetch children on expand)
- Expand/collapse state management
- Selection handling
- Icons: ▼ (expanded), ▷ (collapsed), • (leaf)

**Structure:**
```rust
pub struct TreeView {
    app_state: SharedAppState,
    on_select: Option<Box<dyn Fn(Entry, &mut Context)>>,
}

struct TreeNode {
    entry: Entry,
    is_expanded: bool,
    is_loading: bool,
    has_children: bool,  // Inferred from objectClass or tried loading
    depth: usize,
}

impl TreeView {
    pub fn new(app_state: SharedAppState) -> Self { ... }
    pub fn on_select(mut self, f: impl Fn(Entry, &mut Context) + 'static) -> Self { ... }
    
    fn render_node(&self, node: &TreeNode, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn toggle_expand(&mut self, dn: String, cx: &mut ViewContext<Self>) { ... }
    fn load_children(&mut self, dn: String, cx: &mut ViewContext<Self>) { ... }
}
```

**Lazy Loading Strategy:**
- When node expanded: spawn async task to call `client.get_children(dn)`
- Store results in `AppState.tree_state.node_children`
- Mark DN in `expanded_nodes` set
- Show loading spinner in `loading_nodes`

#### 3. **EntryDetailsTable** (Attribute Display)
**File**: `moribito-rs/crates/gui/src/components/entry_details_table.rs`

**Responsibilities:**
- Display entry DN and attributes
- Table format: Attribute | Value (no header)
- Multi-valued attributes: each value on separate row
- Action buttons: Edit, Delete, Refresh, Export

**Structure:**
```rust
pub struct EntryDetailsTable {
    entry: Option<Entry>,
    on_edit: Option<Box<dyn Fn(&mut Context)>>,
    on_delete: Option<Box<dyn Fn(&mut Context)>>,
    on_refresh: Option<Box<dyn Fn(&mut Context)>>,
    on_export: Option<Box<dyn Fn(&mut Context)>>,
}

impl EntryDetailsTable {
    pub fn new(entry: Option<Entry>) -> Self { ... }
    
    fn render_dn(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn render_attributes_table(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
    fn render_actions(&self, cx: &mut ViewContext<Self>) -> impl IntoElement { ... }
}
```

**Multi-value Rendering:**
```rust
// For attribute with multiple values:
// objectClass: ["inetOrgPerson", "person", "top"]
// Render as:
// | objectClass | inetOrgPerson |
// |             | person        |
// |             | top           |
```

Use gpui-component's `Table` or custom div-based table.

#### 4. **SearchBar** (Search Interface)
**File**: `moribito-rs/crates/gui/src/components/search_bar.rs`

**Responsibilities:**
- Filter input field (LDAP search filter syntax)
- Base DN dropdown/input
- Search and Clear buttons
- Validation feedback

**Structure:**
```rust
pub struct SearchBar {
    filter: String,
    base_dn: String,
    on_search: Option<Box<dyn Fn(String, String, &mut Context)>>,
    on_clear: Option<Box<dyn Fn(&mut Context)>>,
}

impl SearchBar {
    pub fn new() -> Self { ... }
    pub fn on_search(mut self, f: impl Fn(String, String, &mut Context) + 'static) -> Self { ... }
}
```

Use gpui-component's `Input`, `Button`, `Dropdown`.

#### 5. **StatusBar** (Connection Status)
**File**: `moribito-rs/crates/gui/src/components/status_bar.rs`

**Responsibilities:**
- Show connection status (● Connected/○ Disconnected)
- Server info (protocol://host:port)
- Entry count or other stats
- Error messages (transient)

**Structure:**
```rust
pub struct StatusBar {
    app_state: SharedAppState,
}

impl StatusBar {
    pub fn new(app_state: SharedAppState) -> Self { ... }
}
```

### Search Results Strategy

**When search is active:**
- `AppState.search_results = Some(vec![entries])`
- `TreeView` checks if `search_results.is_some()`
- If yes, render flat list of results instead of tree
- Click result → show details (same as tree)
- Clear button → set `search_results = None`, restore tree

**Search Implementation:**
```rust
// In BrowserView::handle_search()
let filter = self.search_query.clone();
let base_dn = self.search_base_dn.clone();

cx.spawn(|this, mut cx| async move {
    let state = this.read(cx).app_state.clone();
    let client = state.read().active_client.as_ref()?;
    
    let results = client.search_paged(&base_dn, &filter, None, 100).await?;
    
    cx.update(|cx| {
        let mut state = state.write();
        state.search_results = Some(results);
        cx.notify();
    });
})
```

### Connection Lifecycle Management

**Connect Flow:**
1. User selects saved connection (from config or init view)
2. Create `LdapClient` from connection settings
3. Call `client.connect()` and `client.bind()` (async)
4. Store in `AppState.active_client`
5. Set `AppState.is_connected = true`
6. Load root DN into tree (auto-expand first level)
7. Update status bar

**Disconnect Flow:**
1. Call `client.close()` (async)
2. Set `AppState.active_client = None`
3. Set `AppState.is_connected = false`
4. Clear tree state
5. Update status bar

### Implementation Steps

**Step 1: Update AppState** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/app_state.rs`
- ✅ Add fields: `active_client`, `current_base_dn`, `tree_state`, `selected_entry`, `search_results`, `search_filter`
- ✅ Add `TreeState` struct
- ⏳ Add methods: `connect()`, `disconnect()`, `expand_node()`, `load_children()` - PARTIALLY (connect logic in ConfigView)

**Step 2: Create StatusBar Component** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/components/status_bar.rs`
- ✅ Simple read-only display from AppState
- ✅ Uses gpui-component's text components

**Step 3: Create SearchBar Component** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/components/search_bar.rs`
- ✅ Input field + buttons
- ✅ Callbacks for search/clear actions
- ✅ Uses gpui-component's `Input`, `Button`

**Step 4: Create EntryDetailsTable Component** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/components/entry_details_table.rs`
- ✅ Table rendering (basic structure)
- ✅ Action buttons (Edit/Delete/Refresh/Export - stubs)
- ✅ Uses gpui-component's `Button`

**Step 5: Create TreeView Component** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/components/tree_view.rs`
- ✅ Tree rendering structure
- ⏳ Expand/collapse with icons - NEEDS IMPLEMENTATION
- ⏳ Lazy loading with async children fetch - NEEDS IMPLEMENTATION
- ⏳ Selection highlighting - NEEDS IMPLEMENTATION
- ⏳ Search results mode - NEEDS IMPLEMENTATION

**Step 6: Create BrowserView** ✅ COMPLETE (Structure)
- File: `moribito-rs/crates/gui/src/views/browser_view.rs`
- ✅ Compose all components
- ✅ Resizable split layout (using gpui-component's `h_resizable`)
- ⏳ Wire up callbacks between components - PARTIALLY
- ⏳ Handle search/clear actions - STUB HANDLERS

**Step 7: Add Browser-specific Actions** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/actions.rs`
- ✅ Added: `Refresh`, `ExpandAll`, `CollapseAll`, `Connect`, `Disconnect`, etc.

**Step 8: Update Component Module Exports** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/components/mod.rs`
- ✅ Export new components: `tree_view`, `entry_details_table`, `search_bar`, `status_bar`

**Step 9: Create Views Module** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/views/mod.rs`
- ✅ Export: `browser_view`, `config_view`

**Step 10: Testing** ⏳ IN PROGRESS
- ⏳ Manual testing with real LDAP server
- ⏳ Test lazy loading, search, selection
- ⏳ Test error handling (connection failures)

---

## Feature 3: Move Config View to Popup Window ✅ COMPLETE

### Design Strategy

**Approach**: Use GPUI's multi-window support to open ConfigView in a separate modal window.

**Window Creation:**
```rust
// In BrowserView or via menu action
fn open_config_window(&mut self, cx: &mut ViewContext<Self>) {
    let app_state = self.app_state.clone();
    
    let bounds = Bounds::centered(None, size(px(700.0), px(500.0)), cx);
    
    cx.open_window(
        WindowOptions {
            window_bounds: Some(WindowBounds::Windowed(bounds)),
            focus: true,
            show: true,
            kind: WindowKind::Normal,
            is_movable: true,
            titlebar: Some(TitlebarOptions {
                title: Some("Connection Settings".into()),
                ..Default::default()
            }),
            ..Default::default()
        },
        |window, cx| {
            let config_view = ConfigView::new(window, cx);
            cx.new(|cx| Root::new(config_view, window, cx))
        },
    );
}
```

### Implementation Steps

**Step 1: Refactor ConfigView** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/views/config_view.rs`
- ✅ ConfigView is self-contained
- ✅ Window close handling via `window.remove_window()`
- ✅ Functionality intact + enhanced with file persistence
- ✅ Connection and close on success implemented

**Step 2: Move ConfigView to views/ Directory** ✅ COMPLETE
- From: `moribito-rs/crates/gui/src/config_view.rs`
- To: `moribito-rs/crates/gui/src/views/config_view.rs`
- ✅ Updated imports in `main.rs` and other files

**Step 3: Add OpenConfigWindow Action** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/actions.rs`
- ✅ Add `OpenConfigWindow` action

**Step 4: Add Config Window Opener in BrowserView** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/views/browser_view.rs`
- ✅ Add `open_config_window()` method
- ✅ Wire to menu item (via action handler)
- ✅ Passes SharedAppState to ConfigView

**Step 5: Update Main Entry Point** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/main.rs`
- ✅ Main window opens BrowserView
- ✅ BrowserView is default view
- ✅ Config opens via menu

---

## Feature 4: Native macOS Menu Bar ✅ COMPLETE

### Overview

Populate the **native macOS system menu bar** (the menu bar at the very top of the screen, where the Apple icon lives) with application-specific menu items. This is the OS-level menu system, NOT a custom menu bar drawn inside the application window.

**Target:** On macOS, the menu bar should show:
```
 [Apple] [Moribito] [File] [Edit] [View] [Connection] [Window] [Help]
```

On Windows/Linux, these menus would appear in the window's title bar (GPUI handles platform differences automatically).

### Menu Structure

```
Moribito (Application Menu - macOS only)
  ├─ About Moribito
  ├─ ──────────
  ├─ Preferences...           Cmd+,
  ├─ ──────────
  ├─ Hide Moribito            Cmd+H
  ├─ Hide Others              Opt+Cmd+H
  ├─ Show All
  ├─ ──────────
  └─ Quit Moribito            Cmd+Q

File
  ├─ New Connection...        Cmd+N
  ├─ ──────────
  ├─ Close Window             Cmd+W

Edit
  ├─ Undo                     Cmd+Z
  ├─ Redo                     Shift+Cmd+Z
  ├─ ──────────
  ├─ Cut                      Cmd+X
  ├─ Copy                     Cmd+C
  ├─ Paste                    Cmd+V
  ├─ Delete                   Delete
  ├─ ──────────
  └─ Select All               Cmd+A

View
  ├─ Refresh                  Cmd+R
  ├─ ──────────
  ├─ Expand All
  └─ Collapse All

Connection
  ├─ Connect                  Cmd+K
  ├─ Disconnect               Cmd+Shift+K
  └─ Test Connection          Cmd+T

Window (macOS only)
  ├─ Minimize                 Cmd+M
  ├─ Zoom
  ├─ ──────────
  └─ Bring All to Front

Help
  ├─ Moribito Help
  └─ Documentation
```

### GPUI Native Menu Integration

GPUI provides APIs to register menus with the OS menu system. On macOS, this automatically populates the system menu bar at the top of the screen.

**Menu Registration:**
```rust
// In main.rs after creating the application
Application::new().run(|cx: &mut App| {
    // Register native menus with the OS
    cx.set_menus(vec![
        build_app_menu(),      // "Moribito" menu (macOS only)
        build_file_menu(),     // "File" menu
        build_edit_menu(),     // "Edit" menu
        build_view_menu(),     // "View" menu
        build_connection_menu(), // "Connection" menu
        build_window_menu(),   // "Window" menu (macOS only)
        build_help_menu(),     // "Help" menu
    ]);
    
    // ... rest of app initialization
});
```

**Menu Definition Example:**
```rust
fn build_file_menu() -> Menu {
    Menu::new("File")
        .entry("New Connection...", Some("cmd-n"), NewConnection.boxed_clone())
        .separator()
        .entry("Close Window", Some("cmd-w"), CloseWindow.boxed_clone())
}

fn build_connection_menu() -> Menu {
    Menu::new("Connection")
        .entry("Connect", Some("cmd-k"), Connect.boxed_clone())
        .entry("Disconnect", Some("shift-cmd-k"), Disconnect.boxed_clone())
        .entry("Test Connection", Some("cmd-t"), TestConnection.boxed_clone())
}
```

### Platform-Specific Behavior

**macOS**: 
- Menus appear in system menu bar at top of screen
- Application menu ("Moribito") is automatically created
- Window menu is standard on macOS

**Windows/Linux**: 
- Menus appear in application window title bar or as traditional menu bar within window
- No application menu (File is first)
- GPUI handles platform differences automatically

### Implementation Steps

**Step 1: Add Menu Actions** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/actions.rs`
- ✅ Add: `NewConnection`, `OpenConfigWindow`, `CloseWindow`
- ✅ Add: `Cut`, `Copy`, `Paste`, `DeleteEntry`, `SelectAll`
- ✅ Add: `Refresh`, `ExpandAll`, `CollapseAll`
- ✅ Add: `Connect`, `Disconnect`, `TestConnection`, `ShowDocumentation`, `ShowAbout`

**Step 2: Create Menu Module** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/menus.rs`
- ✅ Define menu structure (Moribito, File, Edit, View, Connection, Help)
- ✅ Build menu items with actions
- ✅ Export `build_menus()` function
- ✅ Reorganized: Connection management in Connection menu, Settings placeholder in Moribito menu

**Step 3: Register Action Handlers** ✅ COMPLETE
- ✅ In BrowserView: handle `OpenConfigWindow`, `Refresh`, `ExpandAll`, `CollapseAll`
- ⏳ In main.rs or app-level: handle `ShowAbout`, `ShowDocumentation` - STUB HANDLERS

**Step 4: Set Up Menu in Main** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/main.rs`
- ✅ Call `cx.set_menus(build_menus())` after app initialization
- ✅ Menus registered and working

**Step 5: Handle Menu Actions in BrowserView** ✅ COMPLETE
- File: `moribito-rs/crates/gui/src/views/browser_view.rs`
- ✅ Implement action handlers via `on_action()`
- ✅ Actions properly wired to menu items

**Step 6: Implement About Dialog** ⏳ NOT IMPLEMENTED
- File: `moribito-rs/crates/gui/src/components/about_dialog.rs`
- ⏳ Simple modal with app name, version, logo - TODO
- ⏳ Close button - TODO
- ⏳ Uses gpui-component's `Modal` or custom window - TODO

---

## Critical Files Summary

### Files to Create (New)

1. `moribito-rs/crates/gui/src/views/mod.rs` - Views module
2. `moribito-rs/crates/gui/src/views/browser_view.rs` - Main browser view (~400 lines)
3. `moribito-rs/crates/gui/src/components/tree_view.rs` - DN tree component (~300 lines)
4. `moribito-rs/crates/gui/src/components/entry_details_table.rs` - Entry details (~200 lines)
5. `moribito-rs/crates/gui/src/components/search_bar.rs` - Search UI (~150 lines)
6. `moribito-rs/crates/gui/src/components/status_bar.rs` - Status display (~100 lines)
7. `moribito-rs/crates/gui/src/components/about_dialog.rs` - About modal (~100 lines)
8. `moribito-rs/crates/gui/src/menus.rs` - Menu definitions (~200 lines)

**Total new code**: ~1,450 lines

### Files to Modify (Existing)

1. `moribito-rs/crates/gui/src/app_state.rs` - Add browser state fields (~100 lines added)
2. `moribito-rs/crates/gui/src/actions.rs` - Add menu/browser actions (~50 lines added)
3. `moribito-rs/crates/gui/src/components/mod.rs` - Export new components (~4 lines)
4. `moribito-rs/crates/gui/src/main.rs` - Change to BrowserView, add menus (~30 lines modified)
5. `moribito-rs/crates/gui/src/config_view.rs` → `views/config_view.rs` - Move + minor updates (~10 lines)

**Total modified code**: ~200 lines

### Dependencies to Add

Check if needed:
- ✅ `gpui-component` 0.4.0 (already in Cargo.toml)
- Check gpui-component docs for: `Resizable`, `Table`, `Input`, `Button`, `Dropdown`, `Modal`, `Icon`, `Label`

---

## Implementation Order

**Phase 1: Browser View Foundation (Feature 2 - Part 1)**
1. Update AppState with browser fields
2. Create StatusBar component (simplest)
3. Create SearchBar component
4. Create EntryDetailsTable component (no actions yet, just display)

**Phase 2: Tree View (Feature 2 - Part 2)**
5. Create TreeView component
6. Implement lazy loading
7. Implement expand/collapse
8. Implement selection

**Phase 3: Main Browser View (Feature 2 - Part 3)**
9. Create BrowserView
10. Compose all components
11. Implement search logic
12. Add connection lifecycle (connect/disconnect)

**Phase 4: Config Window (Feature 3)**
13. Move ConfigView to views/
14. Add config window opener in BrowserView
15. Update main.rs to launch BrowserView by default

**Phase 5: Menu Bar (Feature 4)**
16. Add menu actions
17. Create menus module
18. Register menu in main.rs
19. Implement action handlers
20. Create About dialog

**Phase 6: Testing & Polish**
21. Manual testing with real LDAP
22. Error handling refinement
23. UI polish (spacing, colors, icons)
24. Documentation updates

---

## Success Criteria

✅ **Feature 1 (Config Saving)**:
- Connection configs persist across app restarts
- CRUD operations on saved connections work
- Config stored in platform-specific location

✅ **Feature 2 (Browser View)**:
- Tree view displays DN hierarchy
- Lazy loading works (children load on expand)
- Entry details show in table format
- Multi-valued attributes display correctly
- Search replaces tree with results
- Status bar shows connection state
- Resizable split works smoothly

✅ **Feature 3 (Config Popup)**:
- Settings button opens config in new window
- Config window is modal/separate
- Main window shows BrowserView by default
- Config changes save and don't require restart

✅ **Feature 4 (Menu Bar)**:
- macOS menu bar displays at top of screen
- All menu items trigger correct actions
- Keyboard shortcuts work (Cmd+N, Cmd+Q, etc.)
- About dialog displays
- Menu actions work from BrowserView

---

## Testing Strategy

### Manual Testing Checklist

**Connection Testing:**
- [ ] Connect to LDAP server from config
- [ ] Test connection validation
- [ ] Disconnect cleanly
- [ ] Reconnect to same server
- [ ] Switch between saved connections

**Browser Testing:**
- [ ] Load root DN into tree
- [ ] Expand container nodes
- [ ] Load children lazily
- [ ] Select entries (details appear)
- [ ] Multi-valued attributes render correctly
- [ ] Resize split panel

**Search Testing:**
- [ ] Enter search filter
- [ ] Execute search (results replace tree)
- [ ] Click search result (details appear)
- [ ] Clear search (tree returns)
- [ ] Search with different base DNs

**Config Window Testing:**
- [ ] Open config from Settings button
- [ ] Open config from menu
- [ ] Save connection in config window
- [ ] Close config window
- [ ] Config changes persist

**Menu Testing:**
- [ ] All menu items clickable
- [ ] Keyboard shortcuts work
- [ ] Menu actions trigger correct behavior
- [ ] About dialog opens
- [ ] Quit works (Cmd+Q)

### Error Scenarios

- [ ] Connection failure (wrong host/port)
- [ ] Authentication failure (wrong credentials)
- [ ] Search timeout
- [ ] Malformed search filter
- [ ] Entry not found (deleted externally)
- [ ] Network interruption during operation

---

## Notes & Considerations

### gpui-component Usage

**Key components to use:**
- `Resizable` - for split panel (https://longbridge.github.io/gpui-component/docs/components/resizable)
- `Input` - for search filter, text fields
- `Button` - for all buttons
- `Dropdown` - for base DN selector
- `Icon` - for tree expand/collapse, status indicators
- `Label` - for text display
- `Modal` - for About dialog (if available)
- `Table` - for entry details (if available, else use custom div layout)

Check docs for latest API: https://longbridge.github.io/gpui-component/docs/components/

### LDAP Client Threading

- All LDAP operations are async (use `cx.spawn()`)
- Client is not Clone, store in `Option` in AppState
- Use `Arc<RwLock>` to share between view and async tasks
- Handle connection drops gracefully

### Tree Performance

- Don't load entire directory at once (use lazy loading)
- Cache loaded children in `TreeState.node_children`
- Clear cache on refresh action
- Consider virtualization for large result sets (future enhancement)

### Search Filter Validation

- Basic validation before sending to server
- Show error in status bar for invalid filters
- Example valid filters: `(cn=*)`, `(&(objectClass=person)(mail=*@example.com))`

### Multi-Window Coordination

- ConfigView changes should update BrowserView state
- Use SharedAppState for coordination
- Consider `cx.notify()` to trigger re-renders after config changes
- Handle case where config window is already open (don't open duplicate)

---

## Future Enhancements (Not in This Plan)

- Entry editing (modify attributes)
- Entry creation (add new entries)
- Entry deletion (implement delete action)
- Export functionality (LDIF, CSV)
- Schema browsing
- Bookmarks/favorites
- Connection history
- Password encryption (OS keychain)
- Init/welcome view with recent connections
- Keyboard navigation in tree
- Copy DN to clipboard (right-click menu)
- Batch operations
- Undo/redo

---

## End of Plan

This plan provides a complete roadmap for implementing all 4 requested features. Implementation should follow the phase order for minimal conflicts and incremental progress. Each phase builds on the previous, ending with a fully functional LDAP browser with menu bar and popup configuration.
`
