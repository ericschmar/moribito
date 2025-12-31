package com.moribito.gui.ui.components.editor.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Manages line number column metrics and layout calculations.
 *
 * This class calculates the width needed for the line number column dynamically
 * based on the number of lines in the document. As files grow, the line number
 * column automatically expands to accommodate larger line numbers.
 *
 * Example: A file with 9 lines needs less width than a file with 1000 lines.
 */
@Stable
class LineNumberState(
    private val textState: TextState
) {
    /**
     * Calculate the width needed for the line number column.
     *
     * This uses derivedStateOf to only recalculate when the line count changes.
     * The width is based on the number of digits in the maximum line number,
     * plus padding for comfortable reading.
     *
     * Formula: (digitCount * approximateCharWidth) + padding
     * - Each digit is approximately 10dp wide in monospace font
     * - Add 20dp total padding (10dp each side)
     */
    val width: Dp by derivedStateOf {
        val maxLineNumber = textState.lineCount
        val digitCount = maxLineNumber.toString().length
        // 10dp per digit + 20dp padding for comfortable spacing
        (digitCount * 10 + 20).dp
    }

    /**
     * Calculate the minimum width for the line number column.
     * This ensures the column is never too narrow, even for single-digit line counts.
     */
    val minWidth: Dp = 32.dp

    /**
     * Get the effective width (max of calculated width and minimum width).
     */
    val effectiveWidth: Dp by derivedStateOf {
        maxOf(width, minWidth)
    }
}
