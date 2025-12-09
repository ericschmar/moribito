# Moribito: Doodle to Jetpack Compose Desktop Migration

**Date:** December 7, 2025  
**Status:** In Progress  
**Target:** Jetpack Compose for Desktop (Desktop-only, no mobile)

---

## Executive Summary

This document outlines the migration from the Doodle UI framework to Jetpack Compose for Desktop. The current Doodle implementation has significant API compatibility issues that make it impractical to continue. Compose Desktop offers:

- **Official JetBrains support** with excellent documentation
- **Mature, stable APIs** that match documentation
- **Familiar patterns** for Android developers
- **Better IDE integration** with IntelliJ IDEA
- **Active community** and abundant examples

---

## Migration Overview

### What We're Keeping

| Component | Status | Notes |
|-----------|--------|-------|
| **Core Module** | ✅ Keep as-is | LDAP client, config, models work perfectly |
| **ViewModels** | ✅ Keep with minor changes | Already uses Kotlin Flow - perfect for Compose |
| **AppState** | ✅ Keep as-is | State definitions are framework-agnostic |
| **Project Structure** | ✅ Keep as-is | Module organization is good |

### What We're Replacing

| Component | From | To |
|-----------|------|-----|
| **Build Dependencies** | Doodle 0.11.5 | Compose Desktop 1.7.0 |
| **Entry Point** | `application { }` + Kodein DI | `application { Window { } }` |
| **Views** | `View()` subclasses | `@Composable` functions |
| **Layouts** | `constrain()` | `Column`, `Row`, `Box` |
| **Styling** | Direct property assignment | Material 3 Theme |
| **Event Handling** | `fired +=` | `onClick = { }` lambdas |

---

## Phase 1: Configuration Screen (Current Focus)

### Goals
1. ✅ Replace Doodle dependencies with Compose Desktop
2. ✅ Create working application window
3. ✅ Implement Configuration screen with all LDAP fields
4. ✅ Wire up ViewModel for state management
5. ✅ Test LDAP connection functionality

### Implementation Checklist

- [ ] Update `gui/build.gradle.kts` with Compose dependencies
- [ ] Create `Main.kt` with Compose application bootstrap
- [ ] Create `theme/MoribitoTheme.kt` for Material 3 theming
- [ ] Create `ui/screens/ConfigurationScreen.kt`
- [ ] Create `ui/components/` for reusable components
- [ ] Update ViewModel to work with Compose state

---

## Phase 2: Tree Browser (Future)

- Tree view with lazy loading
- Expandable/collapsible nodes
- Node selection with entry loading

## Phase 3: Record View (Future)

- Attribute table display
- Multi-value attribute handling
- Copy to clipboard

## Phase 4: Query View (Future)

- Query input field
- Results table
- Result selection

---

## Technical Details

### Compose Desktop Setup

```kotlin
// gui/build.gradle.kts
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(project(":core"))
}
```

### Application Entry Point Pattern

```kotlin
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Moribito - LDAP Explorer"
    ) {
        MoribitoTheme {
            App()
        }
    }
}
```

### State Management Pattern

The existing ViewModel with `StateFlow` works perfectly with Compose:

```kotlin
@Composable
fun ConfigurationScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    
    // UI renders based on state
    when (state.connectionState) {
        is ConnectionState.Connected -> { /* ... */ }
        is ConnectionState.Connecting -> { /* ... */ }
        // ...
    }
}
```

---

## Migration Benefits

### Doodle Issues Resolved
- ❌ Color API mismatches → ✅ Standard Color class
- ❌ Undocumented constraint layouts → ✅ Column/Row/Box
- ❌ Protected children access → ✅ Composable hierarchy
- ❌ Limited behavior modules → ✅ Rich component library
- ❌ Small community → ✅ Large, active community

### Compose Desktop Advantages
- 🎯 Declarative UI matching modern patterns
- 📚 Excellent documentation
- 🛠 Built-in Material 3 components
- 🔄 Hot reload support
- 🎨 Powerful theming system
- ♿ Built-in accessibility support

---

## File Structure After Migration

```
gui/src/main/kotlin/com/moribito/gui/
├── Main.kt                         # Application entry point
├── App.kt                          # Root composable
├── theme/
│   ├── Color.kt                    # Color definitions
│   ├── Type.kt                     # Typography
│   └── Theme.kt                    # Material theme setup
├── ui/
│   ├── navigation/
│   │   └── Navigation.kt           # Navigation state
│   ├── screens/
│   │   ├── ConfigurationScreen.kt  # LDAP config form
│   │   ├── TreeScreen.kt           # Directory browser
│   │   ├── RecordScreen.kt         # Entry details
│   │   └── QueryScreen.kt          # Custom queries
│   └── components/
│       ├── TabBar.kt               # Navigation tabs
│       ├── StatusBar.kt            # Status display
│       ├── ConnectionIndicator.kt  # Connection status
│       └── FormField.kt            # Reusable form field
└── viewmodel/
    ├── AppState.kt                 # (unchanged)
    └── MainViewModel.kt            # (minor updates)
```

---

## Timeline

| Phase | Duration | Status |
|-------|----------|--------|
| Phase 1: Configuration Screen | 2-3 hours | 🔄 In Progress |
| Phase 2: Tree Browser | 3-4 hours | ⏳ Pending |
| Phase 3: Record View | 2-3 hours | ⏳ Pending |
| Phase 4: Query View | 2-3 hours | ⏳ Pending |
| Testing & Polish | 2-3 hours | ⏳ Pending |

**Total Estimated Time: 11-16 hours**

---

## Notes

- The TUI module is unaffected by this migration
- Core module remains completely unchanged
- ViewModel pattern translates directly to Compose
- State management with Flow is ideal for Compose
