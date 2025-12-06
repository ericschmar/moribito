package com.moribito.gui.theme

/**
 * Spacing system for Moribito GUI application.
 *
 * Defines standard spacing values used for padding, margins, and gaps.
 */
object Spacing {
    // Base spacing unit
    const val UNIT = 8.0

    // Common spacing values
    const val TINY = UNIT / 2      // 4px
    const val SMALL = UNIT          // 8px
    const val MEDIUM = UNIT * 2     // 16px
    const val LARGE = UNIT * 3      // 24px
    const val XLARGE = UNIT * 4     // 32px
    const val XXLARGE = UNIT * 6    // 48px

    // Component-specific spacing
    const val BUTTON_PADDING_H = MEDIUM
    const val BUTTON_PADDING_V = SMALL
    const val CARD_PADDING = MEDIUM
    const val LIST_ITEM_PADDING = SMALL
    const val FORM_FIELD_SPACING = MEDIUM
    const val SECTION_SPACING = LARGE

    // Border radii
    const val RADIUS_SMALL = 4.0
    const val RADIUS_MEDIUM = 8.0
    const val RADIUS_LARGE = 12.0

    // Border widths
    const val BORDER_THIN = 1.0
    const val BORDER_MEDIUM = 2.0
    const val BORDER_THICK = 3.0
}
