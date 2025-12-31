package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Island container component following IntelliJ's "New UI" design pattern.
 *
 * Creates an elevated panel with:
 * - Subtle rounded corners (8dp)
 * - Light shadow for depth
 * - Slightly elevated background color
 * - Optional padding
 *
 * Used for major UI panels like tree view, query editor, and record table.
 */
@Composable
fun Island(
    modifier: Modifier = Modifier,
    padding: Dp = AppSpacing.md,
    cornerRadius: Dp = AppSizes.borderRadiusExtraLarge,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Use the darker editor/content background for islands (like IntelliJ's editor pane)
    // IntelliJ dark theme uses #191A1C for editor/panel backgrounds
    val backgroundColor = IntelliJColors.islandBackground
    val borderColor = JewelTheme.globalColors.borders.normal

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = AppSizes.borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(padding),
        content = content
    )
}

/**
 * Compact island variant with minimal padding for dense content.
 */
@Composable
fun CompactIsland(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Island(
        modifier = modifier,
        padding = AppSpacing.sm,
        content = content
    )
}

/**
 * Flat island variant without elevation, for subtle grouping.
 */
@Composable
fun FlatIsland(
    modifier: Modifier = Modifier,
    padding: Dp = AppSpacing.md,
    content: @Composable BoxScope.() -> Unit
) {
    Island(
        modifier = modifier,
        padding = padding,
        elevation = 0.dp,
        content = content
    )
}
