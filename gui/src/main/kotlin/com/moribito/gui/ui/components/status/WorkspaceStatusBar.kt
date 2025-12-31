package com.moribito.gui.ui.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import org.jetbrains.jewel.ui.component.Image
import org.jetbrains.jewel.ui.component.Text
import kotlinx.coroutines.delay

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
                    if (isInspectingSchema) {
                        Image(
                            iconKey = AppIcons.walkingIndicator,
                            contentDescription = "Inspecting schema indicator",
                            modifier = Modifier
                                .offset(x = xOffset)
                                .size(AppSizes.iconExtraLarge)
                        )
                    }
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
