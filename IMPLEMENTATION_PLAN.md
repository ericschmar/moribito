# Start Screen Implementation Plan

## Overview
Create a new start screen for Moribito that displays on app launch with a clean IntelliJ-style layout showing the app branding and recent connections.

## Requirements
- Two-panel horizontal layout (left and right sections)
- Left panel: Centered "Moribito" title with vibrant color
- Right panel: Recent 3 connections list + "Manage Connections" button
- Custom connection card component with hover effects
- Connection selection capability (no navigation yet)
- IntelliJ design language consistency

## Architecture Analysis

### Current State
- **Navigation**: AppView sealed class defines screens (Configuration, Tree, Record, Query)
- **Current Issue**: App.kt always shows ConfigurationScreen regardless of state.currentView
- **ViewModel**: MainViewModel manages state via StateFlow<AppState>
- **Config**: RootConfig.connections stores all LDAP connections
- **Theme**: Gruvbox dark color scheme with Jewel UI components
- **Layout Pattern**: ConfigurationScreen uses HorizontalSplitPane for two-panel design

### Key Files
```
gui/src/main/kotlin/com/moribito/gui/
├── App.kt                          # Entry point - needs navigation logic
├── viewmodel/
│   ├── MainViewModel.kt            # State management
│   └── AppState.kt                 # State models
├── ui/
│   ├── screens/
│   │   ├── ConfigurationScreen.kt  # Reference for design patterns
│   │   └── StartScreen.kt          # NEW - to be created
│   └── components/
│       └── ConnectionCard.kt       # NEW - to be created
└── theme/
    ├── Color.kt                    # Gruvbox color palette
    └── Type.kt                     # Typography system
```

## Implementation Steps

### Step 1: Update AppState Model
**File**: `gui/src/main/kotlin/com/moribito/gui/viewmodel/AppState.kt`

**Changes**:
```kotlin
sealed class AppView {
    object Start : AppView()        // NEW - Add this
    object Configuration : AppView()
    object Tree : AppView()
    object Record : AppView()
    object Query : AppView()
}

data class AppState(
    val currentView: AppView = AppView.Start,  // Change default from Configuration
    // ... rest unchanged
)
```

**Rationale**: Start screen becomes the initial view when app launches.

---

### Step 2: Create ConnectionCard Component
**File**: `gui/src/main/kotlin/com/moribito/gui/ui/components/ConnectionCard.kt`

**Design Specification**:
```kotlin
@Composable
fun ConnectionCard(
    name: String,
    host: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Visual Design**:
- **Dimensions**: Height 64dp, fillMaxWidth() with 12dp padding
- **Shape**: RoundedCornerShape(8.dp) - standard IntelliJ corner radius
- **Background**:
  - Normal: AppColors.surface
  - Hovered: AppColors.surface.hover() (15% overlay)
  - Selected: Subtle blue border (AppColors.primary)
- **Layout**: Column with 8dp spacing
  - Top: Connection name (AppTypography.titleMedium, AppColors.textPrimary)
  - Bottom: Host (AppMonospace.small, AppColors.textSecondary)
- **Interaction**:
  - Hover state via MutableInteractionSource
  - Clickable with ripple effect removed (custom NoIndication)
  - Cursor changes to pointer on hover

**Implementation Pattern**: Follow ConfigurationScreen's IconButton pattern for hover handling.

---

### Step 3: Add ViewModel Support
**File**: `gui/src/main/kotlin/com/moribito/gui/viewmodel/MainViewModel.kt`

**Add Method**:
```kotlin
/**
 * Returns the most recently used connections.
 * For initial implementation, returns first N connections.
 * TODO: Enhance with actual "recently used" tracking.
 */
fun getRecentConnections(limit: Int = 3): List<ConfigLdapConfig> {
    return config.connections.take(limit)
}
```

**Rationale**: Provides data source for start screen. Simple implementation now, can be enhanced later with timestamp-based tracking.

---

### Step 4: Create StartScreen
**File**: `gui/src/main/kotlin/com/moribito/gui/ui/screens/StartScreen.kt`

**Layout Structure**:
```
HorizontalSplitPane (50/50 split)
├── Left Panel (first)
│   └── Box(contentAlignment = Center, fillMaxSize)
│       └── Text("Moribito", displayLarge, orange)
│
└── Right Panel (second)
    └── Column(fillMaxSize)
        ├── Row(horizontalArrangement = End) [Top-right area]
        │   └── OutlinedButton("Manage Connections")
        ├── Spacer(weight = 1f) [Push content to center]
        ├── Column(horizontalAlignment = CenterHorizontally) [Centered list]
        │   └── recentConnections.forEach { ConnectionCard(...) }
        └── Spacer(weight = 1f) [Push content to center]
```

**Composable Signature**:
```kotlin
@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun StartScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
)
```

**Design Details**:
- **Split ratio**: 50/50 (0.5f) - equal panels
- **Title color**: AppColors.orange (most vibrant "pop" color)
- **Title size**: AppTypography.displayLarge (32sp)
- **List spacing**: 12dp vertical spacing between cards
- **Button**: OutlinedButton to match ConfigurationScreen style
- **Button action**: `onClick = { viewModel.navigateTo(AppView.Configuration) }`

**Connection Selection**:
```kotlin
val recentConnections = viewModel.getRecentConnections(3)
val currentIndex = viewModel.currentConnectionIndex

recentConnections.forEachIndexed { index, conn ->
    ConnectionCard(
        name = conn.name.ifBlank { conn.host },
        host = conn.host,
        isSelected = (index == currentIndex),
        onClick = {
            viewModel.setCurrentConnection(index)
        }
    )
}
```

**Background**: Use Background composable from `com.moribito.gui.ui.components.Background` for consistent app background.

---

### Step 5: Implement Navigation in App.kt
**File**: `gui/src/main/kotlin/com/moribito/gui/App.kt`

**Current Code** (line 72-76):
```kotlin
ConfigurationScreen(
    viewModel = vm,
    ldapConfig = vm.getConfig(),
    connectionState = state.connectionState
)
```

**Replace With**:
```kotlin
Background(modifier = Modifier.fillMaxSize()) {
    when (state.currentView) {
        is AppView.Start -> {
            StartScreen(
                viewModel = vm
            )
        }
        is AppView.Configuration -> {
            ConfigurationScreen(
                viewModel = vm,
                ldapConfig = vm.getConfig(),
                connectionState = state.connectionState
            )
        }
        is AppView.Tree -> {
            // TODO: Implement TreeScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tree View - Not Implemented")
            }
        }
        is AppView.Record -> {
            // TODO: Implement RecordScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Record View - Not Implemented")
            }
        }
        is AppView.Query -> {
            // TODO: Implement QueryScreen
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Query View - Not Implemented")
            }
        }
    }
}
```

**Imports to Add**:
```kotlin
import com.moribito.gui.ui.screens.StartScreen
import com.moribito.gui.viewmodel.AppView
```

**Rationale**: Implements proper screen routing based on ViewModel state. This fixes the existing navigation issue where ViewModel state changes didn't affect the UI.

---

### Step 6: Verify Compilation
**Command**: `./gradlew :gui:compileKotlinJvm`

**Expected Issues**:
- Import statements may need adjustment
- Ensure all new files have proper package declarations
- Verify Jewel UI component imports

**Testing Checklist**:
- [ ] App launches to Start screen
- [ ] "Moribito" title displays in orange, centered on left
- [ ] Recent connections list shows up to 3 connections
- [ ] Connection cards display name and host correctly
- [ ] Hover effect works on connection cards
- [ ] Clicking a connection card selects it (verify via logs or state inspection)
- [ ] "Manage Connections" button navigates to ConfigurationScreen
- [ ] Navigation back to Start screen works (if implemented)

---

## Design Decisions

### Color Choice for Title
**Options Considered**:
- Blue (AppColors.primary) - Brand color but less vibrant
- Orange (AppColors.orange) - Most vibrant, high contrast ✓ **SELECTED**
- Yellow (AppColors.yellow120) - Very bright but can be harsh
- Green (AppColors.green100) - Good but less "pop"

**Decision**: Orange provides the best "pop of color" against the dark Gruvbox background while maintaining readability.

### Split Pane Ratio
**Options Considered**:
- 30/70 (like ConfigurationScreen) - More space for connections
- 40/60 - Balanced but asymmetric
- 50/50 - Equal visual weight ✓ **SELECTED**

**Decision**: 50/50 split gives equal prominence to branding and functionality, creating a balanced, welcoming start screen.

### Recent Connections Logic
**Current Implementation**: Return first 3 from `config.connections.take(3)`

**Future Enhancement**: Track actual usage with timestamps
```kotlin
data class ConnectionUsage(
    val connectionName: String,
    val lastUsed: Instant
)

// Store in config or separate preferences file
fun getRecentConnections(limit: Int = 3): List<ConfigLdapConfig> {
    val usage = loadConnectionUsage() // From preferences
    return usage
        .sortedByDescending { it.lastUsed }
        .take(limit)
        .mapNotNull { usage ->
            config.connections.find { it.name == usage.connectionName }
        }
}
```

### Connection Card Selection Visual
**Initial Implementation**: Basic isSelected parameter with subtle border

**Future Enhancement**: Could add:
- Colored sidebar indicator (like ConfigurationScreen)
- Background tint
- Icon indicator
- Animation on selection

---

## Dependencies

### Existing Dependencies (Already Available)
- Jetpack Compose for Desktop
- Jewel UI framework (IntelliJ components)
- Kotlin Coroutines & Flow
- Koin (Dependency Injection)
- Material3 (for some base components)

### New Component Dependencies
All required components already exist in codebase:
- `org.jetbrains.compose.splitpane.HorizontalSplitPane` ✓
- `com.moribito.gui.ui.components.Background` ✓
- `org.jetbrains.jewel.ui.component.OutlinedButton` ✓
- Theme system (AppColors, AppTypography, AppSpacing) ✓

---

## File Creation Order

1. **AppState.kt** (modify) - Enables other components to reference AppView.Start
2. **ConnectionCard.kt** (create) - Required by StartScreen
3. **MainViewModel.kt** (modify) - Provides data for StartScreen
4. **StartScreen.kt** (create) - Main new screen
5. **App.kt** (modify) - Wires everything together

---

## Testing Strategy

### Manual Testing
1. Launch app - should show Start screen
2. Verify title positioning and color
3. Test connection card hover effects
4. Test connection selection (add logging)
5. Test "Manage Connections" button navigation
6. Verify ConfigurationScreen still works
7. Test with 0, 1, 2, and 3+ connections

### Edge Cases
- **No connections**: Show empty state with message "No connections configured"
- **One connection**: Show single card, still centered
- **Many connections**: Only show first 3
- **Very long connection names**: Test text truncation/wrapping
- **Very long hostnames**: Test monospace text wrapping

---

## Future Enhancements (Out of Scope)

1. **Connection History Tracking**
   - Store last-used timestamp for each connection
   - Sort by recency instead of list order
   - Persist in separate preferences file

2. **Quick Connect**
   - Double-click connection card to connect immediately
   - Show connection status in card

3. **Connection Management from Start**
   - Quick delete button on hover
   - Inline rename functionality
   - Drag-to-reorder

4. **Search/Filter**
   - Search bar for large connection lists
   - Filter by tags or categories

5. **Visual Enhancements**
   - Connection status indicator (green/red dot)
   - Last-used timestamp display
   - Connection type icons (SSL/TLS badges)
   - Smooth animations for card selection
   - Fade-in effect on screen load

6. **Keyboard Navigation**
   - Arrow keys to navigate connections
   - Enter to select
   - Ctrl+N for new connection
   - Escape to deselect

---

## Success Criteria

✓ Start screen displays as default on app launch
✓ Two-panel layout with IntelliJ design language
✓ "Moribito" title centered with orange color
✓ Recent 3 connections shown in custom cards
✓ Connection cards have hover effects
✓ Clicking connection selects it as active
✓ "Manage Connections" button opens ConfigurationScreen
✓ No crashes or compilation errors
✓ Code follows existing patterns and conventions
✓ Proper separation of concerns (UI, ViewModel, State)

---

## Notes

- **Do NOT implement**: Actual connection logic or navigation to Tree/Record screens (per requirements)
- **Keep simple**: Initial implementation should be minimal and functional
- **Follow patterns**: Use existing components and styles from ConfigurationScreen
- **Future-proof**: Structure allows easy enhancement without refactoring
- **IntelliJ style**: Maintain consistency with Jewel UI components and Gruvbox theme

---

## Estimated Complexity

- **AppState modification**: Trivial (2 lines)
- **ConnectionCard component**: Low (similar to existing IconButton pattern)
- **ViewModel method**: Trivial (1 line)
- **StartScreen**: Medium (new screen but follows ConfigurationScreen pattern)
- **App.kt navigation**: Low (straightforward when expression)

**Total**: Medium complexity, straightforward implementation following established patterns.