package com.moribito.gui.ui.components.tree

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.moribito.ldap.TreeNode
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer

/**
 * Tree view component with intelligent grouping of LDAP entries.
 * Groups children into: cn entries (Service Accounts), ou entries (Organizational Units), and others.
 * Implements lazy loading by tracking expanded nodes.
 */
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
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }

    // Track all DNs that appear as virtual members to filter them from hierarchy when needed
    // Recompute whenever rootNode changes OR showVirtualMembers changes
    val virtualMemberDNs = remember(rootNode, showVirtualMembers) {
        if (showVirtualMembers) {
            collectVirtualMemberDNs(rootNode)
        } else {
            emptySet()
        }
    }

    VerticallyScrollableContainer(
        modifier = modifier.fillMaxSize()
    ) {
        Column {
            rootNode?.let { root ->
                RenderTreeNode(
                    node = root,
                    level = 0,
                    selectedNode = selectedNode,
                    expandedNodes = expandedNodes,
                    virtualMemberDNs = virtualMemberDNs,
                    showVirtualMembers = showVirtualMembers,
                    onNodeClick = onNodeClick,
                    onNodeDoubleClick = onNodeDoubleClick,
                    onToggleExpand = { node ->
                        if (expandedNodes.contains(node.id)) {
                            expandedNodes = expandedNodes - node.id
                        } else {
                            expandedNodes = expandedNodes + node.id
                            // Trigger lazy loading if needed
                            onNodeExpand(node)
                        }
                    },
                    onInspectAttributes = onInspectAttributes
                )
            }
        }
    }
}

/**
 * Collects all DNs that appear as virtual members anywhere in the tree.
 * This recursively scans the entire tree, including loaded children.
 */
private fun collectVirtualMemberDNs(node: TreeNode?): Set<String> {
    if (node == null) return emptySet()

    val dns = mutableSetOf<String>()

    // Recursively scan all children (even in unloaded nodes, check their children if loaded)
    node.children?.forEach { child ->
        if (child.isVirtualMember) {
            dns.add(child.dn.lowercase())
        }
        // Recursively check this child's children
        dns.addAll(collectVirtualMemberDNs(child))
    }

    return dns
}

@Composable
private fun RenderTreeNode(
    node: TreeNode,
    level: Int,
    selectedNode: TreeNode?,
    expandedNodes: Set<String>,
    virtualMemberDNs: Set<String>,
    showVirtualMembers: Boolean,
    onNodeClick: (TreeNode) -> Unit,
    onNodeDoubleClick: (TreeNode) -> Unit,
    onToggleExpand: (TreeNode) -> Unit,
    onInspectAttributes: (String) -> Unit
) {
    val isExpanded = expandedNodes.contains(node.id)
    val isSelected = selectedNode?.id == node.id

    TreeNodeItem(
        node = node,
        level = level,
        isExpanded = isExpanded,
        isSelected = isSelected,
        onClick = { onNodeClick(node) },
        onDoubleClick = { onNodeDoubleClick(node) },
        onToggleExpand = { onToggleExpand(node) },
        onInspectAttributes = onInspectAttributes
    )

    // Render children if expanded
    val children = node.children
    if (isExpanded && children != null && children.isNotEmpty()) {
        // Filter children based on virtual members setting
        val filteredChildren = children.filter { child ->
            when {
                // If virtual members are shown, include virtual members and exclude
                // hierarchical children that are also virtual members elsewhere
                showVirtualMembers -> {
                    // Always show virtual members
                    if (child.isVirtualMember) {
                        true
                    } else {
                        // Hide hierarchical children if they appear as virtual members
                        !virtualMemberDNs.contains(child.dn.lowercase())
                    }
                }
                // If virtual members are NOT shown, only show hierarchical children
                else -> {
                    !child.isVirtualMember
                }
            }
        }

        // Group children into 3 categories
        val cnEntries = filteredChildren.filter { it.name.startsWith("cn=", ignoreCase = true) }
        val ouEntries = filteredChildren.filter { it.name.startsWith("ou=", ignoreCase = true) }
        val others = filteredChildren.filter {
            !it.name.startsWith("cn=", ignoreCase = true) &&
            !it.name.startsWith("ou=", ignoreCase = true)
        }

        // Render in order: cn, ou, others
        val orderedChildren = cnEntries + ouEntries + others

        orderedChildren.forEach { child ->
            RenderTreeNode(
                node = child,
                level = level + 1,
                selectedNode = selectedNode,
                expandedNodes = expandedNodes,
                virtualMemberDNs = virtualMemberDNs,
                showVirtualMembers = showVirtualMembers,
                onNodeClick = onNodeClick,
                onNodeDoubleClick = onNodeDoubleClick,
                onToggleExpand = onToggleExpand,
                onInspectAttributes = onInspectAttributes
            )
        }
    }
}
