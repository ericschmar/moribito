package com.moribito.gui.ui.components.query

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppMonospace
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Island
import com.moribito.gui.ui.components.editor.CodeEditor
import com.moribito.ldap.LdapSchema
import org.jetbrains.jewel.foundation.modifier.thenIf
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Query input panel with multi-line LDAP filter input and Format/Run buttons.
 * Features a code editor with syntax highlighting and line numbers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueryPanel(
    queryText: String,
    onQueryChange: (String) -> Unit,
    onFormat: () -> Unit,
    onRun: () -> Unit,
    isConnected: Boolean,
    schema: LdapSchema? = null,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Island(
        modifier = modifier.fillMaxSize(),
        padding = 0.dp // No padding between island and content
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Code editor fills entire space
            CodeEditor(
                text = queryText,
                onTextChange = onQueryChange,
                enabled = isConnected,
                schema = schema,
                placeholder = "Enter LDAP filter (e.g., (objectClass=*)) or SQL (e.g., SELECT * FROM people WHERE cn='john')",
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = IntelliJColors.islandBackground,
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            // Floating buttons in bottom-right corner
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                if (error.isNullOrBlank().not()) {
                    Text(
                        text = error,
                        color = JewelTheme.globalColors.text.error,
                        fontFamily = AppMonospace.small.fontFamily,
                        modifier = Modifier.padding(AppSpacing.xs)
                    )
                }
                /*                // Format button
                                OutlinedButton(
                                    onClick = onFormat,
                                    enabled = false
                                ) {
                                    Text("Format")
                                }*/

                Tooltip(tooltip = { Text("Run") }) {
                    IconButton(
                        onClick = onRun,
                        enabled = isConnected && queryText.isNotBlank(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            key = AllIconsKeys.Actions.RunAll,
                            contentDescription = "Run",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
