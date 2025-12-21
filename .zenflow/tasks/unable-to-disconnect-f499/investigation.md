# Investigation: Unable to Disconnect Bug

## Bug Summary
When on the "Browser View", the macOS menu bar "Connection" dropdown has all items grayed out except "Manage Connections". Users cannot disconnect from an active connection and return to the start screen.

## Root Cause Analysis

### Architecture Overview
- **AppView** (`views/app_view.rs`): Main controller that switches between StartView and BrowserView based on `app_state.is_connected`
- **BrowserView** (`views/browser_view.rs`): Displays the LDAP tree browser when connected
- **StartView** (`views/start_view.rs`): Displays the connection selection screen
- **AppState** (`app_state.rs`): Contains the `is_connected` flag and `disconnect()` method
- **Menus** (`menus.rs`): Defines the "Connection" menu with Disconnect action
- **Actions** (`actions.rs`): Defines all available actions including the Disconnect action

### The Problem
1. The "Connection" menu in `menus.rs` line 74-87 defines a "Disconnect" menu item (line 82)
2. However, **BrowserView has no handler for the Disconnect action**
3. In GPUI, menu items without handlers appear grayed out (disabled)
4. The AppState has a `disconnect()` method (line 174-181) that properly resets the connection state
5. The AppView correctly switches back to StartView when `is_connected` becomes false

### Missing Implementation
BrowserView's render method (line 307-391) registers handlers for many actions via `.on_action()`:
- OpenConfigWindow, Refresh, ExpandAll, CollapseAll, FilterByOu, SelectEntry, RefreshEntry, DeleteEntry, ExportEntry

But there's **no handler for the Disconnect action**.

## Proposed Solution

Add a `handle_disconnect` method to BrowserView that:
1. Calls `app_state.disconnect()` to reset connection state
2. Let AppView automatically re-render and show StartView

The implementation is straightforward:
```rust
fn handle_disconnect(
    &mut self,
    _: &Disconnect,
    _window: &mut Window,
    cx: &mut Context<Self>,
) {
    let mut state = self.app_state.write();
    state.disconnect();
    state.set_status("Disconnected");
    cx.notify();
}
```

Then register it in the render method with:
```rust
.on_action(cx.listener(Self::handle_disconnect))
```

## Expected Behavior
1. User is in BrowserView (connected)
2. User clicks "Connection" → "Disconnect" menu item
3. Disconnect handler executes, setting `is_connected = false`
4. AppView re-renders and switches to StartView
5. User is back at the start screen

## Affected Components
- `BrowserView` (needs Disconnect handler)
- No changes needed to AppView, AppState, or Menus

## Implementation Details

### Changes Made

#### 1. Added Disconnect Handler to BrowserView (lines 69-80)
```rust
fn handle_disconnect(
    &mut self,
    _: &Disconnect,
    _window: &mut Window,
    cx: &mut Context<Self>,
) {
    let mut state = self.app_state.write();
    state.disconnect();
    state.set_status("Disconnected");
    cx.notify();
}
```

#### 2. Registered Handler in Render Method (line 334)
Added `.on_action(cx.listener(Self::handle_disconnect))` to the v_flex element's action listeners.

#### 3. Added Regression Tests to app_state.rs
- `test_disconnect()`: Verifies that disconnect() properly clears connection state
- `test_disconnect_from_shared_state()`: Verifies disconnect works with shared state concurrency

### How It Works

1. **Action Dispatch**: User clicks "Connection" → "Disconnect" menu item
2. **Handler Invocation**: The registered handler catches the Disconnect action
3. **State Mutation**: Handler acquires write lock and calls `state.disconnect()`
4. **State Reset**: disconnect() sets is_connected=false and clears all connection state
5. **UI Update**: cx.notify() triggers re-render
6. **View Switch**: AppView checks is_connected, sees false, switches to StartView
7. **User Returns**: User is back at the start screen with "Disconnected" status

## Testing Strategy
1. ✅ Build succeeds without errors
2. ✅ All existing tests pass
3. ✅ New disconnect tests pass
4. Manual Testing:
   - Connect to an LDAP server (should show BrowserView)
   - Click "Connection" menu (Disconnect should be enabled now)
   - Click "Disconnect" (should return to StartView)
   - Verify status message shows "Disconnected"
