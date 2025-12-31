package com.moribito.gui.ui.components.tree

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSpacing
import com.moribito.ldap.TreeNode
import compose.icons.Octicons
import compose.icons.octicons.ChevronDown16
import compose.icons.octicons.ChevronRight16
import compose.icons.octicons.File16
import compose.icons.octicons.FileDirectory16
import compose.icons.octicons.Person16
import org.jetbrains.jewel.foundation.theme.JewelTheme
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TreeNodeItem(
    node: TreeNode,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onInspectAttributes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    // Match Tab styling exactly
    val backgroundColor = when {
        isSelected && isHovered -> Color(0xFF4A90E2).copy(alpha = 0.4f) // Lighter blue on hover
        isSelected -> Color(0xFF4A90E2).copy(alpha = 0.3f) // Light blue background
        isHovered -> Color(0xFF4A90E2).copy(alpha = 0.15f) // Subtle hover for inactive items
        else -> Color.Transparent
    }

    val borderColor = if (isSelected) {
        Color(0xFF2E5F8E) // Darker blue border
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        JewelTheme.contentColor
    } else {
        JewelTheme.contentColor.copy(alpha = 0.6f)
    }

    ContextMenuArea(
        items = {
            if (node.hasChildren()) {
                listOf(
                    ContextMenuItem("Inspect Attributes") {
                        onInspectAttributes(node.dn)
                    }
                )
            } else emptyList()
        }
    ) {
        Row(
            modifier = modifier
                .height(26.dp)
                .padding(start = (level * 16).dp + AppSpacing.xxs)
                .drawBehind {
                    // Draw border for selected item (Island style) with rounded corners
                    if (isSelected) {
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
                    onDoubleClick = onDoubleClick
                )
                .padding(start = 4.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Expand/collapse chevron (12dp)
            if (node.hasChildren()) {
                Icon(
                    imageVector = if (isExpanded) Octicons.ChevronDown16 else Octicons.ChevronRight16,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(12.dp)
                        .clickable(onClick = onToggleExpand),
                    tint = AppColors.neutral140
                )
            }

            // Folder/file icon (12dp)
            Icon(
                imageVector = if (node.hasChildren()) Octicons.FileDirectory16 else Octicons.Person16,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (node.hasChildren()) AppColors.blue100 else AppColors.neutral60
            )

            // Node name
            Text(
                text = node.name,
                color = textColor,
            )
        }
    }
}
