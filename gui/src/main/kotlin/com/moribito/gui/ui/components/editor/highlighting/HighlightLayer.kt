package com.moribito.gui.ui.components.editor.highlighting

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.moribito.gui.ui.components.editor.drawing.LineSegment

/**
 * Abstract base class for all highlight layers in the code editor.
 *
 * Highlight layers can be stacked with z-ordering to create composite syntax highlighting
 * effects. Lower z-index values are rendered first (bottom), higher values render last (top).
 *
 * Common z-index conventions:
 * - 0: Syntax highlighting (base layer)
 * - 5: Search results
 * - 8: Reference highlighting (matching parens)
 * - 10: Error/diagnostic highlighting
 *
 * @property zIndex The rendering order (lower values rendered first)
 */
@Stable
abstract class HighlightLayer(val zIndex: Int) : Comparable<HighlightLayer> {
    /**
     * The list of highlighted segments to render.
     * Implementations should compute this lazily if possible to avoid unnecessary work.
     */
    abstract val segments: List<LineSegment>

    /**
     * Compare layers by z-index for sorting.
     * If z-indices are equal, use hash code for stable ordering.
     */
    override fun compareTo(other: HighlightLayer): Int {
        val comp = zIndex.compareTo(other.zIndex)
        return if (comp != 0) comp else this.hashCode() - other.hashCode()
    }
}

/**
 * Syntax highlighting layer for LDAP filters.
 *
 * This layer wraps the existing LDAP filter parser and converts tokens
 * into line segments for rendering using the provided color scheme.
 *
 * @param text The LDAP filter text to highlight
 * @param colorScheme Map of token types to colors for syntax highlighting
 */
class SyntaxHighlightLayer(
    private val text: String,
    private val colorScheme: Map<LdapTokenType, Color>
) : HighlightLayer(zIndex = 0) {
    /**
     * Parse the text and convert tokens to line segments.
     * Computed lazily and cached.
     */
    override val segments: List<LineSegment> by lazy {
        // Skip highlighting for very large texts (performance optimization)
        if (text.length > 10000) {
            emptyList()
        } else {
            parseLdapFilter(text).map { token ->
                LineSegment(
                    startIndex = token.startIndex,
                    endIndex = token.endIndex,
                    color = colorScheme[token.type] ?: colorScheme[LdapTokenType.Whitespace]!!
                )
            }
        }
    }
}

/**
 * Reference highlighting layer for matching parentheses and similar references.
 *
 * @param ranges The text ranges to highlight
 * @param color The highlight color to use
 */
class ReferenceHighlightLayer(
    private val ranges: List<androidx.compose.ui.text.TextRange>,
    private val color: Color
) : HighlightLayer(zIndex = 8) {
    override val segments: List<LineSegment>
        get() = ranges.map { range ->
            LineSegment(
                startIndex = range.start,
                endIndex = range.end,
                color = Color.Transparent,
                backgroundColor = color.copy(alpha = 0.2f)
            )
        }
}
