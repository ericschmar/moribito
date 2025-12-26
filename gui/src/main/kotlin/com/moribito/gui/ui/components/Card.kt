package com.moribito.gui.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing

/**
 * Custom Card component built from scratch for Zed design system.
 *
 * Uses Surface with custom styling for Zed's flat, bordered aesthetic.
 * No elevation - matches Zed's flat design language.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppSizes.borderRadiusLarge),
    backgroundColor: Color = AppColors.surface,
    contentColor: Color = AppColors.textPrimary,
    border: BorderStroke = BorderStroke(AppSizes.borderWidth, AppColors.border),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = 0.dp, // No elevation - flat design like Zed
        shadowElevation = 0.dp,
        border = border
    ) {
        Column(content = content)
    }
}

/**
 * Card variant without border for cleaner look
 */
@Composable
fun CardNoBorder(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppSizes.borderRadiusLarge),
    backgroundColor: Color = AppColors.surface,
    contentColor: Color = AppColors.textPrimary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(0.dp, Color.Transparent),
        content = content
    )
}

/**
 * Card with default padding applied
 */
@Composable
fun PaddedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppSizes.borderRadiusLarge),
    backgroundColor: Color = AppColors.surface,
    contentColor: Color = AppColors.textPrimary,
    border: BorderStroke = BorderStroke(AppSizes.borderWidth, AppColors.border),
    contentPadding: Modifier = Modifier.padding(AppSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        border = border
    ) {
        Column(
            modifier = contentPadding,
            content = content
        )
    }
}
