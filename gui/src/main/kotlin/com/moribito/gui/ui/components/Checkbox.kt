package com.moribito.gui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSizes

/**
 * Custom Checkbox component built from scratch for Zed design system.
 *
 * Uses Canvas for custom drawing to match Zed's aesthetic.
 * Reference: Jetsnack sample components
 */
@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    // Animated colors
    val checkboxColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.border.copy(alpha = 0.38f)
            checked -> AppColors.primary
            else -> AppColors.border
        },
        animationSpec = tween(durationMillis = 150),
        label = "checkbox color"
    )

    val checkmarkColor = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)

    // Animated checkmark progress (0f = unchecked, 1f = fully checked)
    val checkmarkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "checkmark progress"
    )

    Canvas(
        modifier = modifier
            .requiredSize(20.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null // No ripple for compact design
            )
    ) {
        val checkboxSize = size.minDimension
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = AppSizes.borderRadius.toPx()

        // Draw checkbox box
        if (checked) {
            // Filled box when checked
            drawRoundRect(
                color = checkboxColor,
                size = Size(checkboxSize, checkboxSize),
                cornerRadius = CornerRadius(cornerRadius)
            )
        } else {
            // Outlined box when unchecked
            drawRoundRect(
                color = checkboxColor,
                size = Size(checkboxSize, checkboxSize),
                cornerRadius = CornerRadius(cornerRadius),
                style = Stroke(width = strokeWidth)
            )
        }

        // Draw checkmark
        if (checkmarkProgress > 0f) {
            drawCheckmark(
                checkboxSize = checkboxSize,
                checkmarkColor = checkmarkColor,
                progress = checkmarkProgress,
                strokeWidth = strokeWidth
            )
        }
    }
}

/**
 * Draws the checkmark symbol with animation support
 */
private fun DrawScope.drawCheckmark(
    checkboxSize: Float,
    checkmarkColor: Color,
    progress: Float,
    strokeWidth: Float
) {
    val checkmarkPath = Path().apply {
        // Checkmark is drawn as two lines forming a check shape
        val padding = checkboxSize * 0.25f
        val checkWidth = checkboxSize - (padding * 2)
        val checkHeight = checkboxSize - (padding * 2)

        // Start point (left side of checkmark)
        moveTo(padding, padding + checkHeight * 0.5f)

        // Bottom point (bend of checkmark)
        lineTo(padding + checkWidth * 0.4f, padding + checkHeight * 0.8f)

        // End point (top right of checkmark)
        lineTo(padding + checkWidth, padding + checkHeight * 0.2f)
    }

    // Measure path to animate it
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(checkmarkPath, false)
    val pathLength = pathMeasure.length

    // Create partial path based on progress
    val partialPath = Path()
    pathMeasure.getSegment(0f, pathLength * progress, partialPath, true)

    // Draw the checkmark
    drawPath(
        path = partialPath,
        color = checkmarkColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}
