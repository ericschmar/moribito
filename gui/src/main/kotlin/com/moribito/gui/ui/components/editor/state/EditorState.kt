package com.moribito.gui.ui.components.editor.state

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.moribito.gui.ui.components.editor.highlighting.LdapTokenType
import com.moribito.gui.ui.components.editor.highlighting.SqlSyntaxHighlightLayer
import com.moribito.gui.ui.components.editor.highlighting.SqlTokenType
import com.moribito.gui.ui.components.editor.highlighting.SyntaxHighlightLayer
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Central coordinator for all code editor state.
 *
 * This class integrates all the individual state objects (text, drawing, scrolling, etc.)
 * into a single cohesive editor state. It provides the public API for the editor and
 * coordinates state updates across subsystems.
 *
 * The @Stable annotation enables Compose to optimize recompositions by treating this
 * class as stable despite containing mutable state.
 *
 * Example usage:
 * ```
 * val editorState = rememberEditorState("initial text")
 * editorState.updateText("new text")
 * CodeEditor(editorState = editorState)
 * ```
 */
@Stable
class EditorState(
    initialText: String = "",
    private val ldapColorScheme: Map<LdapTokenType, Color>,
    private val sqlColorScheme: Map<SqlTokenType, Color>
) {
    /**
     * Core text buffer with caret and selection management.
     */
    val textState = TextState(initialText)

    /**
     * Z-ordered highlight layer management (syntax, errors, search, etc.).
     */
    val drawState = DrawState()

    /**
     * Vertical scroll position for the editor.
     */
    val scrollState = ScrollState(0)

    /**
     * Line number column width calculation.
     */
    val lineNumberState = LineNumberState(textState)

    /**
     * TextField bridge with highlight layer composition.
     */
    val textFieldState = EditorTextFieldState(textState, drawState) {
        updateSyntaxHighlighting()
    }

    init {
        // Initialize default layers
        updateSyntaxHighlighting()
    }

    /**
     * Update the text content.
     *
     * This method updates the text and refreshes syntax highlighting.
     * Use this for programmatic text updates (not user typing, which goes through textFieldState).
     *
     * @param newText The new text content
     */
    fun updateText(newText: String) {
        textState.updateText(newText, androidx.compose.ui.text.TextRange(newText.length))
        updateSyntaxHighlighting()
    }

    /**
     * Update syntax highlighting by recreating the syntax layer.
     *
     * This is called automatically when text changes. The lazy evaluation in
     * SyntaxHighlightLayer ensures parsing only happens when needed.
     */
    fun updateSyntaxHighlighting() {
        val text = textState.text
        val isSql = text.trimStart().startsWith("SELECT", ignoreCase = true)

        if (isSql) {
            drawState.addLayer(
                "syntax",
                SqlSyntaxHighlightLayer(text, sqlColorScheme)
            )
        } else {
            drawState.addLayer(
                "syntax",
                SyntaxHighlightLayer(text, ldapColorScheme)
            )
        }
    }

    /**
     * Trigger syntax highlighting refresh manually.
     * Useful when highlight rules change without text changing.
     */
    fun refreshHighlighting() {
        updateSyntaxHighlighting()
    }

    /**
     * Clear all highlight layers except syntax.
     * Useful for clearing search results or error highlights.
     */
    fun clearSecondaryHighlights() {
        val layers = drawState.getLayers().map { layer ->
            // Get layer IDs - we'd need to track this in DrawState to support this properly
            // For now, just clear everything and re-add syntax
        }
        drawState.clearLayers()
        updateSyntaxHighlighting()
    }
}

/**
 * Remember an EditorState across recompositions with Jewel theme colors.
 *
 * This function follows Compose conventions for stateful components.
 * The state is preserved across recompositions as long as the key doesn't change.
 *
 * @param initialText The initial text content (used as remember key)
 * @return A remembered EditorState instance with Jewel theme colors
 */
@Composable
fun rememberEditorState(
    initialText: String = ""
): EditorState {
    // Create color scheme from Jewel theme
    // Using semantic colors available in Jewel theme
    val ldapColorScheme = mapOf(
        LdapTokenType.LogicalOperator to JewelTheme.globalColors.text.warning,      // Orange/amber for operators
        LdapTokenType.Parenthesis to JewelTheme.globalColors.text.normal,                // Normal text for structure
        LdapTokenType.Attribute to JewelTheme.globalColors.text.normal,                   // Blue/cyan for attribute names
        LdapTokenType.ComparisonOperator to JewelTheme.globalColors.text.selected,      // Highlighted color for operators
        LdapTokenType.Value to JewelTheme.globalColors.text.info.copy(green = 0.8f),   // Greenish tint for values
        LdapTokenType.Wildcard to JewelTheme.globalColors.text.warning,             // Yellow/amber for wildcards
        LdapTokenType.Whitespace to JewelTheme.globalColors.text.normal,                // Normal text color
        LdapTokenType.Invalid to JewelTheme.globalColors.text.error                 // Red for errors
    )

    val sqlColorScheme = mapOf(
        SqlTokenType.Keyword to Color(0xFFCC7832), // Orange
        SqlTokenType.Identifier to JewelTheme.globalColors.text.normal,
        SqlTokenType.String to Color(0xFF6A8759), // Green
        SqlTokenType.Operator to JewelTheme.globalColors.text.normal,
        SqlTokenType.Punctuation to JewelTheme.globalColors.text.normal,
        SqlTokenType.Whitespace to JewelTheme.globalColors.text.normal,
        SqlTokenType.Invalid to JewelTheme.globalColors.text.error
    )

    return remember(initialText) {
        EditorState(initialText, ldapColorScheme, sqlColorScheme)
    }
}
