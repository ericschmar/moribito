package com.moribito.gui.theme

import io.nacular.doodle.drawing.Color

/**
 * Color palette for Moribito GUI application.
 *
 * Uses a blue and teal color scheme inspired by the TUI version.
 */
object Colors {
    // Primary colors
    val primaryBlue = Color(0x0066CCu)
    val secondaryTeal = Color(0x008080u)

    // Background colors
    val backgroundLight = Color(0xF5F5F5u)
    val backgroundDark = Color(0x333333u)
    val backgroundWhite = Color(0xFFFFFFu)

    // Text colors
    val textPrimary = Color(0x212121u)
    val textSecondary = Color(0x757575u)
    val textLight = Color(0xFFFFFFu)

    // Status colors
    val success = Color(0x4CAF50u)
    val error = Color(0xF44336u)
    val warning = Color(0xFF9800u)
    val info = Color(0x2196F3u)

    // UI element colors
    val border = Color(0xCCCCCCu)
    val borderLight = Color(0xE0E0E0u)
    val hoverBlue = Color(0x0088EEu)
    val selectedBlue = Color(0x0066CCu)
    val disabledGray = Color(0xBDBDBDu)

    /**
     * Creates a gradient color between blue and teal.
     */
    fun blueToTealGradient(position: Double): Color {
        require(position in 0.0..1.0) { "Position must be between 0.0 and 1.0" }

        // Interpolate between primary blue and secondary teal
        val r1 = primaryBlue.red.toInt()
        val g1 = primaryBlue.green.toInt()
        val b1 = primaryBlue.blue.toInt()

        val r2 = secondaryTeal.red.toInt()
        val g2 = secondaryTeal.green.toInt()
        val b2 = secondaryTeal.blue.toInt()

        val r = lerp(r1.toDouble(), r2.toDouble(), position).toInt()
        val g = lerp(g1.toDouble(), g2.toDouble(), position).toInt()
        val b = lerp(b1.toDouble(), b2.toDouble(), position).toInt()

        return Color(((r shl 16) or (g shl 8) or b).toUInt())
    }

    private fun lerp(start: Double, end: Double, fraction: Double): Double {
        return start + (end - start) * fraction
    }
}
