# Moribito LDAP Browser - Development Guide

This is the main index for development documentation. Detailed guides are organized in the `rules/` directory.

---

## Quick Reference

### Core Documentation

- **[Rust Best Practices](rules/rust-best-practices.md)** - Borrowing, lifetimes, error handling, async patterns, and common Rust pitfalls
- **[GPUI Framework](rules/gpui-framework.md)** - Context types, state management, rendering, event handling, and GPUI-specific patterns
- **[gpui-component Library](rules/gpui-components.md)** - Component library reference for buttons, inputs, tables, trees, and more
- **[Project Architecture](rules/project-architecture.md)** - Moribito-specific patterns, state management, LDAP integration, and conventions

---

## Essential Rust Knowledge

**Common Issues & Solutions**:
- **Borrowing errors**: See [rules/rust-best-practices.md](rules/rust-best-practices.md#borrowing--ownership)
- **Lifetime errors**: See [rules/rust-best-practices.md](rules/rust-best-practices.md#lifetimes)
- **Async/await patterns**: See [rules/rust-best-practices.md](rules/rust-best-practices.md#async-patterns)
- **String handling**: See [rules/rust-best-practices.md](rules/rust-best-practices.md#string-handling)

---

## GPUI Context Types

**Quick Reference**:

| Context | Where Used | Can Create Entities? |
|---------|-----------|---------------------|
| `&mut App` | Init, actions, event handlers | ✅ Yes |
| `&mut Context<Self>` | `Render::render()` | ❌ No (read-only) |
| `&mut Window` | Window operations | ✅ Yes |

**Common Error**: "cannot borrow cx as mutable more than once"
- **Solution**: See [rules/gpui-framework.md](rules/gpui-framework.md#common-context-errors)

---

## Component Library

**Most Used Components**:
- **Button**: [rules/gpui-components.md](rules/gpui-components.md#button-component)
- **Input**: [rules/gpui-components.md](rules/gpui-components.md#input-component)
- **Table**: [rules/gpui-components.md](rules/gpui-components.md#table-component)
- **Tree**: [rules/gpui-components.md](rules/gpui-components.md#tree-component)

**Official Docs**: https://longbridge.github.io/gpui-component/docs/components/

---

## Project Architecture

**Current Architecture**:
- **State Management**: `Arc<RwLock<AppState>>` for global state
- **Views**: `StartView` (connection), `MainView` (browser)
- **Theme**: Gruvbox Dark theme loaded from JSON
- **LDAP Integration**: Async operations with `tokio` and `ldap3`

**See**: [rules/project-architecture.md](rules/project-architecture.md)

---

## Common Patterns

### State Management

```rust
// Global shared state
pub type SharedAppState = Arc<RwLock<AppState>>;

// GPUI entity state
let state = cx.new(|cx| MyState::new());

// Read entity
let value = self.state.read(cx).value;

// Update entity
self.state.update(cx, |state, cx| {
    state.value = 42;
});
```

### Component Creation

```rust
// In initialization (cx is &mut App)
impl MyView {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        let state = cx.new(|cx| MyState::new());
        Self { state }
    }
}

// In render (cx is &mut Context<Self>)
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();
        div().bg(theme.background).child("Hello")
    }
}
```

### Async Operations

```rust
let app_state = self.app_state.clone();

tokio::spawn(async move {
    let result = fetch_data().await;
    
    let mut state = app_state.write().unwrap();
    state.data = result;
});
```

---

## Common Imports

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
```

---

## Troubleshooting

### Build Errors

**"expected function, found module `text`"**
- Use `text::text()` instead of `text()`
- See [rules/gpui-components.md](rules/gpui-components.md#text-component)

**"types differ in mutability" for TableDelegate**
- Delegate methods take `&App`, not `&mut App`
- See [rules/gpui-components.md](rules/gpui-components.md#table-delegate-trait)

**"cannot borrow `*cx` as mutable more than once"**
- Don't pass `cx` to multiple helper methods in render
- See [rules/gpui-framework.md](rules/gpui-framework.md#common-context-errors)

### Runtime Errors

**Lock poisoning**
- Handle `RwLock` errors explicitly
- See [rules/rust-best-practices.md](rules/rust-best-practices.md#mutex-poisoning)

**Table not updating**
- Recreate entire `TableState` when data changes
- See [rules/gpui-components.md](rules/gpui-components.md#updating-table-data)

---

## Dependencies

```toml
[dependencies]
gpui = "0.2.2"
gpui-component = "0.4.0"
gpui-component-assets = "0.4.0"
once_cell = "1.19"
ldap3 = "0.11"
tokio = { version = "1.0", features = ["full"] }
thiserror = "1.0"
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
```

---

## Additional Resources

- **GPUI Repository**: https://github.com/zed-industries/gpui
- **gpui-component Repository**: https://github.com/longbridge/gpui-component
- **gpui-component Docs**: https://longbridge.github.io/gpui-component/docs/components/
- **Rust Book**: https://doc.rust-lang.org/book/
- **Async Book**: https://rust-lang.github.io/async-book/

---

## Contributing Guidelines

1. **Follow Rust conventions**: Use `rustfmt` and `clippy`
2. **Document public APIs**: Use `///` doc comments
3. **Handle errors explicitly**: No silent `.unwrap()` calls
4. **Test your code**: Add unit tests for new functionality
5. **Update documentation**: Keep rule files in sync with code changes

---

*For detailed information on any topic, see the corresponding file in the `rules/` directory.*
