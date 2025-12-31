package com.moribito.gui.ui.components.editor.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Bridges between TextState and BasicTextField using derivedStateOf for performance.
 *
 * This class is responsible for:
 * 1. Creating AnnotatedString with combined highlight layers
 * 2. Converting TextState to TextFieldValue for BasicTextField
 * 3. Syncing TextField changes back to TextState
 *
 * The use of derivedStateOf ensures that AnnotatedString is only rebuilt when
 * the text or highlight layers actually change, preventing unnecessary recompositions.
 */
@Stable
class EditorTextFieldState(
    private val textState: TextState,
    private val drawState: DrawState,
    private val onTextChange: (String) -> Unit
) {
    /**
     * The TextField value with combined highlighting from all layers.
     *
     * This property uses derivedStateOf to minimize recompositions.
     * It only recalculates when textState.text, textState.selection, or drawState layers change.
     */
    val textFieldValue: TextFieldValue by derivedStateOf {
        TextFieldValue(
            annotatedString = buildAnnotatedText(),
            selection = textState.selection
        )
    }

    /**
     * Build an AnnotatedString by combining all highlight layers in z-order.
     *
     * This function applies highlights from multiple layers:
     * 1. Start with plain text
     * 2. Apply each layer's highlights in z-order (low to high)
     * 3. Later layers can override earlier layers for the same range
     *
     * @return AnnotatedString with all highlights applied
     */
    private fun buildAnnotatedText(): AnnotatedString {
        return buildAnnotatedString {
            append(textState.text)

            // Apply highlights from all layers in z-order
            val layers = drawState.getLayers()
            for (layer in layers) {
                for (segment in layer.segments) {
                    // Apply foreground color
                    addStyle(
                        style = SpanStyle(color = segment.color),
                        start = segment.startIndex,
                        end = segment.endIndex
                    )

                    // Apply background color if specified
                    segment.backgroundColor?.let { bgColor ->
                        addStyle(
                            style = SpanStyle(background = bgColor),
                            start = segment.startIndex,
                            end = segment.endIndex
                        )
                    }

                    // Apply underline if specified
                    if (segment.underline) {
                        addStyle(
                            style = SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                            start = segment.startIndex,
                            end = segment.endIndex
                        )
                    }
                }
            }
        }
    }

    /**
     * Handle TextField value changes and sync back to TextState.
     *
     * This method is called by BasicTextField when the user types or modifies text.
     * It updates the TextState, which will trigger a recomposition and rebuild
     * the highlighted text via derivedStateOf.
     *
     * @param newValue The new TextField value from user input
     */
    fun onValueChange(newValue: TextFieldValue) {
        textState.updateText(newValue.text, newValue.selection)
        onTextChange(newValue.text)
    }
}
