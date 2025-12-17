# Investigation

## Summary
- The GUI runtime is panicking because `TreeView::new` attempts to `zero-initialize` an `Entity<TreeView>` handle via `unsafe { std::mem::zeroed() }`, which `gpui::Entity` forbids.
- The panic stack mentions `gpui::app::entity_map::Entity` being zero initialized, exactly the line where `TreeView` (and `EntryDetailsTable`) store their entity handles before they are set by `BrowserView`.

## Root Cause
- `gpui::Entity` is not `Zeroable`; its documentation and source show it stores non-trivial references internally, so zeroing leads to invalid state and panic.
- Both `TreeView` and `EntryDetailsTable` store an `Entity<Self>` field initialized via `std::mem::zeroed()` with the promise to populate it later.
- `BrowserView` sets the field after creating the components, but the zeroing already provoked the panic at creation time.

## Affected Components
- `moribito-rs/crates/gui/src/components/tree_view.rs`
- `moribito-rs/crates/gui/src/components/entry_details_table.rs`
- `moribito-rs/crates/gui/src/views/browser_view.rs` (the only place that filled those handles)

## Proposed Solution
- Remove the stored `Entity<Self>` field from each component and obtain the handle on demand via `Context::entity()` inside their rendering logic.
- Update any closures that currently reference the stored field to capture the context-provided handle instead.
- Remove the `update` calls in `BrowserView` that were patching the field, since no field needs populating anymore.

## Next Steps
1. Update `tree_view.rs` and `entry_details_table.rs` to stop zeroing the entity.
2. Adjust render closures to capture `cx.entity()` handles.
3. Simplify their `Clone` implementations accordingly.
4. Remove the redundant entity updates in `BrowserView::new`.
5. Run the GUI crate tests (e.g., `cargo test --package moribito-gui`) to ensure no regressions.

## Implementation Notes
- Removed the `entity` fields from `TreeView` and `EntryDetailsTable`, and now capture their handles directly from `Context::entity()` during rendering.
- `TreeView` clones the handle per tree row so its event handlers can still call `load_children` and emit actions without a stored field.
- `EntryDetailsTable` and `BrowserView` no longer need the manual entity updates, so their clones only track the shared application state and view caching data.

## Tests
- `cargo test --package moribito-gui` *(fails: compiling `gpui_macros` ran into a `SIGBUS` during macro expansion while parsing `syn`; see the rustc backtrace for repeated `syn::stmt::parsing` frames and the `SIGBUS` note in the log).*