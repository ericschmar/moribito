# GPUI Framework Patterns

Core patterns and best practices for working with GPUI in the Moribito LDAP Browser project.

---

## Context Types

### Understanding GPUI Contexts

GPUI has different context types depending on where you are in the code:

**Context Type Reference**:

| Context Type | Where Used | Common Methods | Can Create Entities? |
|--------------|-----------|----------------|---------------------|
| `&mut App` | Global scope, init functions, actions | `.new()`, `.open_window()`, `.quit()` | Yes |
| `&mut Context<Self>` | `Render::render()` method | `.theme()`, `.read()` | No (read-only) |
| `&mut Window` | Window-specific operations | Window management | Yes |
| `&App` | Read-only access (rare) | `.theme()`, read state | No |

### When to Use Each Context

**&mut App - Application Initialization**:
```rust
fn main() {
    Application::new().run(|cx: &mut App| {
        // cx is &mut App here
        gpui_component::init(cx);
        
        cx.activate(true);
        cx.on_action(|action: &Quit, window, cx: &mut App| {
            cx.quit();
        });
        
        cx.open_window(Default::default(), |window, cx| {
            cx.new(|cx| AppView::new(window, cx))
        });
    });
}
```

**&mut Context<Self> - Rendering**:
```rust
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // cx is &mut Context<Self> here
        let theme = cx.theme(); // Read theme
        
        // Can read entities
        let value = self.state.read(cx).value;
        
        // CANNOT create new entities here!
        // let new_state = cx.new(|cx| State::new()); // ERROR!
        
        div()
            .bg(theme.background)
            .child(format!("Value: {}", value))
    }
}
```

**&mut App - Event Handlers**:
```rust
Button::new("my-btn")
    .on_click(|event, window: &mut Window, cx: &mut App| {
        // cx is &mut App in event handlers
        
        // Can create entities
        let new_state = cx.new(|cx| State::new());
        
        // Can update entities
        state.update(cx, |s, cx| {
            s.value = 42;
        });
    })
```

### Common Context Errors

**Error: Cannot borrow cx as mutable more than once**:
```rust
// BAD: Multiple mutable borrows
fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
    div()
        .child(self.render_header(cx))  // Borrow 1
        .child(self.render_body(cx))    // Borrow 2 - ERROR!
}

// GOOD: Return elements, don't pass cx
fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
    let header = self.render_header();
    let body = self.render_body();
    
    div()
        .child(header)
        .child(body)
}

fn render_header(&self) -> impl IntoElement {
    div().child("Header")
}
```

**Error: Method requires &mut App but we have &mut Context<Self>**:
```rust
// BAD: Trying to create entities in render
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let state = cx.new(|cx| State::new()); // ERROR!
        div()
    }
}

// GOOD: Create entities in initialization, not render
impl MyView {
    fn new(window: &mut Window, cx: &mut App) -> Self {
        let state = cx.new(|cx| State::new()); // OK - cx is &mut App
        Self { state }
    }
}

impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // Just read the state
        let value = self.state.read(cx).value;
        div().child(format!("{}", value))
    }
}
```

---

## State Management

### Entity Pattern

**Creating Entities**:
```rust
// In initialization (where cx is &mut App)
pub struct MyView {
    state: Entity<MyState>,
}

impl MyView {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        let state = cx.new(|cx| MyState {
            value: 0,
            items: vec![],
        });
        
        Self { state }
    }
}
```

**Reading Entity State**:
```rust
// Immutable read
let state = self.state.read(cx);
println!("Value: {}", state.value);
// state is automatically dropped here

// Don't do this:
let state = self.state.read(cx);
let value = state.value;
drop(state); // Unnecessary - auto-dropped
```

**Updating Entity State**:
```rust
// Mutable update
self.state.update(cx, |state, cx| {
    state.value += 1;
    state.items.push("new item".to_string());
    
    // Can trigger other updates inside
    other_state.update(cx, |other, cx| {
        other.sync();
    });
});
```

**Sharing Entities Between Components**:
```rust
pub struct ParentView {
    shared_state: Entity<SharedState>,
    child1: Entity<ChildView>,
    child2: Entity<ChildView>,
}

impl ParentView {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        let shared_state = cx.new(|cx| SharedState::new());
        
        // Clone the entity (cheap - just a reference)
        let child1 = cx.new(|cx| ChildView::new(shared_state.clone(), window, cx));
        let child2 = cx.new(|cx| ChildView::new(shared_state.clone(), window, cx));
        
        Self { shared_state, child1, child2 }
    }
}

pub struct ChildView {
    shared: Entity<SharedState>,
}

impl ChildView {
    pub fn new(shared: Entity<SharedState>, window: &mut Window, cx: &mut App) -> Self {
        Self { shared }
    }
}
```

### Global State with Arc + RwLock

**For state shared across GPUI boundary**:
```rust
use std::sync::{Arc, RwLock};

pub type SharedAppState = Arc<RwLock<AppState>>;

#[derive(Default)]
pub struct AppState {
    pub username: String,
    pub connection_status: ConnectionStatus,
}

// Create shared state
let app_state = Arc::new(RwLock::new(AppState::default()));

// Read
let state = app_state.read().unwrap();
println!("User: {}", state.username);

// Write
let mut state = app_state.write().unwrap();
state.username = "admin".to_string();

// Clone for closures (Arc::clone is cheap)
let state = app_state.clone();
Button::new("btn").on_click(move |_, _, _| {
    let mut s = state.write().unwrap();
    s.username = "new_user".to_string();
});
```

**When to use Entity vs Arc<RwLock>**:

- **Entity**: GPUI-managed state, automatic reactivity, use when possible
- **Arc<RwLock>**: Cross-thread state, external library state, non-GPUI code

---

## Render Trait

### Basic Pattern

```rust
use gpui::prelude::*;
use gpui::*;

pub struct MyView {
    // State fields
    count: usize,
    state: Entity<MyState>,
}

impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // Build and return UI tree
        div()
            .flex()
            .flex_col()
            .child("Hello, World!")
            .child(format!("Count: {}", self.count))
    }
}
```

### Conditional Rendering

**Using .when()**:
```rust
div()
    .child("Always visible")
    .when(self.show_details, |this| {
        this.child("Details panel")
    })
    .when(!self.is_connected, |this| {
        this.child("Disconnected warning")
    })
```

**Using .when_some()**:
```rust
div()
    .when_some(self.error_message.as_ref(), |this, error| {
        this.child(
            div()
                .bg(rgb(0xff0000))
                .child(error.clone())
        )
    })
```

**Pattern Matching**:
```rust
let content = match &self.view_state {
    ViewState::Loading => div().child("Loading..."),
    ViewState::Error(err) => div().child(format!("Error: {}", err)),
    ViewState::Loaded(data) => div().child(format!("Data: {}", data)),
};

div().child(content)
```

### List Rendering

**Static Lists**:
```rust
let items = vec!["Item 1", "Item 2", "Item 3"];

div()
    .flex()
    .flex_col()
    .children(items.iter().map(|item| {
        div().child(*item)
    }))
```

**Dynamic Lists with State**:
```rust
impl Render for ListView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let items = self.items.read(cx).clone();
        
        v_flex()
            .gap_2()
            .children(items.iter().enumerate().map(|(i, item)| {
                self.render_item(i, item)
            }))
    }
}

impl ListView {
    fn render_item(&self, index: usize, item: &Item) -> impl IntoElement {
        div()
            .id(format!("item-{}", index))
            .child(item.name.clone())
    }
}
```

---

## Element Building

### Layout Helpers

**Flexbox**:
```rust
use gpui_component::{h_flex, v_flex};

// Horizontal layout
h_flex()
    .gap_2()
    .items_center()
    .child("Left")
    .child("Center")
    .child("Right")

// Vertical layout
v_flex()
    .gap_4()
    .w_full()
    .child("Top")
    .child("Middle")
    .child("Bottom")

// Nested layouts
v_flex()
    .child(
        h_flex()
            .gap_2()
            .child("Row 1 - Col 1")
            .child("Row 1 - Col 2")
    )
    .child(
        h_flex()
            .gap_2()
            .child("Row 2 - Col 1")
            .child("Row 2 - Col 2")
    )
```

### Sizing

**Fixed Sizes**:
```rust
div()
    .w(px(200.0))
    .h(px(100.0))
    .child("Fixed size")
```

**Relative Sizes**:
```rust
div()
    .w_full()      // 100% width
    .h_full()      // 100% height
    .flex_1()      // flex: 1
    .child("Flexible")
```

**Min/Max Sizes**:
```rust
div()
    .w_full()
    .min_w(px(200.0))
    .max_w(px(800.0))
    .child("Constrained width")
```

### Spacing

**Padding**:
```rust
div()
    .p_2()         // All sides
    .px_4()        // Horizontal (left + right)
    .py_2()        // Vertical (top + bottom)
    .pt_1()        // Top only
    .pr_2()        // Right only
    .pb_3()        // Bottom only
    .pl_4()        // Left only
    .child("Padded")
```

**Margin**:
```rust
div()
    .m_2()         // All sides
    .mx_4()        // Horizontal
    .my_2()        // Vertical
    .mt_1()        // Top only
    .child("Margined")
```

**Gap** (for flex containers):
```rust
h_flex()
    .gap_2()       // Gap between children
    .child("A")
    .child("B")
    .child("C")
```

### Styling

**Colors**:
```rust
use gpui::*;

div()
    .bg(rgb(0x1d2021))           // Hex color
    .bg(rgba(29, 32, 33, 0.8))   // RGBA
    .text_color(rgb(0xebdbb2))
    .border_color(rgb(0x504945))
```

**Theme Colors**:
```rust
use gpui_component::theme::ActiveTheme;

impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = cx.theme();
        
        div()
            .bg(theme.background)
            .text_color(theme.foreground)
            .border_color(theme.border)
            .child("Themed")
    }
}
```

**Borders**:
```rust
div()
    .border_1()              // All sides, 1px
    .border_2()              // All sides, 2px
    .border_t_1()            // Top
    .border_r_1()            // Right
    .border_b_1()            // Bottom
    .border_l_1()            // Left
    .border_color(rgb(0x504945))
    .child("Bordered")
```

**Rounded Corners**:
```rust
div()
    .rounded_md()            // Medium radius
    .rounded_lg()            // Large radius
    .rounded_none()          // No radius
    .child("Rounded")
```

---

## Event Handling

### Click Events

```rust
use gpui_component::button::Button;

Button::new("my-button")
    .label("Click me")
    .on_click(|event, window, cx| {
        // event: &ClickEvent
        // window: &mut Window
        // cx: &mut App
        
        println!("Button clicked!");
    })
```

**With State Updates**:
```rust
let state = self.state.clone();
Button::new("increment")
    .label("+1")
    .on_click(move |_, _, cx| {
        state.update(cx, |s, _| {
            s.count += 1;
        });
    })
```

### Keyboard Events

```rust
div()
    .on_key_down(|event, window, cx| {
        match event.keystroke.key.as_str() {
            "enter" => {
                println!("Enter pressed");
            }
            "escape" => {
                println!("Escape pressed");
            }
            _ => {}
        }
    })
```

### Mouse Events

```rust
div()
    .on_mouse_down(|event, window, cx| {
        println!("Mouse down at {:?}", event.position);
    })
    .on_mouse_up(|event, window, cx| {
        println!("Mouse up");
    })
    .on_hover(|is_hovering, window, cx| {
        println!("Hovering: {}", is_hovering);
    })
```

---

## Actions

### Defining Actions

```rust
use gpui::actions;

actions!(my_app, [Save, Open, Close]);

// With data
#[derive(Clone, PartialEq)]
pub struct Navigate {
    pub path: String,
}

actions!(my_app, [Navigate]);
```

### Registering Action Handlers

```rust
impl MyView {
    pub fn new(window: &mut Window, cx: &mut App) -> Self {
        cx.on_action(|action: &Save, window, cx| {
            println!("Save action triggered");
        });
        
        Self { /* ... */ }
    }
}
```

### Dispatching Actions

```rust
// Dispatch action
cx.dispatch_action(Box::new(Save));

// With data
cx.dispatch_action(Box::new(Navigate {
    path: "/home".to_string(),
}));
```

---

## Window Management

### Opening Windows

```rust
cx.open_window(WindowOptions::default(), |window, cx| {
    cx.new(|cx| MyView::new(window, cx))
});

// With specific options
cx.open_window(
    WindowOptions {
        window_bounds: Some(WindowBounds::Windowed(Bounds {
            origin: Point::new(px(100.0), px(100.0)),
            size: Size::new(px(800.0), px(600.0)),
        })),
        titlebar: Some(TitlebarOptions {
            title: Some("My Window".into()),
            ..Default::default()
        }),
        ..Default::default()
    },
    |window, cx| {
        cx.new(|cx| MyView::new(window, cx))
    },
);
```

### Window Bounds

```rust
// Get current bounds
let bounds = cx.window_bounds();

// Update bounds
cx.set_window_bounds(WindowBounds::Windowed(Bounds {
    origin: Point::new(px(0.0), px(0.0)),
    size: Size::new(px(1024.0), px(768.0)),
}));
```

---

## Performance Tips

### Avoid Unnecessary Clones in Render

```rust
// BAD: Clone entire state in render
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let state = self.state.read(cx).clone(); // Expensive clone!
        div().child(format!("{}", state.value))
    }
}

// GOOD: Read only what you need
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let value = self.state.read(cx).value; // Just copy the number
        div().child(format!("{}", value))
    }
}
```

### Cache Computed Values

```rust
pub struct MyView {
    items: Entity<Vec<Item>>,
    filtered_count: usize, // Cache computed value
}

impl MyView {
    fn update_filter(&mut self, cx: &mut App) {
        self.items.update(cx, |items, _| {
            // Filter items
        });
        
        // Update cached count
        self.filtered_count = self.items.read(cx).len();
    }
}

impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // Use cached value instead of recomputing
        div().child(format!("Showing {} items", self.filtered_count))
    }
}
```

### Minimize Entity Reads

```rust
// BAD: Multiple reads
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .child(format!("Name: {}", self.state.read(cx).name))
            .child(format!("Age: {}", self.state.read(cx).age))
            .child(format!("Email: {}", self.state.read(cx).email))
    }
}

// GOOD: Single read
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let state = self.state.read(cx);
        div()
            .child(format!("Name: {}", state.name))
            .child(format!("Age: {}", state.age))
            .child(format!("Email: {}", state.email))
    }
}
```

---

## Common Patterns

### Modal Dialog Pattern

```rust
pub struct MainView {
    show_modal: bool,
}

impl Render for MainView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        v_flex()
            .child("Main content")
            .when(self.show_modal, |this| {
                this.child(
                    div()
                        .absolute()
                        .top_0()
                        .left_0()
                        .w_full()
                        .h_full()
                        .bg(rgba(0, 0, 0, 0.5))
                        .child("Modal content")
                )
            })
    }
}
```

### Loading State Pattern

```rust
pub enum LoadingState<T> {
    NotStarted,
    Loading,
    Loaded(T),
    Error(String),
}

impl Render for DataView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        match &self.state {
            LoadingState::NotStarted => div().child("Click to load"),
            LoadingState::Loading => div().child("Loading..."),
            LoadingState::Loaded(data) => div().child(format!("Data: {}", data)),
            LoadingState::Error(err) => div().child(format!("Error: {}", err)),
        }
    }
}
```

### Form Input Pattern

```rust
use gpui_component::input::{Input, InputState};

pub struct FormView {
    username: Entity<InputState>,
    password: Entity<InputState>,
}

impl FormView {
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
    
    fn submit(&self, cx: &mut App) {
        let username = self.username.read(cx).text();
        let password = self.password.read(cx).text();
        
        // Process form...
    }
}
```
