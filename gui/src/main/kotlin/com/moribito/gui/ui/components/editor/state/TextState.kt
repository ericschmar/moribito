package com.moribito.gui.ui.components.editor.state

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange

/**
 * Core text buffer state that manages the raw text content, caret position, and selection.
 *
 * This class serves as the single source of truth for text data, separate from UI concerns.
 * It provides derived states for performance optimization and supports text manipulation operations.
 */
@Stable
class TextState(initialText: String = "") {
    /**
     * The raw text content.
     */
    var text by mutableStateOf(initialText)
        private set

    /**
     * The current caret offset position in the text.
     * Set to -1 when there's a non-collapsed selection.
     */
    var caretOffset by mutableStateOf(initialText.length)
        private set

    /**
     * The current text selection range.
     */
    var selection by mutableStateOf(TextRange(initialText.length))
        private set

    /**
     * Total number of lines in the text.
     * Computed lazily and cached until text changes.
     */
    val lineCount by derivedStateOf {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }

    /**
     * Split text into individual lines.
     * Computed lazily and cached until text changes.
     */
    val lines by derivedStateOf {
        text.lines()
    }

    /**
     * Reference highlight ranges for features like matching parentheses.
     * These are rendered as a separate highlight layer.
     */
    var referenceHighlightRanges by mutableStateOf<List<TextRange>>(emptyList())
        private set

    /**
     * Update the text content and selection.
     *
     * @param newText The new text content
     * @param newSelection The new selection range (defaults to current selection)
     */
    fun updateText(newText: String, newSelection: TextRange = selection) {
        text = newText
        selection = newSelection
        caretOffset = if (newSelection.collapsed) newSelection.start else -1
    }

    /**
     * Insert text at the specified offset.
     *
     * @param offset The position to insert at
     * @param insertion The text to insert
     * @return The new caret position after insertion
     */
    fun insertText(offset: Int, insertion: String): Int {
        val newText = text.substring(0, offset) + insertion + text.substring(offset)
        val newCaretOffset = offset + insertion.length
        updateText(newText, TextRange(newCaretOffset))
        return newCaretOffset
    }

    /**
     * Delete text in the specified range.
     *
     * @param range The range of text to delete
     * @return The new caret position after deletion
     */
    fun deleteText(range: TextRange): Int {
        val newText = text.substring(0, range.start) + text.substring(range.end)
        val newCaretOffset = range.start
        updateText(newText, TextRange(newCaretOffset))
        return newCaretOffset
    }

    /**
     * Update the reference highlight ranges (e.g., for matching parentheses).
     *
     * @param ranges The list of text ranges to highlight
     */
    fun updateReferenceHighlight(ranges: List<TextRange>) {
        referenceHighlightRanges = ranges
    }

    /**
     * Clear all reference highlights.
     */
    fun clearReferenceHighlight() {
        referenceHighlightRanges = emptyList()
    }
}
