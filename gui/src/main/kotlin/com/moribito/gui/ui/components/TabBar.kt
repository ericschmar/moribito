package com.moribito.gui.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.viewmodel.RecordTab
import compose.icons.Octicons
import compose.icons.octicons.X16
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.HorizontallyScrollableContainer
import org.jetbrains.jewel.ui.component.Icon

/**
 * Tab bar component for managing multiple record tabs.
 * Uses Island theme style with subtle borders and backgrounds.
 * Temporary tabs are shown in italic without close buttons.
 * Permanent tabs have regular text with close buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabBar(
    tabs: List<RecordTab>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabDoubleClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontallyScrollableContainer(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(
                color = IntelliJColors.islandBackground,
                shape = RoundedCornerShape(
                    topStart = AppSizes.borderRadiusExtraLarge,
                    topEnd = AppSizes.borderRadiusExtraLarge,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onDoubleClick = { onTabDoubleClick(tab.id) },
                    onClose = { onTabClose(tab.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TabItem(
    tab: RecordTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onClose: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isActive && isHovered -> Color(0xFF4A90E2).copy(alpha = 0.4f) // Lighter blue on hover
        isActive -> Color(0xFF4A90E2).copy(alpha = 0.3f) // Light blue background
        isHovered -> Color(0xFF4A90E2).copy(alpha = 0.15f) // Subtle hover for inactive tabs
        else -> Color.Transparent
    }

    val borderColor = if (isActive) {
        Color(0xFF2E5F8E) // Darker blue border
    } else {
        Color.Transparent
    }

    val textColor = if (isActive) {
        JewelTheme.contentColor
    } else {
        JewelTheme.contentColor.copy(alpha = 0.6f)
    }

    val fontStyle = if (tab.isTemporary) FontStyle.Italic else FontStyle.Normal

    Row(
        modifier = Modifier
            .height(24.dp)
            .drawBehind {
                // Draw border for active tab (Island style) with rounded corners
                if (isActive) {
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .background(backgroundColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = if (tab.isTemporary) onDoubleClick else null
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tab text
        Text(
            text = tab.displayName,
            color = textColor,
            fontStyle = fontStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .size(16.dp)
                .combinedClickable(
                    onClick = { onClose() },
                    onDoubleClick = {} // Prevent double-click from propagating
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Octicons.X16,
                contentDescription = "Close tab",
                modifier = Modifier.size(12.dp),
                tint = textColor.copy(alpha = 0.8f)
            )
        }
    }
}
