package com.moribito.gui.ui.components.editor.drawing

import androidx.compose.ui.graphics.Color

/**
 * Represents a single highlighted text span in the editor.
 *
 * Line segments are used by highlight layers to specify which ranges of text
 * should be colored and how. Multiple layers can overlap, with z-ordering
 * determining which highlights take precedence.
 *
 * @property startIndex The starting character index (inclusive)
 * @property endIndex The ending character index (exclusive)
 * @property color The foreground text color for this segment
 * @property backgroundColor Optional background color (for error/warning backgrounds)
 * @property underline Whether to underline this segment (for suggestions/warnings)
 */
data class LineSegment(
    val startIndex: Int,
    val endIndex: Int,
    val color: Color,
    val backgroundColor: Color? = null,
    val underline: Boolean = false
)
