# IntelliJ Platform Icons Setup

This document explains how to use IntelliJ Platform icons in your Moribito project using Jewel.

## Dependencies

The IntelliJ Platform icons are already configured in `gui/build.gradle.kts`:

```kotlin
repositories {
    maven("https://www.jetbrains.com/intellij-repository/releases/")
}

dependencies {
    implementation("com.jetbrains.intellij.platform:icons:232.9921.47")
}
```

## Usage with Jewel

### Using AllIconsKeys

Jewel provides `AllIconsKeys` which maps to the IntelliJ Platform's `AllIcons` class. Here's how to use it:

```kotlin
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun ExampleComponent() {
    // Using IntelliJ Platform icons
    Icon(
        key = AllIconsKeys.Actions.Compile,
        contentDescription = "Compile"
    )

    Icon(
        key = AllIconsKeys.General.Settings,
        contentDescription = "Settings"
    )

    Icon(
        key = AllIconsKeys.Nodes.Folder,
        contentDescription = "Folder"
    )
}
```

### Common Icon Categories

The `AllIconsKeys` object contains many categories of icons:

- **Actions**: Common action icons (Play, Stop, Compile, Debug, etc.)
- **General**: General purpose icons (Add, Remove, Settings, etc.)
- **Nodes**: Tree node icons (Folder, Class, Method, etc.)
- **FileTypes**: File type icons (Java, Kotlin, XML, etc.)
- **Debugger**: Debugger-related icons
- **Toolwindows**: Tool window icons
- **Vcs**: Version control icons

### Example: Replacing Octicons with IntelliJ Icons

**Before (using Octicons):**
```kotlin
import compose.icons.Octicons
import compose.icons.octicons.ChevronDown16
import compose.icons.octicons.ChevronRight16
import org.jetbrains.jewel.ui.component.Icon

Icon(
    ChevronDown16,
    contentDescription = "Expand"
)
```

**After (using IntelliJ icons):**
```kotlin
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

Icon(
    key = AllIconsKeys.General.ArrowDown,
    contentDescription = "Expand"
)
```

### Icon Sizes

IntelliJ Platform icons come in standard sizes (16x16, 13x13, 12x12). The `Icon` composable will automatically render them at the appropriate size. You can also scale them using the `modifier` parameter:

```kotlin
Icon(
    key = AllIconsKeys.Actions.Compile,
    contentDescription = "Compile",
    modifier = Modifier.size(24.dp)  // Scale if needed
)
```

### Browsing Available Icons

You can browse all available IntelliJ Platform icons at:
- https://intellij-icons.jetbrains.design/

Or explore them programmatically by examining the `AllIconsKeys` class in your IDE.

## Migration Path

Your project currently uses multiple icon libraries:
- Octicons: `br.com.devsrsouza.compose.icons:octicons`
- Fluent Icons: `io.github.compose-fluent:fluent-icons-extended`
- Material Icons: `compose.materialIconsExtended`

With IntelliJ Platform icons now available, you can:
1. Continue using all libraries for maximum flexibility
2. Gradually migrate to IntelliJ icons for a consistent IDE-like appearance
3. Remove unused icon dependencies to reduce bundle size

## Notes

- IntelliJ icons automatically support light/dark themes when used with Jewel
- Version 232.9921.47 corresponds to IntelliJ Platform 2023.2
- To use newer icon versions, update the version number in `build.gradle.kts` to match your preferred IntelliJ Platform version
