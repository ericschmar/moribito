package com.moribito.gui.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppMonospace
import com.moribito.gui.ui.components.editor.state.EditorState
import com.moribito.gui.ui.components.editor.state.rememberEditorState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A code editor component with line numbers and LDAP syntax highlighting.
 *
 * This component follows the compose-code-editor architecture with separated state management:
 * - TextState: Core text buffer
 * - EditorTextFieldState: TextField bridge with derivedStateOf for performance
 * - DrawState: Z-ordered highlight layers
 * - LineNumberState: Dynamic width calculation
 * - EditorState: Central coordinator
 *
 * Features:
 * - Line numbers synchronized with content
 * - LDAP filter syntax highlighting with Gruvbox colors
 * - Smooth scrolling
 * - Text selection and cursor handling
 * - External/internal state synchronization
 * - Extensible highlight layer system
 *
 * @param text The text content to display and edit
 * @param onTextChange Callback invoked when text changes
 * @param modifier Modifier for the editor container
 * @param enabled Whether the editor is enabled for input
 * @param placeholder Placeholder text to show when empty
 */
@Composable
fun CodeEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = ""
) {
    // Create editor state (remembers across recompositions)
    val editorState = rememberEditorState(text)

    // Sync external text changes to internal state
    LaunchedEffect(text) {
        if (editorState.textState.text != text) {
            editorState.updateText(text)
        }
    }

    // Text style for editor (16sp line height to match current design)
    val editorTextStyle = AppMonospace.medium.copy(
        lineHeight = 16.sp,
        color = JewelTheme.globalColors.text.normal
    )

    // Text style for line numbers (same line height for alignment)
    val lineNumberTextStyle = AppMonospace.medium.copy(
        lineHeight = 16.sp,
        color = JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
    )

    Row(modifier = modifier) {
        // Line numbers column
        LineNumberColumn(
            lineCount = editorState.textState.lineCount,
            scrollState = editorState.scrollState,
            textStyle = lineNumberTextStyle,
            width = editorState.lineNumberState.effectiveWidth
        )

        // Editor area
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Show placeholder if text is empty
            if (editorState.textState.text.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = editorTextStyle.copy(
                        color = JewelTheme.globalColors.text.info
                    )
                )
            }

            // Main text field with syntax highlighting
            BasicTextField(
                value = editorState.textFieldState.textFieldValue,
                onValueChange = { newValue ->
                    editorState.textFieldState.onValueChange(newValue)
                    // Notify external listener of text change
                    if (newValue.text != text) {
                        onTextChange(newValue.text)
                    }
                    // Update syntax highlighting when text changes
                    editorState.updateSyntaxHighlighting()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(editorState.scrollState, enabled = true),
                enabled = enabled,
                textStyle = editorTextStyle,
                cursorBrush = SolidColor(AppColors.blue100)
            )
        }
    }
}

/**
 * Line number column component.
 *
 * Displays line numbers synchronized with the editor's scroll position.
 * Uses a separate scroll state with enabled=false to follow the main editor's scroll.
 *
 * @param lineCount Number of lines to display
 * @param scrollState Scroll state to sync with
 * @param textStyle Text style for line numbers
 * @param width Width of the line number column
 */
@Composable
private fun LineNumberColumn(
    lineCount: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    textStyle: TextStyle,
    width: Dp
) {
    Column(
        modifier = Modifier
            .width(width)
            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .verticalScroll(scrollState, enabled = false), // Follow scroll but don't handle input
        horizontalAlignment = Alignment.End
    ) {
        repeat(lineCount) { index ->
            Text(
                text = "${index + 1}",
                style = textStyle,
                maxLines = 1
            )
        }
    }
}
