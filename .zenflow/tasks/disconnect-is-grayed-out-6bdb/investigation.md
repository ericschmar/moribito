## Bug Summary
Connection menu items (Connect/Disconnect/Manage Connections/Test Connection) are all disabled while viewing the BrowserView even after a successful connection. Users expect at least “Disconnect” and “Manage Connections” to remain enabled when connected.

## Root Cause Analysis
- Menus are registered globally (`main.rs` sets `cx.set_menus(build_menus())`), but action handlers for Connection items exist only inside `BrowserView` via `.on_action(...)` on its root `v_flex`.
- The window focus is set once to `AppView`’s focus handle (`main.rs:83`); `AppView` itself has no action handlers and does not refocus to `BrowserView` when switching views.
- In GPUI 0.2.2, menu items disable when no handler is available in the current action context/focus chain (see gpui-component issue #1728: wrong action_context when another element is focused). Because the action context remains on `AppView` (or on focused child widgets without these handlers), the Connection menu has no reachable handlers and is grayed out.

## Affected Components
- `moribito-rs/crates/gui/src/main.rs` (initial focus)
- `moribito-rs/crates/gui/src/views/app_view.rs` (view switching without refocusing)
- `moribito-rs/crates/gui/src/views/browser_view.rs` (connection action handlers scoped to this view)
- `moribito-rs/crates/gui/src/menus.rs` (menu definitions depend on available handlers)

## Proposed Solution
- Ensure the BrowserView becomes the focused action context when connected so its `.on_action` handlers back the Connection menu. Options:
  - On switching to `BrowserView`, call `window.focus(&browser.focus_handle(cx))` (or an equivalent focus transfer) so the menu action context includes BrowserView handlers.
  - Alternatively/additionally, register window-level action handlers for `OpenConfigWindow`, `Disconnect`, etc., when connected (e.g., via `window.on_action`/`cx.on_action`) to keep Connection items enabled regardless of focus.
- After focusing/registration, verify that “Disconnect” and “Manage Connections” remain enabled while connected and that Disconnect returns to StartView.

## Implementation Notes
- AppView now focuses the active view: when connected, it reads the BrowserView focus handle and calls `window.focus`, and when disconnected it focuses StartView. This keeps BrowserView connection handlers in the action context so Connection menu items stay enabled.

## Test Results
- `cargo test --all` (from `moribito-rs`): failed due to rustc SIGBUS during compilation (toolchain crash, no assertion failures reported).
