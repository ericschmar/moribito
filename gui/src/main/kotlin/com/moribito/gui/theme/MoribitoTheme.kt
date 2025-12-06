package com.moribito.gui.theme

import io.nacular.doodle.drawing.Color

/**
 * Main theme for Moribito GUI application.
 *
 * Provides access to colors, typography, spacing, and component styling.
 */
class MoribitoTheme(val fonts: LoadedFonts) {
    // Color palette
    val colors = Colors

    // Spacing system
    val spacing = Spacing

    /**
     * Gets a color for selected states based on intensity.
     */
    fun selectedColor(intensity: Double = 1.0): Color {
        return colors.blueToTealGradient(intensity * 0.3)
    }

    /**
     * Gets a color for hover states.
     */
    fun hoverColor(): Color {
        return colors.hoverBlue
    }

    /**
     * Gets the appropriate text color for a given background.
     */
    fun textColorFor(background: Color): Color {
        // Simple luminance check - use white text on dark backgrounds
        val r = background.red.toInt() / 255.0
        val g = background.green.toInt() / 255.0
        val b = background.blue.toInt() / 255.0

        val luminance = r * 0.299 + g * 0.587 + b * 0.114

        return if (luminance < 0.5) colors.textLight else colors.textPrimary
    }
}
