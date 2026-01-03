package com.moribito.gui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * Authentic Gruvbox Dark color palette using original naming convention.
 * Replaces Material-style tints with official Gruvbox "Retro" tones.
 */
object AppColors {
    // Neutral colors - Mapped to Gruvbox Dark (Medium) palette
    val neutral10 = Color(0xFF1D2021)            // bg0_h - Hard background
    val neutral20 = Color(0xFF282828)            // bg0 - Main background
    val neutral30 = Color(0xFF32302F)            // bg0_s - Soft background
    val neutral40 = Color(0xFF3C3836)            // bg1 - UI background (Buttons/Fields)
    val neutral50 = Color(0xFF504945)            // bg2 - Selection / Subtle borders
    val neutral60 = Color(0xFF665C54)            // bg3 - Secondary Selection
    val neutral70 = Color(0xFF7C6F64)            // bg4 - Gray (Muted elements)

    val neutral80 = Color(0xFF928374)            // gray_245 - Muted text
    val neutral90 = Color(0xFFA89984)            // fg4 - Muted text
    val neutral100 = Color(0xFFBDAE93)           // fg3 - Hint text
    val neutral110 = Color(0xFFD5C4A1)           // fg2 - Secondary text
    val neutral120 = Color(0xFFEBDBB2)           // fg1 - Primary text (Cream)
    val neutral130 = Color(0xFFFBF1C7)           // fg0 - Brightest text
    val neutral140 = Color(0xFFF9F5D7)           // fg0_h - Maximum brightness

    // Background & Surface
    val background = neutral10                   // Darkest (Hard)
    val surface = neutral20                      // Standard background
    val surfaceVariant = neutral30               // Raised surface
    val surfaceDim = neutral10                   // Recessed surface

    // Border colors
    val border = neutral40                       // Low contrast border
    val borderVariant = neutral50                // High contrast border

    // Text colors
    val textPrimary = neutral120                 // Primary (fg1)
    val textSecondary = neutral110               // Secondary (fg2)
    val textTertiary = neutral100                // Placeholders (fg3)
    val textDisabled = neutral70                 // Disabled (bg4)

    // Semantic Colors (Using Gruvbox "Bright" variants)
    val blue100 = Color(0xFF83A598)              // Blue (Aqua/Blue mix)
    val blue90 = Color(0xFF458588)               // Dark Blue
    val blue60 = Color(0xFF076678)               // Deep Blue (Focus)

    val green100 = Color(0xFFB8BB26)             // Green
    val green60 = Color(0xFF98971A)              // Dark Green

    val red100 = Color(0xFFFB4934)               // Red
    val red60 = Color(0xFFCC241D)                // Dark Red

    val orange = Color(0xFFFE8019)               // Orange
    val yellow120 = Color(0xFFFABD2F)            // Yellow

    val purple100 = Color(0xFFD3869B)            // Purple
    val violet100 = Color(0xFFB16286)            // Violet

    // Semantic mapping
    val primary = blue100
    val primaryVariant = blue90
    val primaryContainer = Color(0xFF282828)

    val success = green100
    val error = red100
    val warning = orange
    val info = blue100

    // State overlays (Matching Gruvbox contrast)
    val hoverOverlay = Color(0x1AFFFFFF)        // 10% white
    val pressedOverlay = Color(0x33FFFFFF)      // 20% white
    val focusOutline = blue100

    /**
     * Material 3 ColorScheme with Authentic Gruvbox Dark.
     */
    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = neutral10,
        primaryContainer = neutral40,
        onPrimaryContainer = neutral120,

        secondary = orange,
        onSecondary = neutral10,

        tertiary = green100,
        onTertiary = neutral10,

        error = red100,
        onError = neutral10,

        background = background,
        onBackground = neutral120,

        surface = surface,
        onSurface = neutral120,
        surfaceVariant = neutral30,
        onSurfaceVariant = neutral110,

        outline = neutral50,
        outlineVariant = neutral60
    )
}
/**
 * Extension function to get hover color for any base color.
 */
fun Color.hover(): Color {
    // We create the overlay color first, then apply it "over" the base (this)
    val overlay = AppColors.neutral120.copy(alpha = 0.15f)
    return overlay.compositeOver(this)
}

/**
 * Extension function to get pressed color for any base color.
 */
fun Color.pressed(): Color {
    val overlay = AppColors.neutral120.copy(alpha = 0.25f)
    return overlay.compositeOver(this)
}

/**
 * IntelliJ-specific colors for matching the official dark theme.
 */
object IntelliJColors {
    /**
     * IntelliJ's island/panel background color (#191A1C).
     * Used for editor panes, tool windows, and elevated panels.
     */
    val islandBackground = Color(0xFF191A1C)

    /**
     * The bordering color in intellij
     */
    val baseBackground = Color(0xFF26282b)

    val baseBackgroundDisabled = Color(0xFF2D3033)

    /**
     * Slightly lighter hover state for interactive elements.
     */
    val hoverBackground = Color(0xFF313438)

    /**
     * Blue-tinted background for selected items.
     */
    val selectedBackground = Color(0xFF1E415F)

    /**
     * Semantic colors matching IntelliJ's palette
     */
    val success = Color(0xFF6AAB73) // IntelliJ green
    val warning = Color(0xFFCDA869) // IntelliJ yellow/orange
    val error = Color(0xFFCC666E) // IntelliJ red
}