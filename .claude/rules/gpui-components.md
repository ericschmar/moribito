# gpui-component Library Reference

Comprehensive guide to the gpui-component library used in Moribito LDAP Browser.

**Official Documentation**: https://longbridge.github.io/gpui-component/docs/components/

---

## Setup & Installation

### Dependencies

```toml
[dependencies]
gpui = "0.2.2"
gpui-component = "0.4.0"
gpui-component-assets = "0.4.0"
once_cell = "1.19"  # For Lazy static data
```

### Initialization

Must be called once at app startup:

```rust
use gpui::*;
use gpui_component::Assets;

fn main() {
    Application::new()
        .with_assets(Assets)
        .run(|cx: &mut App| {
            gpui_component::init(cx);
            
            // Rest of app setup...
        });
}
```

### Common Imports

```rust
use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    button::Button,
    input::{Input, InputState},
    text,
    theme::ActiveTheme,
    h_flex, v_flex,
    Icon, IconName,
    Sizable,
};
```

---

## Layout Components

### h_flex / v_flex

Horizontal and vertical flex containers.

```rust
use gpui_component::{h_flex, v_flex};

// Horizontal layout
h_flex()
    .gap_2()
    .items_center()
    .justify_between()
    .child("Left")
    .child("Center")
    .child("Right")

// Vertical layout
v_flex()
    .gap_4()
    .w_full()
    .items_start()
    .child("Top")
    .child("Middle")
    .child("Bottom")
```

**Common Methods**:
- `.gap_1()` through `.gap_8()` - Gap between children
- `.items_start()`, `.items_center()`, `.items_end()` - Align items
- `.justify_start()`, `.justify_center()`, `.justify_end()`, `.justify_between()` - Justify content
- `.w_full()`, `.h_full()` - Full width/height
- `.flex_1()` - Flex grow

### div

Generic container element.

```rust
use gpui_component::div;

div()
    .w(px(200.0))
    .h(px(100.0))
    .bg(rgb(0x1d2021))
    .p_4()
    .rounded_md()
    .child("Content")
```

---

## Button Component

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/button

### Basic Usage

```rust
use gpui_component::button::Button;

Button::new("unique-button-id")
    .label("Click Me")
    .on_click(|event, window, cx| {
        println!("Clicked!");
    })
```

### Variants

**Style Variants** (mutually exclusive):
```rust
Button::new("btn").primary()      // Blue/primary color
Button::new("btn").secondary()    // Gray/secondary color
Button::new("btn").danger()       // Red/danger color
Button::new("btn").warning()      // Yellow/warning color
Button::new("btn").success()      // Green/success color
Button::new("btn").info()         // Cyan/info color
Button::new("btn").ghost()        // Transparent background
Button::new("btn").link()         // Link-style (no background/border)
Button::new("btn").text()         // Text-only style
```

### Modifiers

**Outline Variant** (works with any style):
```rust
Button::new("btn")
    .primary()
    .outline()  // Primary outline style
```

**Sizes**:
```rust
Button::new("btn").xsmall()  // Extra small
Button::new("btn").small()   // Small
Button::new("btn")           // Default/medium (no method call)
Button::new("btn").large()   // Large
```

**States**:
```rust
Button::new("btn")
    .disabled(true)    // Disable button
    .loading(true)     // Show loading spinner
```

**Icons**:
```rust
Button::new("btn")
    .icon(IconName::Check)  // Icon only (no label)

Button::new("btn")
    .label("Save")
    .icon(IconName::Save)   // Icon + label
```

### Complete Example

```rust
let state = self.app_state.clone();

Button::new("save-button")
    .label("Save Changes")
    .icon(IconName::Save)
    .primary()
    .on_click(move |event, window, cx| {
        let mut s = state.write().unwrap();
        s.save_data();
        println!("Data saved!");
    })
```

### Important Notes

- **Unique IDs Required**: Each button needs a unique ID in its scope
- **Variant Methods**: `.primary()`, `.secondary()`, etc. replace the default styling
- **Outline Modifier**: `.outline()` modifies the current variant (e.g., outlined primary)
- **Icon-Only Buttons**: Use `.icon()` without `.label()` for icon-only buttons

---

## Input Component

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/input

### Basic Usage

```rust
use gpui_component::input::{Input, InputState};

// Create state (in initialization where cx is &mut App)
let input_state = cx.new(|cx| {
    InputState::new(window, cx)
        .placeholder("Enter text...")
        .default_value("initial value")
});

// Render
Input::new(&input_state)
    .w_full()
    .text_size(px(14.0))
```

### Input State Methods

**Builder Methods** (during creation):
```rust
let state = cx.new(|cx| {
    InputState::new(window, cx)
        .placeholder("Username")
        .default_value("admin")
});
```

**Setter Methods** (after creation):
```rust
let state = cx.new(|cx| InputState::new(window, cx));

// Set masked (password input)
state.update(cx, |state, cx| {
    state.set_masked(true, window, cx);
});

// Set placeholder
state.update(cx, |state, cx| {
    state.set_placeholder("Enter password...");
});

// Set value programmatically
state.update(cx, |state, cx| {
    state.set_text("new value", window, cx);
});
```

### Reading Input Value

```rust
// Read current value
let text = input_state.read(cx).text();
println!("User entered: {}", text);

// In event handler
Button::new("submit")
    .label("Submit")
    .on_click(move |_, _, cx| {
        let value = input_state.read(cx).text();
        process_input(value);
    })
```

### Input Styling

```rust
Input::new(&input_state)
    .w_full()
    .text_xs()              // Extra small text
    .text_sm()              // Small text
    .appearance(false)      // Remove default styling
```

### Password Input Example

```rust
pub struct LoginForm {
    username: Entity<InputState>,
    password: Entity<InputState>,
}

impl LoginForm {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        let username = cx.new(|cx| {
            InputState::new(window, cx)
                .placeholder("Username")
        });
        
        let password = cx.new(|cx| {
            let mut state = InputState::new(window, cx);
            state.set_masked(true, window, cx);
            state.placeholder("Password")
        });
        
        Self { username, password }
    }
}

impl Render for LoginForm {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        v_flex()
            .gap_2()
            .child(Input::new(&self.username).w_full())
            .child(Input::new(&self.password).w_full())
    }
}
```

---

## Table Component

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/table

### Table Delegate Trait

Tables require implementing the `TableDelegate` trait:

```rust
use gpui_component::table::{Column, Table, TableDelegate, TableState};
use once_cell::sync::Lazy;

// Define columns statically
static COLUMNS: Lazy<Vec<Column>> = Lazy::new(|| {
    vec![
        Column::new("name", "Name")
            .width(200.0)
            .sortable()
            .resizable(true),
        Column::new("value", "Value")
            .width(400.0)
            .resizable(true),
        Column::new("type", "Type")
            .width(150.0)
            .sortable(),
    ]
});

#[derive(Clone)]
pub struct DataRow {
    pub name: String,
    pub value: String,
    pub row_type: String,
}

pub struct MyTableDelegate {
    rows: Vec<DataRow>,
}

impl MyTableDelegate {
    pub fn new(rows: Vec<DataRow>) -> Self {
        Self { rows }
    }
}

impl TableDelegate for MyTableDelegate {
    fn columns_count(&self, _cx: &App) -> usize {
        COLUMNS.len()
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
            .font_weight(FontWeight::BOLD)
            .into_any_element()
    }

    fn render_td(&self, row_ix: usize, col_ix: usize, _window: &mut Window, cx: &mut App) -> AnyElement {
        let theme = cx.theme();
        let row = &self.rows[row_ix];
        
        let content = match col_ix {
            0 => &row.name,
            1 => &row.value,
            2 => &row.row_type,
            _ => "",
        };
        
        text::text(content)
            .text_size(px(13.0))
            .text_color(theme.foreground)
            .into_any_element()
    }
}
```

### Creating Table State

```rust
pub struct DataView {
    table_state: Entity<TableState>,
}

impl DataView {
    pub fn new(rows: Vec<DataRow>, window: &mut Window, cx: &mut App) -> Self {
        let delegate = MyTableDelegate::new(rows);
        let table_state = cx.new(|cx| {
            TableState::new(delegate, window, cx)
                .stripe(true)  // Alternating row colors
        });
        
        Self { table_state }
    }
}
```

### Rendering Table

```rust
impl Render for DataView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        Table::new(&self.table_state)
            .w_full()
            .h_full()
    }
}
```

### Updating Table Data

```rust
impl DataView {
    pub fn update_data(&mut self, new_rows: Vec<DataRow>, window: &mut Window, cx: &mut App) {
        let new_delegate = MyTableDelegate::new(new_rows);
        
        self.table_state.update(cx, |state, cx| {
            *state = TableState::new(new_delegate, window, cx)
                .stripe(true);
        });
    }
}
```

### Important Notes

- **&App vs &mut App**: Delegate methods (`columns_count`, `rows_count`, `column`) take `&App`, not `&mut App`
- **Static Columns**: Use `Lazy` for column definitions to avoid recreating them
- **AnyElement**: `render_th` and `render_td` must return `AnyElement` (use `.into_any_element()`)
- **Table Recreation**: To update data, recreate the entire `TableState` with new delegate

---

## Tree Component

**Documentation**: https://longbridge.github.io/gpui-component/docs/components/tree

### Basic Tree Structure

```rust
use gpui_component::tree::{tree, TreeItem, TreeState};
use gpui_component::list::ListItem;

// Create tree state
let tree_state = cx.new(|cx| {
    TreeState::new(cx).items(vec![
        TreeItem::new("root", "Root Folder")
            .expanded(true)
            .child(
                TreeItem::new("folder1", "Subfolder 1")
                    .child(TreeItem::new("file1", "File 1.txt"))
                    .child(TreeItem::new("file2", "File 2.txt"))
            )
            .child(TreeItem::new("file3", "File 3.txt")),
        TreeItem::new("folder2", "Another Folder"),
    ])
});
```

### Rendering Tree

```rust
impl Render for FileExplorer {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        tree(&self.tree_state, |ix, entry, selected, window, cx| {
            let item = entry.item();
            let is_folder = entry.is_folder();
            let is_expanded = entry.is_expanded();
            let depth = entry.depth();
            
            let icon = if !is_folder {
                IconName::File
            } else if is_expanded {
                IconName::FolderOpen
            } else {
                IconName::Folder
            };
            
            ListItem::new(SharedString::from(format!("item-{}", ix)))
                .selected(selected)
                .pl(px(16.0) * depth + px(12.0))  // Indent by depth
                .on_click(move |event, window, cx| {
                    // Handle click
                    println!("Clicked: {}", item.label);
                })
                .child(
                    h_flex()
                        .gap_2()
                        .items_center()
                        .child(Icon::new(icon).size_4())
                        .child(text::text(item.label.clone()))
                )
        })
    }
}
```

### TreeItem API

**Creating Items**:
```rust
TreeItem::new("unique-id", "Display Label")
    .child(TreeItem::new("child-1", "Child 1"))
    .children(vec![
        TreeItem::new("child-2", "Child 2"),
        TreeItem::new("child-3", "Child 3"),
    ])
    .expanded(true)      // Initially expanded
    .disabled(false)     // Enable/disable interaction
```

### TreeEntry API

Methods available in the render closure:

```rust
tree(&tree_state, |ix, entry, selected, window, cx| {
    let item = entry.item();           // Get TreeItem
    let depth = entry.depth();          // Nesting level (0-based)
    let is_folder = entry.is_folder();  // Has children?
    let is_expanded = entry.is_expanded(); // Currently expanded?
    
    // Build list item...
})
```

### Expanding/Collapsing Nodes

Tree expansion is managed internally by clicking on folder items. The `is_expanded` state updates automatically.

### Dynamic Tree Updates

```rust
impl FileExplorer {
    pub fn add_item(&mut self, parent_id: &str, item: TreeItem, cx: &mut App) {
        self.tree_state.update(cx, |state, cx| {
            // Recreate tree with new item
            let mut items = state.items().to_vec();
            // Modify items...
            *state = TreeState::new(cx).items(items);
        });
    }
}
```

---

## Text Component

### Basic Text

```rust
use gpui_component::text;

// Recommended usage
text("Hello, World!")
    .text_size(px(14.0))
    .text_color(theme.foreground)

// Explicit module path
text::text("Hello, World!")
```

### Text Sizing

**Predefined Sizes**:
```rust
text("Text").text_xs()     // Extra small
text("Text").text_sm()     // Small
text("Text").text_base()   // Base size
text("Text").text_lg()     // Large
text("Text").text_xl()     // Extra large
```

**Custom Size**:
```rust
text("Custom").text_size(px(16.0))
```

### Text Styling

```rust
use gpui::FontWeight;

text("Styled Text")
    .text_color(rgb(0xebdbb2))
    .font_weight(FontWeight::BOLD)
    .font_weight(FontWeight::MEDIUM)
    .font_weight(FontWeight::NORMAL)
```

### Common Error

```rust
// ERROR: text is a module!
use gpui_component::text;
text("hello")  // Won't work!

// CORRECT: Use text::text or import differently
use gpui_component::text;
text::text("hello")  // Works!

// OR
use gpui_component::text::text;
text("hello")  // Works!
```

---

## Icon Component

### Basic Icons

```rust
use gpui_component::{Icon, IconName};

Icon::new(IconName::Check)
Icon::new(IconName::File)
Icon::new(IconName::Folder)
Icon::new(IconName::FolderOpen)
```

### Icon Sizing

```rust
Icon::new(IconName::Check)
    .size_3()   // Small
    .size_4()   // Medium
    .size_5()   // Large
    .size_6()   // Extra large
```

### Icon Styling

```rust
let theme = cx.theme();

Icon::new(IconName::Check)
    .text_color(theme.success)
    .size_4()
```

### Common Icons

| IconName | Usage |
|----------|-------|
| `Check` | Checkmark, success |
| `Close`, `X` | Close buttons |
| `File` | File items |
| `Folder`, `FolderOpen` | Folder items |
| `Globe` | Network, internet |
| `Star` | Favorites |
| `Trash` | Delete |
| `Edit`, `Pencil` | Edit actions |
| `Plus` | Add new |
| `Minus` | Remove |
| `Search` | Search functionality |
| `Settings` | Settings/configuration |
| `ChevronDown`, `ChevronRight` | Expand/collapse |

---

## Theme System

### Accessing Theme

```rust
use gpui_component::theme::ActiveTheme;

impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();
        
        div()
            .bg(theme.background)
            .text_color(theme.foreground)
            .border_color(theme.border)
            .child("Themed content")
    }
}
```

### Available Theme Colors

**Base Colors**:
- `theme.background` - Main background
- `theme.foreground` - Main text color
- `theme.border` - Border color
- `theme.ring` - Focus ring color

**Status Colors**:
- `theme.primary` - Primary actions (usually blue)
- `theme.secondary` - Secondary actions (usually gray)
- `theme.success` - Success states (green)
- `theme.warning` - Warning states (yellow)
- `theme.danger` - Error/danger states (red)
- `theme.info` - Info states (cyan)

**Muted Colors**:
- `theme.muted` - Muted background
- `theme.muted_foreground` - Muted text

### Custom Theme

Themes are loaded from JSON files:

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

## ListItem Component

### Basic Usage

```rust
use gpui_component::list::ListItem;

ListItem::new(SharedString::from("item-id"))
    .selected(false)
    .child("Item content")
```

### With Selection

```rust
impl Render for MyList {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let selected_index = self.selected_index;
        
        v_flex()
            .children((0..self.items.len()).map(|i| {
                ListItem::new(SharedString::from(format!("item-{}", i)))
                    .selected(i == selected_index)
                    .on_click(move |_, _, cx| {
                        // Update selection
                    })
                    .child(self.items[i].clone())
            }))
    }
}
```

### Styling ListItem

```rust
ListItem::new(id)
    .selected(is_selected)
    .pl(px(16.0))      // Left padding for indentation
    .py(px(8.0))       // Vertical padding
    .child(content)
```

---

## Sizable Trait

Provides sizing methods for components.

```rust
use gpui_component::Sizable;

Input::new(&state)
    .small()     // Small size
    .large()     // Large size
    .xsmall()    // Extra small
```

Works with: `Button`, `Input`, and other components that support sizing.

---

## Common Patterns

### Form with Multiple Inputs

```rust
pub struct SettingsForm {
    host: Entity<InputState>,
    port: Entity<InputState>,
    username: Entity<InputState>,
    password: Entity<InputState>,
}

impl SettingsForm {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        Self {
            host: cx.new(|cx| {
                InputState::new(window, cx).placeholder("ldap://localhost")
            }),
            port: cx.new(|cx| {
                InputState::new(window, cx).placeholder("389")
            }),
            username: cx.new(|cx| {
                InputState::new(window, cx).placeholder("admin")
            }),
            password: cx.new(|cx| {
                let mut state = InputState::new(window, cx);
                state.set_masked(true, window, cx);
                state.placeholder("Password")
            }),
        }
    }
    
    pub fn get_config(&self, cx: &App) -> ConnectionConfig {
        ConnectionConfig {
            host: self.host.read(cx).text(),
            port: self.port.read(cx).text().parse().unwrap_or(389),
            username: self.username.read(cx).text(),
            password: self.password.read(cx).text(),
        }
    }
}

impl Render for SettingsForm {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        v_flex()
            .gap_3()
            .p_4()
            .child(
                v_flex()
                    .gap_1()
                    .child(text::text("Host").text_sm())
                    .child(Input::new(&self.host).w_full())
            )
            .child(
                v_flex()
                    .gap_1()
                    .child(text::text("Port").text_sm())
                    .child(Input::new(&self.port).w_full())
            )
            .child(
                v_flex()
                    .gap_1()
                    .child(text::text("Username").text_sm())
                    .child(Input::new(&self.username).w_full())
            )
            .child(
                v_flex()
                    .gap_1()
                    .child(text::text("Password").text_sm())
                    .child(Input::new(&self.password).w_full())
            )
    }
}
```

### Data Table with Actions

```rust
impl TableDelegate for ActionTableDelegate {
    // ... column/row methods ...
    
    fn render_td(&self, row_ix: usize, col_ix: usize, window: &mut Window, cx: &mut App) -> AnyElement {
        let theme = cx.theme();
        let row = &self.rows[row_ix];
        
        if col_ix == self.columns_count(cx) - 1 {
            // Actions column
            h_flex()
                .gap_2()
                .child(
                    Button::new(format!("edit-{}", row_ix))
                        .icon(IconName::Edit)
                        .small()
                        .ghost()
                )
                .child(
                    Button::new(format!("delete-{}", row_ix))
                        .icon(IconName::Trash)
                        .small()
                        .danger()
                        .ghost()
                )
                .into_any_element()
        } else {
            // Regular cell
            text::text(/* cell data */)
                .text_color(theme.foreground)
                .into_any_element()
        }
    }
}
```

---

## Troubleshooting

### "expected function, found module `text`"

```rust
// Wrong
use gpui_component::text;
text("hello")

// Right
use gpui_component::text;
text::text("hello")
```

### "method not found" on Button

Check gpui-component version. Ensure you're using 0.4.0 or newer.

### TableDelegate type errors

Delegate methods take `&App`, not `&mut App`:
```rust
fn columns_count(&self, _cx: &App) -> usize { ... }
```

### Table not updating after data change

Recreate the entire TableState:
```rust
self.table_state.update(cx, |state, cx| {
    *state = TableState::new(new_delegate, window, cx);
});
```

### Input state not updating

Make sure to clone the entity for closures:
```rust
let input = self.input.clone();
Button::new("btn").on_click(move |_, _, cx| {
    let value = input.read(cx).text();
});
```
