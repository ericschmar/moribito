package com.moribito.gui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Application spacing system based on Gruvbox/IntelliJ patterns.
 * Provides compact spacing tokens for information-dense layouts.
 */
object AppSpacing {
    /** Extra extra extra small - 2dp - Minimal gaps between tightly grouped elements */
    val xxxs: Dp = 2.dp

    /** Extra extra small - 4dp - Very tight spacing for inline elements */
    val xxs: Dp = 4.dp

    /** Extra small - 6dp - Tight spacing between related elements */
    val xs: Dp = 6.dp

    /** Small - 8dp - Compact spacing for dense layouts */
    val sm: Dp = 8.dp

    /** Medium - 12dp - Standard spacing between components */
    val md: Dp = 12.dp

    /** Large - 16dp - Comfortable spacing between sections */
    val lg: Dp = 16.dp

    /** Extra large - 20dp - Spacious padding */
    val xl: Dp = 20.dp

    /** Extra extra large - 24dp - Very spacious padding for main containers */
    val xxl: Dp = 24.dp

    /** Extra extra extra large - 32dp - Maximum spacing for major sections */
    val xxxl: Dp = 32.dp

    // IntelliJ-style specific spacing
    /** Inset spacing - 12dp - For inset content areas */
    val inset: Dp = 12.dp

    /** Panel padding - 8dp - Standard panel padding */
    val panelPadding: Dp = 8.dp

    /** Tool window padding - 4dp - For compact tool windows */
    val toolWindowPadding: Dp = 4.dp
}

/**
 * Component size definitions based on Gruvbox/IntelliJ patterns.
 * Compact design for information-dense interfaces.
 */
object AppSizes {
    // Icon sizes (IntelliJ uses 16dp as base icon size)
    /** Tiny icon size - 12dp - For very compact indicators */
    val iconTiny: Dp = 12.dp

    /** Small icon size - 16dp - Standard icon size (IntelliJ default) */
    val iconSmall: Dp = 16.dp

    /** Medium icon size - 20dp - For toolbar icons */
    val iconMedium: Dp = 20.dp

    /** Large icon size - 24dp - For prominent icons (IntelliJ checkboxes) */
    val iconLarge: Dp = 24.dp

    /** Extra large icon size - 32dp - For very prominent icons */
    val iconExtraLarge: Dp = 32.dp

    // Button heights (IntelliJ uses compact buttons)
    /** Compact button height - 24dp - For very dense toolbars */
    val buttonHeightCompact: Dp = 24.dp

    /** Standard button height - 28dp - Default button size (IntelliJ standard) */
    val buttonHeightStandard: Dp = 28.dp

    /** Large button height - 32dp - For prominent actions */
    val buttonHeightLarge: Dp = 32.dp

    // Input field heights
    /** Compact input height - 24dp - For dense forms */
    val inputHeightCompact: Dp = 24.dp

    /** Standard input height - 28dp - Default input size */
    val inputHeightStandard: Dp = 28.dp

    /** Large input height - 32dp - For prominent inputs */
    val inputHeightLarge: Dp = 32.dp

    // Border widths
    /** Standard border width - 1dp - Subtle borders */
    val borderWidth: Dp = 1.dp

    /** Thick border width - 2dp - More prominent borders */
    val borderWidthThick: Dp = 2.dp

    // Border radius (IntelliJ uses minimal rounding)
    /** No radius - 0dp - Sharp corners (IntelliJ style) */
    val borderRadiusNone: Dp = 0.dp

    /** Small border radius - 2dp - Subtle rounding */
    val borderRadiusSmall: Dp = 2.dp

    /** Standard border radius - 4dp - Default rounding for buttons, inputs */
    val borderRadius: Dp = 4.dp

    /** Large border radius - 6dp - More pronounced rounding for cards */
    val borderRadiusLarge: Dp = 6.dp

    /** Extra large border radius - 8dp - For prominent cards/containers */
    val borderRadiusExtraLarge: Dp = 8.dp

    // Component-specific heights (IntelliJ-style)
    /** Toolbar height - 28dp - For compact toolbars (IntelliJ ActionButton) */
    val toolbarHeight: Dp = 28.dp

    /** Status bar height - 24dp - For bottom status bars */
    val statusBarHeight: Dp = 24.dp

    /** Tab bar height - 28dp - For navigation tabs */
    val tabBarHeight: Dp = 28.dp

    /** Title bar height - 28dp - For window title bars */
    val titleBarHeight: Dp = 28.dp

    /** Sidebar width - 240dp - For tree view sidebar */
    val sidebarWidth: Dp = 240.dp

    /** Sidebar collapsed width - 48dp - For collapsed sidebar */
    val sidebarCollapsedWidth: Dp = 48.dp

    // Divider sizes
    /** Divider thickness - 1dp - For horizontal/vertical dividers */
    val dividerThickness: Dp = 1.dp

    // Minimum touch target
    /** Minimum touch target - 28dp - IntelliJ accessibility standard */
    val minTouchTarget: Dp = 28.dp

    // Checkbox and radio button sizes (from Gruvbox theme)
    /** Checkbox size - 24dp - Standard checkbox (matches Gruvbox iconSize) */
    val checkboxSize: Dp = 24.dp

    /** Radio button size - 24dp - Standard radio button */
    val radioSize: Dp = 24.dp

    /** Checkbox icon-text gap - 4dp - Space between checkbox and label */
    val checkboxTextGap: Dp = 4.dp
}

/**
 * Shape definitions for consistent rounded corners throughout the app.
 */
object AppShapes {
    /** No rounding - For rectangular elements */
    val none = RoundedCornerShape(0.dp)

    /** Small rounding - 2dp - Subtle corners */
    val small = RoundedCornerShape(AppSizes.borderRadiusSmall)

    /** Medium rounding - 4dp - Standard for buttons, inputs */
    val medium = RoundedCornerShape(AppSizes.borderRadius)

    /** Large rounding - 6dp - For cards and panels */
    val large = RoundedCornerShape(AppSizes.borderRadiusLarge)

    /** Extra large rounding - 8dp - For prominent containers */
    val extraLarge = RoundedCornerShape(AppSizes.borderRadiusExtraLarge)

    /** Full rounding - 50% - For circular elements */
    val full = RoundedCornerShape(50)
}

/**
 * Elevation values for consistent shadows (minimal elevation for flat design).
 */
object AppElevation {
    /** No elevation - Flat design */
    val none: Dp = 0.dp

    /** Subtle elevation - 1dp - Very subtle shadow */
    val subtle: Dp = 1.dp

    /** Small elevation - 2dp - Small shadow */
    val small: Dp = 2.dp

    /** Medium elevation - 4dp - Standard shadow */
    val medium: Dp = 4.dp

    /** Large elevation - 8dp - Prominent shadow */
    val large: Dp = 8.dp
}
