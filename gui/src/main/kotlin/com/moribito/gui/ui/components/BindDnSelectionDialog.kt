package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moribito.config.BindCredential
import com.moribito.config.LdapConfig
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import com.moribito.gui.theme.IntelliJColors
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer

/**
 * Dialog to prompt user to select a bind DN when multiple are available.
 */
@Composable
fun BindDnSelectionDialog(
    connection: LdapConfig,
    onSelect: (BindCredential) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = true, onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Island(
            modifier = Modifier
                .width(450.dp)
                .clickable(enabled = false, onClick = {}),
            padding = AppSpacing.lg
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    text = "Select Bind Credential",
                    style = AppTypography.titleMedium
                )
                
                Text(
                    text = "Multiple bind DNs are configured for '${connection.name}'. Please select one to connect:",
                    style = AppTypography.bodyMedium,
                    color = AppColors.neutral100
                )
                
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                
                VerticallyScrollableContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        connection.effectiveBindCredentials.forEachIndexed { index, credential ->
                            CredentialSelectionRow(
                                credential = credential,
                                isEven = index % 2 == 0,
                                onClick = { onSelect(credential) }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialSelectionRow(
    credential: BindCredential,
    isEven: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isEven) {
        Color(0xFF2D3033)
    } else {
        Color(0xFF26282B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credential.label,
                style = AppTypography.labelMedium,
                color = AppColors.neutral140
            )
            Text(
                text = credential.bindUser,
                style = AppTypography.bodySmall,
                color = AppColors.neutral80,
                maxLines = 1
            )
        }
        
        if (credential.isDefault) {
            Text(
                text = "DEFAULT",
                style = AppTypography.labelSmall,
                color = IntelliJColors.warning
            )
        }
    }
}
