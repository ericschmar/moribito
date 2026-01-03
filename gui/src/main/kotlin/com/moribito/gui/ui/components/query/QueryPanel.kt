package com.moribito.gui.ui.components.query

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moribito.gui.theme.AppMonospace
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Island
import com.moribito.gui.ui.components.editor.CodeEditor
import com.moribito.ldap.LdapSchema
import compose.icons.Octicons
import compose.icons.octicons.PaperAirplane16
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Query input panel with multi-line LDAP filter input and Format/Run buttons.
 * Features a code editor with syntax highlighting and line numbers.
 */
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
                if(error.isNullOrBlank().not()) {
                    Text(
                        text = error,
                        color = JewelTheme.globalColors.text.error,
                        fontFamily = AppMonospace.small.fontFamily,
                        modifier = Modifier.padding(AppSpacing.xs)
                    )
                }
                // Format button
                OutlinedButton(
                    onClick = onFormat,
                    enabled = false
                ) {
                    Text("Format")
                }

                // Run button
                DefaultButton(
                    onClick = onRun,
                    enabled = isConnected && queryText.isNotBlank()
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(
                            imageVector = Octicons.PaperAirplane16,
                            contentDescription = "Run",
                            modifier = Modifier.size(14.dp),
                            tint = JewelTheme.contentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run")
                    }
                }
            }
        }
    }
}
