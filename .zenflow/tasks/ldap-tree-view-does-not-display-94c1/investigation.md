# LDAP Tree View Not Displaying - Investigation

## Bug Summary
When a user connects to an LDAP server via the connection dialog, the browser view opens but the tree view does not populate with entries from the base DN. The tree remains empty despite a successful connection.

## Root Cause Analysis

### The Problem
The BrowserView is initialized **before** any LDAP connection is established. The flow is:

1. **Application starts** (`main.rs:77-82`)
   - `BrowserView::new()` is called with app_state
   - At this point, `app_state.is_connected = false` and `active_client = None`

2. **User clicks Connect** (`config_view.rs:329-375`)
   - ConfigView validates settings
   - Creates LDAP client and authenticates
   - Updates `app_state` with:
     - `active_client = Some(client)`
     - `is_connected = true`
     - `current_base_dn = Some(base_dn)`
   - **Closes config window** (line 375)

3. **Browser window exists but tree is never initialized**
   - The `BrowserView.initialize()` method exists (browser_view.rs:88-90)
   - This method calls `tree_view.reload_tree()` with the base DN
   - **BUT THIS METHOD IS NEVER CALLED** after connection

### Affected Components

**browser_view.rs**
- `BrowserView::new()` (lines 34-52): Creates TreeView with empty state
- `BrowserView::initialize()` (lines 88-90): **Exists but is never called**
- This method properly calls `tree_view.reload_tree()` to populate the tree

**tree_view.rs**
- `TreeView::new()` (lines 21-29): Initializes with empty TreeState
- `TreeView::reload_tree()` (lines 111-151): Loads children from LDAP and populates tree
  - Checks for active client (line 115)
  - Calls `client.get_children(base_dn)` (line 124)
  - Updates TreeState with loaded items (lines 143-145)

**config_view.rs**
- `connect_and_close()` (lines 329-375): Successfully connects and updates app_state
  - Sets `current_base_dn` (line 365)
  - **Only closes window, doesn't trigger tree initialization**

**main.rs**
- Lines 77-82: Creates BrowserView once at startup
- No mechanism to detect connection changes or trigger initialization

## Proposed Solution

### Option 1: Call initialize() after connection (Simplest)
After successfully connecting in ConfigView, trigger BrowserView initialization:
- Need to access the BrowserView entity from ConfigView
- Call `browser_view.update(cx, |view, cx| view.initialize(&base_dn, window, cx))`

**Issue**: ConfigView doesn't have a reference to BrowserView entity

### Option 2: Add connection event listener (Reactive)
Make BrowserView react to app_state connection changes:
- Add a subscription/listener in BrowserView that watches `app_state.is_connected`
- When connection state changes to true, automatically call `initialize()`

### Option 3: Initialize on first render (Pragmatic) ✅ **RECOMMENDED**
Check connection state in BrowserView's render or during update:
- In `BrowserView::render()` or via a separate update method
- If `app_state.is_connected && tree is empty`, call `initialize()`
- Use a flag to prevent repeated initialization

### Option 4: Window focus callback
When config window closes and browser window regains focus:
- Add window focus handler to BrowserView
- Check if connection state changed and initialize if needed

## Edge Cases to Consider
1. **Reconnection**: What if user disconnects and connects to a different server?
   - Should clear existing tree and reload

2. **Failed connection**: What if connection succeeds initially but fails during tree load?
   - Error handling already exists in `reload_tree()` (line 148)

3. **Multiple reconnects**: Prevent redundant tree reloads
   - Need flag to track if tree has been loaded for current connection

## Recommended Implementation: Option 3

Add a reactive check in BrowserView that initializes the tree when:
- `app_state.is_connected == true`
- `app_state.current_base_dn.is_some()`
- Tree hasn't been initialized yet for this base DN

This can be implemented by:
1. Adding a `last_initialized_base_dn: Option<String>` field to BrowserView
2. In render() or via a periodic check, compare current base DN with last initialized
3. If different and connected, call `initialize()`

This approach is:
- Non-invasive (doesn't require changing ConfigView or main.rs)
- Reactive (responds to state changes)
- Handles reconnection scenarios
- Prevents redundant reloads

---

## Implementation Summary

### Changes Made

**File: moribito-rs/crates/gui/src/views/browser_view.rs**

1. **Added tracking field** (line 30):
   ```rust
   last_initialized_base_dn: Option<String>
   ```
   This field tracks the last base DN that was used to initialize the tree view.

2. **Updated BrowserView::new()** (line 52):
   - Initialize `last_initialized_base_dn` to `None`

3. **Updated BrowserView::initialize()** (line 92):
   - Now stores the base DN in `last_initialized_base_dn` after successful initialization
   - This allows tracking which base DN the tree was last initialized with

4. **Added check_and_initialize() method** (lines 95-129):
   - Called from `render()` at the start of each render cycle
   - Checks if:
     - LDAP connection is active (`app_state.is_connected`)
     - A base DN is available (`app_state.current_base_dn.is_some()`)
     - Tree hasn't been initialized with this base DN yet
   - If all conditions are met, calls `initialize()` with the current base DN
   - Uses proper Rust borrowing to avoid holding locks across method calls

5. **Updated render() method** (line 148):
   - Added call to `check_and_initialize()` at the start
   - This ensures tree initialization happens reactively when connection state changes

6. **Updated Clone implementation** (line 245):
   - Added `last_initialized_base_dn` field to clone

### How It Works

1. **Application startup**: BrowserView is created with empty tree, `last_initialized_base_dn = None`
2. **User connects**: ConfigView establishes connection and updates `SharedAppState` with client and base DN
3. **Next render cycle**: BrowserView's render method calls `check_and_initialize()`
4. **Initialization check**: Detects that connection exists and base DN differs from last initialized
5. **Tree population**: Calls `initialize()` which triggers `tree_view.reload_tree()`
6. **Tracking**: `last_initialized_base_dn` is updated to prevent redundant reloads

### Edge Cases Handled

- **Never initialized**: When `last_initialized_base_dn` is `None`, initialization occurs on first connection
- **Different base DN**: When user reconnects with different base DN, tree is reloaded
- **Same base DN**: Redundant initialization is prevented by comparing base DNs
- **No connection**: When not connected, check is skipped entirely
- **Rust borrowing**: Properly releases app_state read lock before calling initialize to avoid borrow conflicts

### Test Results

- **Build Status**: ✅ Successfully compiled with no errors
- **Warnings**: 34 warnings (mostly unused variables and dead code - not related to this fix)
- **Manual Testing**: Requires LDAP server connection to verify tree population

The implementation follows Option 3 from the investigation and provides a reactive, non-invasive solution that automatically initializes the tree view when an LDAP connection is established.
