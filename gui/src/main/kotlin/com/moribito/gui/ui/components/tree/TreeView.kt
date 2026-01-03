package com.moribito.gui.ui.components.tree

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppColors
import com.moribito.ldap.TreeNode
import org.jetbrains.jewel.foundation.lazy.SelectableLazyItemScope
import org.jetbrains.jewel.foundation.lazy.tree.BasicLazyTree
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.TreeElementState
import org.jetbrains.jewel.foundation.lazy.tree.rememberTreeState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Tree view component with intelligent grouping of LDAP entries using Jewel's LazyTree.
 * Groups children into: cn entries (Service Accounts), ou entries (Organizational Units), and others.
 * Implements lazy loading by tracking expanded nodes.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TreeView(
    rootNode: TreeNode?,
    selectedNode: TreeNode?,
    showVirtualMembers: Boolean,
    onNodeClick: (TreeNode) -> Unit,
    onNodeDoubleClick: (TreeNode) -> Unit,
    onNodeExpand: (TreeNode) -> Unit,
    onInspectAttributes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track all DNs that appear as virtual members to filter them from hierarchy when needed
    // Recompute whenever rootNode changes OR showVirtualMembers changes
    val virtualMemberDNs = remember(rootNode, showVirtualMembers) {
        if (showVirtualMembers) {
            collectVirtualMemberDNs(rootNode)
        } else {
            emptySet()
        }
    }

    // Build the Jewel tree structure
    val tree = remember(rootNode, virtualMemberDNs, showVirtualMembers) {
        buildLdapTree(rootNode, virtualMemberDNs, showVirtualMembers)
    }

    val treeState = rememberTreeState()

    // Track expansion changes to trigger lazy loading
    LaunchedEffect(treeState.openNodes) {
        // When a node is expanded, trigger lazy loading
        treeState.openNodes.forEach { nodeId ->
            // Find the corresponding TreeNode
            val element = tree.roots.firstOrNull()?.let { findElementById(it, nodeId) }
            element?.data?.let { onNodeExpand(it) }
        }
    }

    BasicLazyTree(
        tree = tree,
        modifier = modifier.fillMaxSize(),
        treeState = treeState,
        onElementClick = { element ->
            onNodeClick(element.data)
        },
        onElementDoubleClick = { element ->
            onNodeDoubleClick(element.data)
        },
        onSelectionChange = { /* Handle selection if needed */ },
        // Compact styling to match the existing design
        elementMinHeight = 26.dp,
        indentSize = 16.dp,
        elementPadding = PaddingValues(start = 2.dp, end = 5.dp),
        elementContentPadding = PaddingValues(start = 4.dp, end = 5.dp),
        chevronContentGap = 4.dp,
        // Transparent backgrounds - we'll handle selection/hover in nodeContent
        elementBackgroundFocused = Color.Transparent,
        elementBackgroundSelected = Color.Transparent,
        elementBackgroundSelectedFocused = Color.Transparent,
        elementBackgroundCornerSize = androidx.compose.foundation.shape.CornerSize(6.dp),
        chevronContent = { nodeState ->
            if (nodeState.isExpanded) {
                Icon(
                    key = AllIconsKeys.General.ChevronDown,
                    contentDescription = "Collapse",
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.neutral140
                )
            } else {
                Icon(
                    key = AllIconsKeys.General.ChevronRight,
                    contentDescription = "Expand",
                    modifier = Modifier.size(14.dp),
                    tint = AppColors.neutral140
                )
            }
        },
        nodeContent = { element ->
            TreeNodeContent(
                node = element.data,
                isSelected = selectedNode?.id == element.data.id,
                onInspectAttributes = onInspectAttributes
            )
        }
    )
}

/**
 * Recursively finds a tree element by its ID.
 */
private fun <T> findElementById(element: Tree.Element<T>, id: Any): Tree.Element<T>? {
    if (element.id == id) return element

    // Only nodes have children
    if (element is Tree.Element.Node<T>) {
        element.children?.forEach { child ->
            findElementById(child, id)?.let { return it }
        }
    }

    return null
}

/**
 * Custom node content that matches the existing TreeNodeItem styling.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SelectableLazyItemScope.TreeNodeContent(
    node: TreeNode,
    isSelected: Boolean,
    onInspectAttributes: (String) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    // Match existing Tab styling
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
            modifier = Modifier
                .height(26.dp)
                .drawBehind {
                    // Draw border for selected item (Island style) with rounded corners
                    if (isSelected) {
                        drawRoundRect(
                            color = borderColor,
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                .background(backgroundColor, shape = RoundedCornerShape(6.dp))
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                .padding(start = 4.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Folder/file icon (12dp)
            Icon(
                key = if (node.hasChildren()) AllIconsKeys.Nodes.Folder else AllIconsKeys.General.User,
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
