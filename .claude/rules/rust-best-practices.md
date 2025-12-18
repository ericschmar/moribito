# Rust Best Practices & Common Patterns

This guide covers Rust-specific knowledge critical for the Moribito LDAP Browser project, with emphasis on borrowing, lifetimes, and common pitfalls.

---

## Borrowing & Ownership

### Core Rules

1. **One mutable reference OR multiple immutable references** - never both simultaneously
2. **References must always be valid** - no dangling references
3. **Data has exactly one owner** - ownership can be transferred (moved)

### Common Borrowing Patterns

**Pass by Reference for Reading**:
```rust
// Good: Borrow for read-only access
fn display_data(data: &AppState) {
    println!("{:?}", data.username);
}

// Bad: Unnecessary ownership transfer
fn display_data(data: AppState) { // Takes ownership, can't use data after!
    println!("{:?}", data.username);
}
```

**Mutable Borrowing**:
```rust
// Good: Clear mutable borrow
fn update_data(data: &mut AppState) {
    data.username = "new_user".to_string();
}

// Common error: Multiple mutable borrows
fn bad_example(data: &mut AppState) {
    let ref1 = &mut data.username; // First mutable borrow
    let ref2 = &mut data.username; // ERROR: Second mutable borrow!
}
```

### Borrowing in GPUI Contexts

**Context Borrowing Rules**:
```rust
// In Render::render() - use &mut Context<Self>
impl Render for MyView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // cx is &mut Context<Self> here
        let theme = cx.theme(); // OK: read-only method
        
        // ERROR: Can't call &mut App methods directly
        // self.state.update(cx, ...); // Won't compile!
        
        div().child(...)
    }
}

// Outside Render - use &mut App
fn init(cx: &mut App) {
    // cx is &mut App here
    cx.new(|cx| MyView::new(cx)) // OK: can create entities
}
```

**Entity Updates**:
```rust
// Good: Update through entity reference
self.state.update(cx, |state, cx| {
    state.value = 42;
    state.needs_refresh = true;
});

// Bad: Trying to mutate directly
let state = self.state.read(cx);
state.value = 42; // ERROR: read() returns immutable reference
```

### Closure Borrowing

**Move Semantics in Closures**:
```rust
// Without move: Borrows from environment
let id = "button-1";
Button::new(id).on_click(|event, window, cx| {
    // Borrows id - won't work if id doesn't live long enough
});

// With move: Transfers ownership
let id = "button-1".to_string();
Button::new(&id).on_click(move |event, window, cx| {
    // Moves id into closure - id now owned by closure
    println!("{}", id); // OK: id is owned
});
```

**Cloning for Multiple Closures**:
```rust
// Good: Clone for each closure that needs ownership
let state = self.app_state.clone(); // Arc::clone is cheap
Button::new("btn1").on_click(move |_, _, _| {
    let data = state.read().unwrap();
    // Use data...
});

let state2 = self.app_state.clone();
Button::new("btn2").on_click(move |_, _, _| {
    let data = state2.read().unwrap();
    // Use data...
});
```

---

## Lifetimes

### Basic Lifetime Syntax

**Lifetime Annotations**:
```rust
// Function returning a reference - must specify lifetime
fn first<'a>(items: &'a [String]) -> &'a str {
    &items[0] // Returned reference lives as long as input
}

// Multiple lifetimes
fn longer<'a, 'b>(x: &'a str, y: &'b str) -> &'a str {
    if x.len() > y.len() { x } else { x }
}
```

**Struct with Lifetimes**:
```rust
// Good: Explicit lifetime for borrowed data
struct Config<'a> {
    host: &'a str,
    port: u16,
}

// Better: Own the data instead
struct Config {
    host: String, // Owned - no lifetime needed
    port: u16,
}
```

### Common Lifetime Errors

**Returning References to Local Data**:
```rust
// ERROR: Returns reference to local variable
fn bad() -> &str {
    let s = String::from("hello");
    &s // ERROR: s dropped at end of function
}

// Good: Return owned data
fn good() -> String {
    String::from("hello")
}
```

**Static Lifetime**:
```rust
// 'static means "lives for entire program"
const APP_NAME: &'static str = "Moribito";

// String literals are 'static
let name: &'static str = "LDAP Browser";

// Don't use 'static unless you mean it!
// Bad: Forcing 'static when not needed
fn bad(s: &'static str) { } // Too restrictive!

// Good: Use generic lifetime
fn good<'a>(s: &'a str) { }
```

---

## Error Handling

### Result and Option

**Prefer ? Operator**:
```rust
// Good: Clean error propagation
fn connect() -> Result<Connection, Error> {
    let config = load_config()?; // Propagates error
    let conn = establish_connection(&config)?;
    Ok(conn)
}

// Bad: Manual match everywhere
fn connect() -> Result<Connection, Error> {
    let config = match load_config() {
        Ok(c) => c,
        Err(e) => return Err(e),
    };
    // Too verbose!
}
```

**Option Handling**:
```rust
// Good: Use combinators
let value = optional_value
    .map(|v| v * 2)
    .unwrap_or(0);

// Good: Pattern matching for complex logic
match optional_value {
    Some(v) if v > 100 => handle_large(v),
    Some(v) => handle_small(v),
    None => handle_none(),
}

// Avoid: Unnecessary unwrap
let value = optional_value.unwrap(); // Panics if None!

// Better: Provide default or propagate
let value = optional_value.unwrap_or_default();
```

**Custom Error Types**:
```rust
use thiserror::Error;

#[derive(Error, Debug)]
pub enum LdapError {
    #[error("Connection failed: {0}")]
    ConnectionFailed(String),
    
    #[error("Authentication failed")]
    AuthFailed,
    
    #[error("Query error: {0}")]
    QueryError(#[from] ldap3::LdapError),
}

// Usage
fn connect() -> Result<(), LdapError> {
    let conn = ldap3::LdapConn::new("ldap://server")?; // Auto-converts
    Ok(())
}
```

---

## String Handling

### String Types

**Know Your String Types**:
```rust
// &str - Borrowed string slice (usually use this for parameters)
fn print_name(name: &str) { println!("{}", name); }

// String - Owned, heap-allocated string
fn create_name() -> String { String::from("User") }

// String vs &str conversion
let owned = String::from("hello");
let borrowed: &str = &owned; // Cheap
let owned_again: String = borrowed.to_string(); // Allocates

// Accept both with AsRef
fn flexible(s: impl AsRef<str>) {
    let s = s.as_ref();
    println!("{}", s);
}
flexible("literal");
flexible(String::from("owned"));
```

**Efficient String Building**:
```rust
// Bad: Repeated allocations
let mut result = String::new();
result = result + "a"; // Allocates
result = result + "b"; // Allocates again!

// Good: Use push_str
let mut result = String::new();
result.push_str("a");
result.push_str("b");

// Good: Format macro for one-time construction
let result = format!("{}:{}", host, port);

// Good: Pre-allocate if size known
let mut result = String::with_capacity(100);
```

---

## Collections

### Vec Patterns

**Initialization**:
```rust
// Empty vector
let mut items: Vec<String> = Vec::new();
let mut items = Vec::<String>::new();
let mut items: Vec<String> = vec![];

// With initial values
let items = vec![1, 2, 3];

// Pre-allocate capacity
let mut items = Vec::with_capacity(100);
```

**Iteration**:
```rust
let items = vec![1, 2, 3];

// Borrow each item
for item in &items {
    println!("{}", item);
}

// Mutably borrow each item
for item in &mut items {
    *item += 1;
}

// Take ownership (consumes vector)
for item in items {
    // items moved, can't use after loop
}
```

### HashMap Patterns

**Creation and Access**:
```rust
use std::collections::HashMap;

let mut map = HashMap::new();
map.insert("key", "value");

// Safe access
if let Some(value) = map.get("key") {
    println!("{}", value);
}

// Or with default
let value = map.get("key").unwrap_or(&"default");

// Entry API for updates
map.entry("key")
    .and_modify(|v| *v += 1)
    .or_insert(0);
```

---

## Async Patterns

### Tokio Runtime

**Setup**:
```rust
// In main.rs
#[tokio::main]
async fn main() {
    run_app().await;
}

// Spawn background tasks
tokio::spawn(async {
    // Runs concurrently
    fetch_data().await;
});
```

**Async/Await**:
```rust
// Define async function
async fn fetch_user(id: u32) -> Result<User, Error> {
    let response = http_client.get(id).await?;
    Ok(response.user)
}

// Call async function
async fn example() {
    let user = fetch_user(1).await.unwrap();
}
```

**Blocking Operations**:
```rust
// Don't block async runtime!
async fn bad() {
    std::thread::sleep(Duration::from_secs(1)); // BLOCKS ENTIRE RUNTIME!
}

// Good: Use async sleep
async fn good() {
    tokio::time::sleep(Duration::from_secs(1)).await;
}

// For CPU-intensive work
async fn heavy_work() {
    let result = tokio::task::spawn_blocking(|| {
        // CPU-intensive operation
        compute_something()
    }).await.unwrap();
}
```

---

## Trait Patterns

### Common Derives

**Standard Traits**:
```rust
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct User {
    pub name: String,
    pub id: u32,
}

// Debug - for debugging output
// Clone - for explicit copying
// PartialEq/Eq - for comparisons
// Hash - for use in HashMap/HashSet
// Default - for default values
```

**When NOT to Derive Clone**:
```rust
// Bad: Expensive clone
#[derive(Clone)]
pub struct LargeData {
    buffer: Vec<u8>, // Could be megabytes!
}

// Better: Use Arc for shared ownership
use std::sync::Arc;
pub struct LargeData {
    buffer: Arc<Vec<u8>>, // Cheap to clone
}
```

### Implementing Traits

**Display Trait**:
```rust
use std::fmt;

impl fmt::Display for User {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{} (ID: {})", self.name, self.id)
    }
}
```

**From/Into for Conversions**:
```rust
// Implement From, get Into for free
impl From<&str> for UserId {
    fn from(s: &str) -> Self {
        UserId(s.to_string())
    }
}

// Usage
let id: UserId = "user123".into();
let id = UserId::from("user123");
```

---

## Common Pitfalls

### Cloning vs Moving

```rust
// Unnecessary clone
let data = large_struct.clone(); // Expensive!
process(data);
// If you don't need large_struct anymore, just move it:
process(large_struct);

// Necessary clone (need both)
let data = large_struct.clone();
process(data);
use_original(large_struct); // Still have original
```

### Mutex Poisoning

```rust
use std::sync::{Arc, RwLock};

// Good: Handle lock errors
let data = Arc::new(RwLock::new(State::new()));
match data.write() {
    Ok(mut guard) => {
        guard.update();
    }
    Err(poisoned) => {
        // Lock poisoned - a thread panicked while holding it
        eprintln!("Lock poisoned: {}", poisoned);
    }
}

// Or: Recover from poison
let mut guard = data.write().unwrap_or_else(|poisoned| {
    poisoned.into_inner() // Get data despite poison
});
```

### Iterator Consumption

```rust
let items = vec![1, 2, 3];

// Bad: Consumes iterator
let sum: i32 = items.iter().sum();
let count = items.iter().count(); // ERROR: items.iter() already consumed

// Good: Separate iterators
let sum: i32 = items.iter().sum();
let count = items.iter().count(); // OK: new iterator
```

---

## Memory Management

### Stack vs Heap

**Stack Allocation** (fast, limited size):
```rust
let x = 42; // Stack
let arr = [1, 2, 3]; // Stack (fixed size)
```

**Heap Allocation** (slower, unlimited size):
```rust
let s = String::from("hello"); // Heap
let v = vec![1, 2, 3]; // Heap (dynamic size)
let b = Box::new(large_struct); // Explicit heap allocation
```

### Smart Pointers

**Box<T>** - Single ownership, heap allocation:
```rust
let b = Box::new(5); // Heap-allocated integer
```

**Rc<T>** - Multiple ownership, single-threaded:
```rust
use std::rc::Rc;

let data = Rc::new(vec![1, 2, 3]);
let data2 = data.clone(); // Both point to same data
// Dropped when last reference dropped
```

**Arc<T>** - Multiple ownership, thread-safe:
```rust
use std::sync::Arc;

let data = Arc::new(vec![1, 2, 3]);
let data2 = data.clone(); // Atomic reference counting
// Can share across threads
```

**RefCell<T>** - Interior mutability, runtime borrow checking:
```rust
use std::cell::RefCell;

let data = RefCell::new(vec![1, 2, 3]);
data.borrow_mut().push(4); // Runtime borrow check
```

---

## Testing

### Unit Tests

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_addition() {
        assert_eq!(add(2, 3), 5);
    }

    #[test]
    #[should_panic(expected = "divide by zero")]
    fn test_panic() {
        divide(1, 0);
    }

    #[test]
    fn test_result() -> Result<(), String> {
        let result = risky_operation()?;
        assert_eq!(result, 42);
        Ok(())
    }
}
```

### Integration Tests

```rust
// tests/integration_test.rs
use moribito::ldap;

#[test]
fn test_connection() {
    let conn = ldap::connect("ldap://localhost").unwrap();
    assert!(conn.is_connected());
}
```

---

## Performance Tips

### Avoid Unnecessary Allocations

```rust
// Bad: Allocates String
fn get_message(error: bool) -> String {
    if error {
        "Error".to_string()
    } else {
        "Success".to_string()
    }
}

// Better: Return &str when possible
fn get_message(error: bool) -> &'static str {
    if error { "Error" } else { "Success" }
}
```

### Use iterators

```rust
// Bad: Temporary Vec
let sum: i32 = (0..100).collect::<Vec<_>>().iter().sum();

// Good: Direct iteration
let sum: i32 = (0..100).sum();
```

### Preallocate Collections

```rust
// Bad: Grows incrementally
let mut v = Vec::new();
for i in 0..1000 {
    v.push(i);
}

// Good: Preallocate
let mut v = Vec::with_capacity(1000);
for i in 0..1000 {
    v.push(i);
}
```
