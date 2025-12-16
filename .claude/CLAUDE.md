# GPUI and gpui-component Development Guide

This document provides essential information for working with GPUI and gpui-component in the Moribito LDAP Browser project.

## Table of Contents
- [GPUI Basics](#gpui-basics)
- [gpui-component Library](#gpui-component-library)
- [Common Patterns](#common-patterns)
- [Component Reference](#component-reference)

---

## GPUI Basics

### Core Concepts

**Context Types**:
- `&mut App` - Global application context (used in non-Render methods)
- `&mut Context<Self>` - Component-specific context (used in `Render::render()`)
- `&mut Window` - Window context

**Render Trait**:
```rust
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // Build UI here
    }
}
```

**State Management**:
- Use `Entity<T>` for shared mutable state
- Access with `.read(cx)` and `.update(cx, |state, cx| { ... })`
- GPUI is reactive - changes to entities trigger re-renders

### Element Methods

Common element modifiers:
- `.w(px(200.0))` - Width
- `.h(px(100.0))` - Height
- `.w_full()` - Full width
- `.h_full()` - Full height
- `.flex_1()` - Flex: 1
- `.p_2()`, `.px_3()`, `.py_4()` - Padding
- `.m_2()`, `.mx_3()`, `.my_4()` - Margin
- `.gap_2()` - Gap between children
- `.bg(color)` - Background color
- `.border_1()`, `.border_b_1()`, `.border_r_1()` - Borders
- `.border_color(color)` - Border color
- `.items_center()`, `.justify_center()` - Flexbox alignment

---

## gpui-component Library

**Official Documentation**: https://longbridge.github.io/gpui-component/docs/components/

### Installation

```toml
[dependencies]
gpui-component = "0.4.0"
gpui-component-assets = "0.4.0"
```

### Initialization

In your main function:
```rust
Application::new().with_assets(Assets).run(|cx: &mut App| {
    gpui_component::init(cx);
    // ... rest of app setup
});
```

### Importing

```rust
use gpui_component::{
    button::Button,
    input::{Input, InputState},
    theme::ActiveTheme,
    h_flex, v_flex,
    Sizable,
};
```

**Important**: Most components are in submodules (e.g., `button::Button`), but layout helpers like `h_flex`, `v_flex`, `div` are at the root level.

---

## Common Patterns

### Theme Access

```rust
let theme = cx.theme();

// Use theme colors
.bg(theme.background)
.text_color(theme.foreground)
.border_color(theme.border)
```

Available theme colors:
- `background`, `foreground`
- `primary`, `secondary`, `success`, `warning`, `danger`, `info`
- `muted`, `muted_foreground`
- `border`, `ring`

### Layout

```rust
// Horizontal flex
h_flex()
    .gap_2()
    .items_center()
    .child(...)
    .child(...)

// Vertical flex
v_flex()
    .gap_4()
    .child(...)

// Conditional children
.when(condition, |this| {
    this.child(...)
})

// Optional children
.when_some(option.as_ref(), |this, value| {
    this.child(text(value))
})
```

---

## Component Reference

### Button

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/button

```rust
use gpui_component::button::Button;

Button::new("button-id")
    .label("Click Me")
    .primary()      // Variant methods
    .secondary()
    .danger()
    .warning()
    .success()
    .info()
    .ghost()
    .link()
    .text()
    .outline()      // Modifier
    .small()        // Size
    .large()
    .xsmall()
    .disabled(true)
    .loading(true)
    .icon(IconName::Check)
    .on_click(|event, window, cx| {
        // Handle click
    })
```

**Important**: Each button needs a unique ID. Variant methods like `.primary()` replace the default styling, while `.outline()` modifies the current variant.

### Input

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/input

```rust
use gpui_component::input::{Input, InputState};

// Create state
let input_state = cx.new(|cx| {
    InputState::new(window, cx)
        .placeholder("Enter text...")
        .default_value("initial value")
});

// Render
Input::new(&input_state)
    .w_full()
    .text_xs()
    .appearance(false)  // Remove default styling

// For password inputs
let mut state = InputState::new(window, cx);
state.set_masked(true, window, cx);

// Read value
let text = input_state.read(cx).text();
```

### Table

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/table

Tables require implementing the `TableDelegate` trait:

```rust
use gpui_component::table::{Column, Table, TableDelegate, TableState};
use once_cell::sync::Lazy;

// Define columns statically
static COLUMNS: Lazy<Vec<Column>> = Lazy::new(|| {
    vec![
        Column::new("id", "Name")
            .width(200.0)
            .sortable()
            .resizable(true),
        Column::new("value", "Value")
            .width(400.0)
            .resizable(true),
    ]
});

struct MyTableDelegate {
    rows: Vec<MyRow>,
}

impl TableDelegate for MyTableDelegate {
    fn columns_count(&self, _cx: &App) -> usize {
        2
    }

    fn rows_count(&self, _cx: &App) -> usize {
        self.rows.len()
    }

    fn column(&self, ix: usize, _cx: &App) -> &Column {
        &COLUMNS[ix]
    }

    fn render_th(&self, col_ix: usize, _window: &mut Window, cx: &mut App) -> AnyElement {
        let theme = cx.theme();
        let col = self.column(col_ix, cx);
        text::text(col.label.clone())
            .text_size(px(12.0))
            .text_color(theme.foreground)
            .into_any_element()
    }

    fn render_td(&self, row_ix: usize, col_ix: usize, _window: &mut Window, cx: &mut App) -> AnyElement {
        let theme = cx.theme();
        let content = /* get cell content */;
        text::text(content)
            .text_size(px(13.0))
            .text_color(theme.foreground)
            .into_any_element()
    }
}

// Create table
let delegate = MyTableDelegate::new(rows);
let table_state = cx.new(|cx| {
    TableState::new(delegate, window, cx)
        .stripe(true)
});

// Render
Table::new(&table_state)
    .w_full()
    .h_full()
```

**Important**: 
- `columns_count`, `rows_count`, `column` take `&App` (not `&mut App`)
- Column definitions should be static (use `Lazy` or store in delegate)
- Must add `once_cell = "1.19"` to dependencies

### Tree

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/tree

```rust
use gpui_component::tree::{tree, TreeItem, TreeState};
use gpui_component::list::ListItem;

// Create tree state
let tree_state = cx.new(|cx| {
    TreeState::new(cx).items(vec![
        TreeItem::new("node1", "Folder")
            .expanded(true)
            .child(TreeItem::new("node1-1", "File 1")),
        TreeItem::new("node2", "File 2"),
    ])
});

// Render
tree(&tree_state, |ix, entry, selected, window, cx| {
    let item = entry.item();
    let is_folder = entry.is_folder();
    let is_expanded = entry.is_expanded();
    
    let icon = if !is_folder {
        IconName::File
    } else if is_expanded {
        IconName::FolderOpen
    } else {
        IconName::Folder
    };
    
    ListItem::new(SharedString::from(format!("item-{}", ix)))
        .selected(selected)
        .pl(px(16.0) * entry.depth() + px(12.0))
        .on_click(move |event, window, cx| {
            // Handle click
        })
        .child(
            h_flex()
                .gap_2()
                .child(Icon::new(icon))
                .child(text::text(item.label.clone()))
        )
})
```

**TreeItem API**:
- `new(id, label)` - Create item
- `child(item)` / `children(items)` - Add children
- `expanded(bool)` - Set initial expansion
- `disabled(bool)` - Disable interaction

**TreeEntry API** (in render closure):
- `item()` - Get TreeItem
- `depth()` - Nesting level
- `is_folder()` - Has children
- `is_expanded()` - Expansion state

### Text

The `text` import brings in both a module and provides access to the `text()` function:

```rust
use gpui_component::text;

// Both of these work:
text("Hello")               // Recommended shorthand
text::text("Hello")         // Explicit module path

// Methods:
text("Hello")
    .text_size(px(14.0))
    .text_color(theme.foreground)
    .font_weight(FontWeight::BOLD)
```

Text size helpers:
- `.text_xs()` - Extra small
- `.text_sm()` - Small
- `.text_base()` - Base
- `.text_lg()` - Large

### Icons

```rust
use gpui_component::{Icon, IconName};

Icon::new(IconName::Check)
    .text_color(theme.success)

Icon::new(IconName::FolderOpen)
    .size_4()
```

Common icons: `Check`, `File`, `Folder`, `FolderOpen`, `Globe`, `Star`, `Trash`, `Edit`, `Plus`, `Minus`

---

## Important Notes

### Module vs Function Imports

**Correct**:
```rust
use gpui_component::text;
text::text("content")
```

**Incorrect**:
```rust
use gpui_component::text;  // This is a module!
text("content")  // Error: expected function, found module
```

### Component IDs

Many components require unique IDs:
- Buttons: `Button::new("unique-id")`
- ListItems: `ListItem::new(SharedString::from("item-1"))`
- Tree nodes: `TreeItem::new("node-id", "label")`

IDs should be unique within their scope to avoid conflicts.

### Stateful Components

Components that maintain state (Input, Table, Tree) require:
1. Create state entity with `cx.new()`
2. Pass entity reference to component
3. Update state through `.update(cx, |state, cx| { ... })`

### Context Usage

- Use `&App` for read-only access to theme, global state
- Use `&mut App` for updates outside of Render
- Use `&mut Context<Self>` in `Render::render()` method
- Cannot call methods that take `&mut App` from within render closures

### Flexbox Layout

gpui-component uses CSS Flexbox model:
- `h_flex()` - Horizontal flex container
- `v_flex()` - Vertical flex container
- `.flex_1()` - Flex: 1 (grow to fill space)
- `.gap_N()` - Gap between children
- `.items_center()` - Align items center
- `.justify_center()` - Justify content center

---

## Troubleshooting

### "expected function, found module `text`"
Use `text::text()` instead of `text()`

### "method `primary` not found for struct `Button`"
Check the gpui-component version. Older versions may have different APIs. Current methods are: `primary()`, `secondary()`, `danger()`, etc.

### "types differ in mutability" for TableDelegate
TableDelegate methods take `&App`, not `&mut App`

### "cannot borrow `*cx` as mutable more than once"
In render closures, you can't call methods that need `&mut Context<Self>`. Use actions or update handlers instead.

### Table not updating
Make sure to recreate TableState when data changes:
```rust
self.table_state.update(cx, |state, cx| {
    *state = TableState::new(new_delegate, window, cx);
});
```

---

## Additional Resources

- **Official Docs**: https://longbridge.github.io/gpui-component/docs/components/
- **GPUI Repository**: https://github.com/zed-industries/gpui
- **gpui-component Repository**: https://github.com/longbridge/gpui-component
- **Examples**: Check existing code in `moribito-rs/crates/gui/src/`

---

## Project-Specific Notes

### Current Architecture

- **AppState**: Global state managed via `SharedAppState` (Arc + RwLock)
- **Views**: Main views implement `Render` trait
- **Components**: Reusable UI components in `src/components/`
- **Theme**: Using Gruvbox Dark theme loaded from JSON

### Dependencies

```toml
gpui = "0.2.2"
gpui-component = "0.4.0"
gpui-component-assets = "0.4.0"
once_cell = "1.19"  # For Lazy static data
```

### Common Imports

```rust
use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    button::Button,
    h_flex,
    input::{Input, InputState},
    text,
    theme::ActiveTheme,
    v_flex,
    Sizable,
};
```
