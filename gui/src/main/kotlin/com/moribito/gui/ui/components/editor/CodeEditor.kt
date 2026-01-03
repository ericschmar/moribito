package com.moribito.gui.ui.components.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppMonospace
import com.moribito.gui.ui.components.editor.state.EditorState
import com.moribito.gui.ui.components.editor.state.rememberEditorState
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.jewel.ui.component.Popup
import com.moribito.ldap.LdapSchema
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors

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
    schema: LdapSchema? = null,
    enabled: Boolean = true,
    placeholder: String = ""
) {
    // Create editor state (remembers across recompositions)
    val editorState = rememberEditorState(text, schema)
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Calculate cursor position for autocomplete
    val cursorOffset = remember(textLayoutResult, editorState.textState.selection, editorState.scrollState.value) {
        textLayoutResult?.let { layout ->
            val offset = editorState.textState.selection.end
            if (offset <= layout.layoutInput.text.length) {
                val cursorRect = layout.getCursorRect(offset)
                IntOffset(
                    x = cursorRect.left.toInt(),
                    y = cursorRect.bottom.toInt() - editorState.scrollState.value
                )
            } else IntOffset.Zero
        } ?: IntOffset.Zero
    }

    val cursorLineHeight = remember(textLayoutResult, editorState.textState.selection) {
        textLayoutResult?.let { layout ->
            val offset = editorState.textState.selection.end
            if (offset <= layout.layoutInput.text.length) {
                layout.getCursorRect(offset).height.toInt()
            } else 20
        } ?: 20
    }

    // Sync external text changes to internal state
    LaunchedEffect(text) {
        if (editorState.textState.text != text) {
            editorState.updateText(text)
        }
    }

    // Sync schema changes to autocomplete state
    LaunchedEffect(schema) {
        editorState.autocompleteState.updateSchema(schema)
    }

    // Update autocomplete on selection change
    LaunchedEffect(editorState.textState.selection) {
        editorState.autocompleteState.update(
            editorState.textState.text,
            editorState.textState.selection.end
        )
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
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(editorState.scrollState)
                    .onPreviewKeyEvent { keyEvent ->
                        if (editorState.autocompleteState.isVisible) {
                            when {
                                keyEvent.key == Key.DirectionDown && keyEvent.type == KeyEventType.KeyDown -> {
                                    val nextIndex = (editorState.autocompleteState.selectedIndex + 1) % editorState.autocompleteState.suggestions.size
                                    editorState.autocompleteState.selectedIndex = nextIndex
                                    true
                                }
                                keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown -> {
                                    val prevIndex = if (editorState.autocompleteState.selectedIndex > 0) {
                                        editorState.autocompleteState.selectedIndex - 1
                                    } else {
                                        editorState.autocompleteState.suggestions.size - 1
                                    }
                                    editorState.autocompleteState.selectedIndex = prevIndex
                                    true
                                }
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.Tab) && keyEvent.type == KeyEventType.KeyDown -> {
                                    val selected = editorState.autocompleteState.suggestions[editorState.autocompleteState.selectedIndex]
                                    insertSuggestion(editorState, selected, onTextChange)
                                    true
                                }
                                keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                                    editorState.autocompleteState.isVisible = false
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                enabled = enabled,
                textStyle = editorTextStyle,
                cursorBrush = SolidColor(AppColors.blue100)
            )

            // Autocomplete Popup
            if (editorState.autocompleteState.isVisible) {
                AutocompleteDropdown(
                    state = editorState.autocompleteState,
                    cursorOffset = cursorOffset,
                    cursorLineHeight = cursorLineHeight,
                    onSuggestionSelected = { suggestion ->
                        insertSuggestion(editorState, suggestion, onTextChange)
                    }
                )
            }
        }
    }
}

/**
 * Inserts the selected suggestion into the editor.
 */
private fun insertSuggestion(
    editorState: EditorState,
    suggestion: AutocompleteSuggestion,
    onTextChange: (String) -> Unit
) {
    val text = editorState.textState.text
    val cursor = editorState.textState.selection.end
    val query = editorState.autocompleteState.query
    
    val before = text.substring(0, cursor - query.length)
    val after = text.substring(cursor)
    
    val newText = before + suggestion.insertValue + after
    val newCursor = before.length + suggestion.insertValue.length
    
    editorState.textState.updateText(newText, TextRange(newCursor))
    onTextChange(newText)
    editorState.autocompleteState.isVisible = false
}

@Composable
private fun AutocompleteDropdown(
    state: AutocompleteState,
    cursorOffset: IntOffset,
    cursorLineHeight: Int,
    onSuggestionSelected: (AutocompleteSuggestion) -> Unit
) {
    val popupPositionProvider = remember(cursorOffset, cursorLineHeight) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = (anchorBounds.left + cursorOffset.x).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                var y = anchorBounds.top + cursorOffset.y
                
                // If it goes off the bottom of the window, flip it to above the cursor
                if (y + popupContentSize.height > windowSize.height) {
                    val flippedY = anchorBounds.top + cursorOffset.y - cursorLineHeight - popupContentSize.height
                    if (flippedY >= 0) {
                        y = flippedY
                    }
                }
                
                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = { state.isVisible = false }
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 400.dp)
                .heightIn(max = 300.dp)
                .shadow(8.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(IntelliJColors.islandBackground)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            state.suggestions.forEachIndexed { index, suggestion ->
                val isSelected = state.selectedIndex == index
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) JewelTheme.globalColors.text.selected.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onSuggestionSelected(suggestion) }
                        .padding(horizontal = AppSpacing.sm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (suggestion.isContainer) suggestion.displayValue else suggestion.insertValue,
                            style = AppMonospace.medium.copy(fontSize = 12.sp)
                        )
                        if (suggestion.description != null) {
                            Text(
                                text = suggestion.description,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 10.sp,
                                    color = JewelTheme.globalColors.text.info
                                ),
                                maxLines = 1
                            )
                        } else if (suggestion.isContainer) {
                             Text(
                                text = suggestion.displayValue,
                                style = JewelTheme.defaultTextStyle.copy(
                                    fontSize = 10.sp,
                                    color = JewelTheme.globalColors.text.info
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
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
    scrollState: ScrollState,
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
