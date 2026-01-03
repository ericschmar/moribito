package com.moribito.gui.ui.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.viewmodel.ConnectionState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import com.moribito.gui.ui.icons.AppIcons
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.GifImage
import com.moribito.gui.ui.components.editor.drawing.AnimatedGirl
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.Text

/**
 * Status bar for the workspace screen with three sections: left (status chip), center, and right.
 * Follows IntelliJ design patterns with fixed height and neutral background.
 */
@Composable
fun WorkspaceStatusBar(
    connectionState: ConnectionState,
    isInspectingSchema: Boolean = false,
    schemaInspectionProgress: Float = 0f,
    schemaInspectionStatus: String? = null,
    modifier: Modifier = Modifier,
    centerContent: @Composable RowScope.() -> Unit = {},
    rightContent: @Composable RowScope.() -> Unit = {}
) {

    var xOffset by remember { mutableStateOf(0.dp) }

    LaunchedEffect(isInspectingSchema) {
        if (isInspectingSchema) {
            while (true) {
                delay(250)
                // Walk from -60dp to 60dp in steps
                xOffset += 4.dp
                if (xOffset > 60.dp) {
                    xOffset = (-60).dp
                }
            }
        } else {
            xOffset = 0.dp
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppSizes.statusBarHeight)
            .background(IntelliJColors.baseBackground)
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

        // Center section: Custom content or Schema Inspection Progress
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isInspectingSchema || schemaInspectionStatus != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedGirl(animate = isInspectingSchema)
                }
            } else {
                centerContent()
            }
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
