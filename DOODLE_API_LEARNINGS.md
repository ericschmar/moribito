# Doodle UI Framework API Learnings

**Date:** December 5, 2025
**Version:** Doodle 0.9.1
**Purpose:** Document key learnings about Doodle API for Moribito GUI implementation

---

## Executive Summary

Doodle is a pure Kotlin UI framework that differs significantly from traditional UI frameworks. It uses:
- **Canvas-based rendering** instead of DOM manipulation
- **Constraint-based layouts** instead of traditional layout managers
- **Behavior modules** for component rendering
- **Dependency injection** via Kodein
- **Coroutines** for async operations

Key insight: Doodle requires a different mental model than HTML/CSS, Swing, or even Jetpack Compose.

---

## 1. Application Structure

### What I Initially Coded (WRONG)
```kotlin
class MoribitoGuiApp(...) : Application {
    override fun run(display: Display) {
        // Setup code here
    }
}
```

### Correct Doodle Pattern
```kotlin
class MoribitoGuiApp(
    display: Display,
    fonts: FontLoader,
    uiDispatcher: CoroutineDispatcher
) : Application {
    init {
        // All setup happens in init, not run()
        display += view {
            // View setup
        }
    }

    override fun shutdown() {
        // Cleanup only
    }
}
```

**Key Differences:**
- ❌ No `run()` method - everything goes in `init`
- ✅ Application is fully initialized at constructor time
- ✅ Dependencies injected via constructor (Kodein)
- ✅ `shutdown()` is for cleanup only

### Launching Applications
```kotlin
fun main() {
    application(modules = listOf(
        PointerModule,           // Required for mouse/touch events
        FontModule,              // Required for FontLoader
        basicLabelBehavior(),    // Required for Label rendering
        nativeTextFieldBehavior(), // Required for TextField
        basicButtonBehavior()    // Required for PushButton
    )) {
        // Inject dependencies
        MoribitoGuiApp(
            display = instance(),
            fonts = instance(),
            uiDispatcher = instance()
        )
    }
}
```

**Critical:** Must include behavior modules or controls won't render!

---

## 2. Layout System

### What I Initially Coded (WRONG)
```kotlin
layout = VerticalLayout(spacing = 10.0)
layout = HorizontalLayout(spacing = 8.0)
```

### Correct Doodle Pattern
Doodle uses **ConstraintLayout** exclusively for fine-grained positioning:

```kotlin
container.layout = constrain(child1, child2, child3) { c1, c2, c3 ->
    // Top-to-bottom vertical stacking
    c1.top eq parent.top
    c1.left eq parent.left

    c2.top eq c1.bottom + 10  // spacing
    c2.left eq c1.left

    c3.top eq c2.bottom + 10
    c3.left eq c1.left
}
```

**Key Constraint Operations:**
- `eq` - equals (NOT `=`)
- `lessEq` - less than or equal
- `greaterEq` - greater than or equal
- Arithmetic: `+`, `-`, `*`, `/`

**Common Patterns:**

**Fill parent:**
```kotlin
display.layout = constrain(display.first(), fill)
// With insets
display.layout = constrain(display.first(), fill(insets = Insets(20.0)))
```

**Center alignment:**
```kotlin
view.layout = constrain(child) { c ->
    c.center eq parent.center
}
```

**Relative positioning:**
```kotlin
view.layout = constrain(header, content, footer) { h, c, f ->
    h.top eq parent.top
    h.height eq 60

    c.top eq h.bottom + spacing
    c.bottom eq f.top - spacing

    f.bottom eq parent.bottom
    f.height eq 40
}
```

**For grid layouts:** Use `GridPanel` instead
```kotlin
val grid = GridPanel().apply {
    add(label, col = 0, row = 0)
    add(field, col = 1, row = 0)
    verticalSpacing = { 10.0 }
}
```

---

## 3. View Hierarchy & Children

### What I Initially Coded (WRONG)
```kotlin
container.children += child  // children is protected!
container.children.clear()   // Can't access from outside
```

### Correct Doodle Pattern

**Inside a View subclass:**
```kotlin
class CustomView : View() {
    init {
        children += Label("Text")  // ✅ Works inside the class
        children += PushButton("Button")
    }
}
```

**Using View DSL:**
```kotlin
val container = view {
    +Label("my label")  // Unary plus operator
    +PushButton("my button")
}
```

**Using Display:**
```kotlin
display += view { /* ... */ }  // plusAssign operator
```

**Why Protected?**
Doodle encourages **encapsulation**. Instead of exposing raw children:

```kotlin
class VSplitPanel : View() {
    var left: View? = null
        set(new) {
            field?.let { children -= it }  // Remove old
            field = new
            new?.let { children += it }    // Add new
        }

    var right: View? = null
        set(new) { /* similar */ }
}

// Usage
splitPanel.left = myLeftView   // Clean API
// NOT: splitPanel.children += myLeftView
```

---

## 4. Color API

### What I Initially Coded (WRONG)
```kotlin
Color(0x0066CCu)  // Wrong constructor
color.redFloat    // Doesn't exist
color.greenFloat  // Doesn't exist
Color(red = ..., green = ..., blue = ..., alpha = 1f)  // Wrong signature
```

### Correct Doodle Pattern

**Creating colors:**
```kotlin
import io.nacular.doodle.drawing.Color

// RGB with hex
Color.rgb(0x0066CCu)
Color.rgb(0xFF0000u)

// RGB with components (0-255)
Color.rgb(red = 255, green = 0, blue = 0)

// With alpha (0.0 - 1.0)
Color.rgb(red = 255, green = 0, blue = 0, alpha = 0.5)

// OkLCh (perceptually uniform)
Color.oklch(lightness = 0.5, chroma = 0.4, hue = 0.degrees)
```

**Accessing color components:**
```kotlin
val color = Color.rgb(0xFF0000u)
val rgb = color.rgb      // Int value
val red = (rgb shr 16) and 0xFF
val green = (rgb shr 8) and 0xFF
val blue = rgb and 0xFF
```

**Gradients:**
```kotlin
import io.nacular.doodle.drawing.LinearGradientPaint
import io.nacular.doodle.geometry.Point

val gradient = LinearGradientPaint(
    color1 = Color.Blue,
    color2 = Color.Red,
    start = Point(0, 0),
    end = Point(100, 0)
)

// Use in render
canvas.rect(bounds.atOrigin, gradient)
```

---

## 5. Font Loading

### What I Initially Coded (WRONG)
```kotlin
fun body(fontLoader: FontLoader): Font? {
    return fontLoader {  // Not suspending!
        size = 14
        weight = 400
    }
}
```

### Correct Doodle Pattern

FontLoader uses **suspend functions** - must be called in coroutine scope:

```kotlin
class MyApp(
    display: Display,
    fonts: FontLoader,
    uiDispatcher: CoroutineDispatcher
) : Application {

    private var bodyFont: Font? = null

    init {
        // Launch coroutine for async font loading
        CoroutineScope(uiDispatcher).launch {
            bodyFont = fonts {
                family = "Roboto"
                size = 14
                weight = 400
            }

            // Update UI with loaded font
            updateViewsWithFont()
        }
    }
}
```

**Alternative: Load fonts before app starts**
```kotlin
fun main() {
    application {
        val fonts: FontLoader = instance()

        // Load fonts first
        val myFonts = runBlocking {
            Fonts(
                body = fonts { size = 14 },
                heading = fonts { size = 18; weight = 700 }
            )
        }

        MyApp(instance(), myFonts)
    }
}
```

**Requires:** `FontModule` in application modules

---

## 6. Canvas Rendering

### What I Initially Coded (WRONG)
```kotlin
override fun render(canvas: Canvas) {
    canvas.circle(Circle(6.0, 6.0, 5.0), color)  // Wrong Circle constructor
}
```

### Correct Doodle Pattern

**Circle constructor:**
```kotlin
Circle(x = 50.0, y = 50.0, radius = 25.0)  // Named params
// OR
Circle(center = Point(50.0, 50.0), radius = 25.0)
```

**Drawing on canvas:**
```kotlin
override fun render(canvas: Canvas) {
    // Rectangle
    canvas.rect(
        rectangle = bounds.atOrigin,  // Local coordinates!
        fill = Color.Blue.paint
    )

    // Circle
    canvas.circle(
        circle = Circle(x = 50.0, y = 50.0, radius = 25.0),
        fill = Color.Red.paint,
        stroke = Stroke(color = Color.Black, thickness = 2.0)
    )

    // Path
    val path = path {
        moveTo(Point(0, 0))
        lineTo(Point(100, 50))
        close()
    }
    canvas.path(path, fill = Color.Green.paint)
}
```

**CRITICAL: `bounds.atOrigin`**
```kotlin
bounds.atOrigin  // Rectangle with same size but at (0,0)
// For a 100x50 view:
bounds           // Rectangle(x=10, y=20, width=100, height=50)
bounds.atOrigin  // Rectangle(x=0, y=0, width=100, height=50)
```

Always use `bounds.atOrigin` for drawing within a View's local coordinate system.

**Using View DSL:**
```kotlin
val customView = view {
    size = Size(200, 100)
    render = {
        // 'this' is the Canvas
        rect(bounds.atOrigin, Color.Blue.paint)
        circle(Circle(x = 50, y = 50, radius = 20), Color.Red.paint)
    }
}
```

---

## 7. Controls & Components

### Labels

```kotlin
import io.nacular.doodle.controls.text.Label

val label = Label("Some Text").apply {
    font = myFont
    foregroundColor = Color.Black
    wrapsWords = true
}
```

**Requires:** `basicLabelBehavior()` module

### TextField

```kotlin
import io.nacular.doodle.controls.text.TextField

val textField = TextField().apply {
    text = "initial value"
    mask = '*'  // For password fields
    fitText = setOf(Width, Height)
}

// Listen to changes
textField.changed += { _, old, new ->
    println("Text changed from '$old' to '$new'")
}
```

**Requires:** `nativeTextFieldBehavior()` module

### PushButton

```kotlin
import io.nacular.doodle.controls.buttons.PushButton

val button = PushButton("Click Me").apply {
    fired += {
        // Handle click
    }
}
```

**Requires:**
- `basicButtonBehavior()` or theme
- `PointerModule` for events

### Creating Custom Components

```kotlin
class StatusIndicator(
    private val status: ConnectionState
) : View() {

    private val dot = view {
        size = Size(12, 12)
        render = {
            val color = when (status) {
                Connected -> Color.Green
                Disconnected -> Color.Red
                else -> Color.Yellow
            }
            circle(Circle(6, 6, 5), color.paint)
        }
    }

    private val label = Label(status.toString())

    init {
        children += dot
        children += label

        layout = constrain(dot, label) { d, l ->
            d.left eq parent.left
            d.centerY eq parent.centerY

            l.left eq d.right + 8
            l.centerY eq parent.centerY
        }
    }
}
```

---

## 8. Event Handling

### Pointer Events

```kotlin
import io.nacular.doodle.event.PointerListener

view.pointerChanged += object : PointerListener {
    override fun pressed(event: PointerEvent) {
        // Handle click
    }

    override fun released(event: PointerEvent) {
        // Handle release
    }

    override fun entered(event: PointerEvent) {
        // Handle hover
    }
}
```

**Requires:** `PointerModule`

### Keyboard Events

```kotlin
import io.nacular.doodle.event.KeyListener
import io.nacular.doodle.event.KeyText

view.keyChanged += object : KeyListener {
    override fun pressed(event: KeyEvent) {
        when (event.key) {
            KeyText(code = "Enter") -> handleEnter()
            else -> {}
        }
    }
}
```

### Property Changes

```kotlin
// TextField changes
textField.changed += { _, oldValue, newValue ->
    println("Changed from $oldValue to $newValue")
}

// Focus changes
view.focusChanged += { _, _, hasFocus ->
    if (hasFocus) {
        // Handle focus gained
    }
}
```

---

## 9. Common Patterns

### Modal Dialog Pattern

```kotlin
class ModalDialog(
    private val modalManager: ModalManager
) : View() {

    suspend fun show(): Boolean = suspendCoroutine { continuation ->
        val overlay = view {
            size = display.size
            backgroundColor = Color.Black.opacity(0.5)

            +dialogContent
        }

        modalManager.display(overlay)

        okButton.fired += {
            modalManager.hide(overlay)
            continuation.resume(true)
        }

        cancelButton.fired += {
            modalManager.hide(overlay)
            continuation.resume(false)
        }
    }
}
```

### Responsive Layout Pattern

```kotlin
class ResponsiveView : View() {
    init {
        sizeChanged += { _, _, _ ->
            updateLayout()
        }
    }

    private fun updateLayout() {
        layout = when {
            width < 600 -> createMobileLayout()
            width < 1200 -> createTabletLayout()
            else -> createDesktopLayout()
        }
    }
}
```

### Theme Pattern

```kotlin
class Theme(fonts: Fonts, colors: Colors) {
    val primaryButton: (PushButton) -> Unit = { button ->
        button.apply {
            font = fonts.body
            backgroundColor = colors.primary
            foregroundColor = colors.onPrimary
        }
    }

    val secondaryButton: (PushButton) -> Unit = { ... }
}

// Usage
theme.primaryButton(myButton)
```

---

## 10. Key Differences from Other Frameworks

| Aspect | Traditional UI | Doodle |
|--------|---------------|--------|
| **Rendering** | DOM/Native widgets | Canvas-based |
| **Layouts** | FlexBox, Grid, Managers | Constraint-based |
| **Children** | Public property | Protected (encapsulation) |
| **Styling** | CSS/Style objects | Direct property setting |
| **Events** | addEventListener | `+=` operator |
| **Async** | Callbacks/Promises | Kotlin Coroutines |
| **DI** | Manual/Framework | Kodein built-in |
| **Behaviors** | Built-in | Module-based |

---

## 11. Common Mistakes & Solutions

### Mistake 1: Accessing children from outside
```kotlin
// ❌ WRONG
container.children += child

// ✅ CORRECT
class Container : View() {
    fun addChild(child: View) {
        children += child
    }
}
```

### Mistake 2: Using wrong operators in constraints
```kotlin
// ❌ WRONG
layout = constrain(view) { v ->
    v.width = 100  // Assignment operator
}

// ✅ CORRECT
layout = constrain(view) { v ->
    v.width eq 100  // eq operator
}
```

### Mistake 3: Forgetting behavior modules
```kotlin
// ❌ WRONG - Label won't render!
application {
    MyApp(instance())
}

// ✅ CORRECT
application(modules = listOf(basicLabelBehavior())) {
    MyApp(instance())
}
```

### Mistake 4: Suspending font loading
```kotlin
// ❌ WRONG
val font = fontLoader { size = 14 }  // Not in coroutine!

// ✅ CORRECT
val font = runBlocking {
    fontLoader { size = 14 }
}
// OR
CoroutineScope(dispatcher).launch {
    val font = fontLoader { size = 14 }
}
```

### Mistake 5: Wrong coordinate system
```kotlin
// ❌ WRONG - Uses parent coordinates
render = {
    rect(bounds, color.paint)
}

// ✅ CORRECT - Uses local coordinates
render = {
    rect(bounds.atOrigin, color.paint)
}
```

---

## 12. Migration Checklist

When converting code to Doodle:

- [ ] Change `Application.run()` to `init` block
- [ ] Add required modules: PointerModule, FontModule, behaviors
- [ ] Replace VerticalLayout/HorizontalLayout with `constrain()`
- [ ] Change `children.add()` to `children +=` (inside View class)
- [ ] Update Color creation to `Color.rgb()`
- [ ] Make font loading suspending with coroutines
- [ ] Fix Circle constructor: `Circle(x, y, radius)`
- [ ] Use `bounds.atOrigin` for rendering
- [ ] Replace `=` with `eq` in constraints
- [ ] Inject dependencies via constructor
- [ ] Add behavior modules for all controls

---

## 13. Useful Resources

- **Main Docs:** https://nacular.github.io/doodle/
- **API Reference:** https://nacular.github.io/doodle-api/
- **GitHub:** https://github.com/nacular/doodle
- **Tutorials:** https://github.com/nacular/doodle-tutorials
- **Examples:**
  - Calculator: https://github.com/nacular/doodle-tutorials/tree/master/Calculator
  - Contacts: https://nacular.github.io/doodle-tutorials/docs/contacts

---

## Conclusion

Doodle requires thinking in:
1. **Constraints** not boxes
2. **Behaviors** not built-in rendering
3. **Encapsulation** not direct access
4. **Coroutines** not callbacks
5. **Canvas** not DOM

Once you understand these paradigms, Doodle provides powerful, type-safe UI development with excellent cross-platform support.
