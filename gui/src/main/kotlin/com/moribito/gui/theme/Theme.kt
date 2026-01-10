package com.moribito.gui.theme

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Application theme for Moribito.
 *
 * This theme combines Material 3 to create a cohesive design system:
 * - Material 3 provides the color scheme, typography, and shapes
 * - CompositionLocals set default styling for all text and content
 *
 * Usage:
 * ```kotlin
 * AppTheme {
 *     // Your app content here
 *     ConfigurationScreen(...)
 * }
 * ```
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalContentColor provides AppColors.textPrimary,
        LocalTextStyle provides AppTypography.bodyMedium,
    ) {
        MaterialTheme(
            colorScheme = AppColors.colorScheme,
            typography = AppTypography,
            shapes = Shapes(
                extraSmall = AppShapes.small,
                small = AppShapes.medium,
                medium = AppShapes.large,
                large = AppShapes.extraLarge,
                extraLarge = AppShapes.extraLarge
            ),
            content = content
        )
    }
}

/**
 * Preview theme for testing individual components.
 * Same as AppTheme but can be used in Compose Desktop previews.
 */
@Composable
fun AppThemePreview(
    content: @Composable () -> Unit
) {
    AppTheme(content = content)
}
