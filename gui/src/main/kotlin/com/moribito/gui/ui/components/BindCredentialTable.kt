package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.config.BindCredential
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import com.moribito.gui.theme.IntelliJColors
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Table component for managing bind credentials.
 * Displays a list of bind DNs with zebra striping and action buttons.
 */
@Composable
fun BindCredentialTable(
    credentials: List<BindCredential>,
    selectedCredential: BindCredential? = null,
    onCredentialSelected: (BindCredential) -> Unit,
    onAddCredential: () -> BindCredential,
    onDeleteCredential: (BindCredential) -> Unit,
    onUpdateCredential: (BindCredential) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCredentialId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color(0xFF1F2124)) // Slightly lighter than island background
                .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Label",
                style = AppTypography.labelMedium,
                color = AppColors.neutral140,
                modifier = Modifier.weight(0.25f)
            )
            Text(
                text = "Bind User",
                style = AppTypography.labelMedium,
                color = AppColors.neutral140,
                modifier = Modifier.weight(0.35f)
            )
            Text(
                text = "Password",
                style = AppTypography.labelMedium,
                color = AppColors.neutral140,
                modifier = Modifier.weight(0.25f)
            )
            Text(
                text = "Default",
                style = AppTypography.labelMedium,
                color = AppColors.neutral140,
                modifier = Modifier.weight(0.1f)
            )
            Spacer(modifier = Modifier.width(40.dp)) // Space for action buttons
        }

        // Table rows
        if (credentials.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bind credentials configured",
                    style = AppTypography.bodyMedium,
                    color = AppColors.neutral60
                )
            }
        } else {
            VerticallyScrollableContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { editingCredentialId = null } // Exit edit mode when clicking empty space
                        )
                    }
            ) {
                Column {
                    credentials.forEachIndexed { index, credential ->
                        BindCredentialRow(
                            credential = credential,
                            isEven = index % 2 == 0,
                            isSelected = selectedCredential?.id == credential.id,
                            isEditing = editingCredentialId == credential.id,
                            onSelected = {
                                onCredentialSelected(credential)
                                editingCredentialId = null
                            },
                            onStartEdit = {
                                // Don't call onCredentialSelected here - it changes the view
                                // Just start editing inline
                                editingCredentialId = credential.id
                            },
                            onStopEdit = { editingCredentialId = null },
                            onDelete = { onDeleteCredential(credential) },
                            onUpdate = onUpdateCredential
                        )
                    }
                }
            }
        }

        // Action bar
        ActionBar(
            rightContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    IconButton(
                        onClick = {
                            val newCred = onAddCredential()
                            editingCredentialId = newCred.id
                        }
                    ) {
                        Icon(
                            key = AllIconsKeys.General.Add,
                            contentDescription = "Add",
                        )
                    }
                }
            }
        )
    }
}

/**
 * Single row in the bind credential table with zebra striping.
 */
@Composable
private fun BindCredentialRow(
    credential: BindCredential,
    isEven: Boolean,
    isSelected: Boolean,
    isEditing: Boolean,
    onSelected: () -> Unit,
    onStartEdit: () -> Unit,
    onStopEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (BindCredential) -> Unit,
    modifier: Modifier = Modifier
) {
    // Darker stripe uses island background, lighter stripe is slightly lighter
    val backgroundColor = if (isEven) {
        IntelliJColors.islandBackground
    } else {
        Color(0xFF1F2124) // Slightly lighter than island background
    }

    var label by remember(credential.label) { mutableStateOf(credential.label) }
    var bindUser by remember(credential.bindUser) { mutableStateOf(credential.bindUser) }
    var bindPass by remember(credential.bindPass) { mutableStateOf(credential.bindPass) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(if (isSelected) Color(0xFF2D3033) else backgroundColor)
            .pointerInput(credential.id) {
                detectTapGestures(
                    onDoubleTap = { onStartEdit() }
                )
            }
            .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label column (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            if (isEditing) {
                InlineTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        onUpdate(credential.copy(label = it))
                    },
                    onDone = onStopEdit
                )
            } else {
                Text(
                    text = credential.label,
                    style = AppTypography.bodyMedium,
                    color = AppColors.neutral140,
                    maxLines = 1
                )
            }
        }

        // Bind User column (35%)
        Box(modifier = Modifier.weight(0.35f)) {
            if (isEditing) {
                InlineTextField(
                    value = bindUser,
                    onValueChange = {
                        bindUser = it
                        onUpdate(credential.copy(bindUser = it))
                    },
                    onDone = onStopEdit
                )
            } else {
                Text(
                    text = credential.bindUser,
                    style = AppTypography.bodySmall,
                    color = AppColors.neutral100,
                    maxLines = 1
                )
            }
        }

        // Password column (25%)
        Box(modifier = Modifier.weight(0.25f)) {
            if (isEditing) {
                InlineTextField(
                    value = bindPass,
                    onValueChange = {
                        bindPass = it
                        onUpdate(credential.copy(bindPass = it))
                    },
                    onDone = onStopEdit,
                    isPassword = false // Reveal password when editing
                )
            } else {
                Text(
                    text = "•".repeat(8), // Show dots for password
                    style = AppTypography.bodySmall,
                    color = AppColors.neutral100,
                    maxLines = 1
                )
            }
        }

        // Default indicator (10%)
        Box(
            modifier = Modifier.weight(0.1f),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = credential.isDefault,
                onCheckedChange = { onUpdate(credential.copy(isDefault = !credential.isDefault)) }
            )
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.width(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Close,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    isPassword: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        textStyle = TextStyle(
            color = AppColors.neutral140,
            fontSize = 12.sp
        ),
        cursorBrush = SolidColor(AppColors.primary),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() })
    )
}
