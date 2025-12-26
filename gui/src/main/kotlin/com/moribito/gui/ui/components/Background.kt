package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.moribito.gui.theme.AppColors

/**
 * Custom Background component for Zed design system.
 *
 * Replaces Fluent's Mica component with Zed-themed backgrounds.
 * Supports solid backgrounds and optional translucent/layered effects.
 */
@Composable
fun Background(
    modifier: Modifier = Modifier,
    style: BackgroundStyle = BackgroundStyle.Solid,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundModifier = when (style) {
        BackgroundStyle.Solid -> modifier.background(AppColors.background)
        BackgroundStyle.Surface -> modifier.background(AppColors.surface)
        is BackgroundStyle.Translucent -> modifier.background(AppColors.surface.copy(alpha = style.alpha))
        is BackgroundStyle.Layered -> modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    AppColors.background,
                    AppColors.surface.copy(alpha = 0.5f),
                    AppColors.background
                )
            )
        )
    }

    Box(
        modifier = backgroundModifier,
        content = content
    )
}

/**
 * Background style options for Zed design system
 */
sealed class BackgroundStyle {
    /**
     * Solid background using AppColors.background (#1e1e1e)
     */
    object Solid : BackgroundStyle()

    /**
     * Surface background using AppColors.surface (#252525)
     */
    object Surface : BackgroundStyle()

    /**
     * Translucent surface with configurable opacity (Mica-like effect)
     * @param alpha Opacity level (0f = transparent, 1f = opaque)
     */
    data class Translucent(val alpha: Float = 0.95f) : BackgroundStyle()

    /**
     * Layered background with subtle gradient effect
     */
    object Layered : BackgroundStyle()
}

/**
 * Convenience composable for Mica-like translucent background
 */
@Composable
fun MicaBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.95f,
    content: @Composable BoxScope.() -> Unit
) {
    Background(
        modifier = modifier,
        style = BackgroundStyle.Translucent(alpha),
        content = content
    )
}
