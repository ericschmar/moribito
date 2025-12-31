package com.moribito.gui.ui.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.viewmodel.ConnectionState
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Status bar for the workspace screen with three sections: left (status chip), center, and right.
 * Follows IntelliJ design patterns with fixed height and neutral background.
 */
@Composable
fun WorkspaceStatusBar(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    centerContent: @Composable RowScope.() -> Unit = {},
    rightContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSizes.statusBarHeight)
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section: Status chip
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(state = connectionState)
        }

        // Center section: Custom content
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            centerContent()
        }

        // Right section: Custom content
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            rightContent()
        }
    }
}
