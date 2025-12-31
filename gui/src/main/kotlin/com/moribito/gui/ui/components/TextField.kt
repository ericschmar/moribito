package com.moribito.gui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.theme.textFieldStyle

/**
 * Custom TextField component with Gruvbox theming and validation support.
 *
 * Uses BasicTextField with custom decoration for full control over colors and styling.
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    isRequired: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animated border color based on focus and error state
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.borderVariant.copy(alpha = 0.5f)
            isError -> AppColors.error
            isFocused -> AppColors.primary
            else -> AppColors.border
        },
        animationSpec = tween(durationMillis = 150),
        label = "border color"
    )

    Column(modifier = modifier) {
        // Label above field with required indicator
        if (label != null) {
            Row(
                modifier = Modifier.padding(bottom = AppSpacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxxs)
            ) {
                Text(
                    text = label,
                    style = AppTypography.labelMedium,
                    color = when {
                        !enabled -> JewelTheme.globalColors.text.disabled
                        isError -> JewelTheme.globalColors.text.error
                        else -> JewelTheme.globalColors.text.info
                    }
                )
                if (isRequired) {
                    Text(
                        text = "*",
                        style = AppTypography.labelMedium,
                        color = JewelTheme.globalColors.text.error
                    )
                }
            }
        }

        // Text field container with BasicTextField for full color control
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppSizes.inputHeightStandard),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = LocalTextStyle.current.copy(
                color = if (enabled) JewelTheme.contentColor else JewelTheme.contentColor.copy(alpha = 0.5f),
                lineHeight = TextUnit.Unspecified,
                fontSize = 12.sp
            ),
            cursorBrush = SolidColor(AppColors.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = AppSizes.borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(AppSizes.borderRadius)
                        )
                        .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxxs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    // Leading icon with spacing
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier.size(AppSizes.iconSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            leadingIcon()
                        }
                    }

                    // Text field with placeholder
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Placeholder text - dimmer Gruvbox color
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = LocalTextStyle.current,
                                color = JewelTheme.textFieldStyle.colors.placeholder  // Dimmer version
                            )
                        }

                        // Actual text field
                        innerTextField()
                    }

                    // Trailing icon
                    if (trailingIcon != null) {
                        Box(
                            modifier = Modifier.size(AppSizes.iconSmall),
                            contentAlignment = Alignment.Center
                        ) {
                            trailingIcon()
                        }
                    }
                }
            }
        )

        // Error message below field
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = AppTypography.labelSmall,
                color = AppColors.error,
                modifier = Modifier.padding(top = AppSpacing.xxxs, start = AppSpacing.xxs)
            )
        }
    }
}
