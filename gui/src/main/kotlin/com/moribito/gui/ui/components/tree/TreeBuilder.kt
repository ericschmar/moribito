package com.moribito.gui.ui.components.tree

import com.moribito.ldap.TreeNode
import org.jetbrains.jewel.foundation.lazy.tree.buildTree as jewelBuildTree
import org.jetbrains.jewel.foundation.lazy.tree.Tree

/**
 * Builds a Jewel Tree from a TreeNode structure.
 * Handles intelligent grouping (cn, ou, others) and virtual member filtering.
 */
fun buildLdapTree(
    rootNode: TreeNode?,
    virtualMemberDNs: Set<String>,
    showVirtualMembers: Boolean
): Tree<TreeNode> {
    return jewelBuildTree {
        rootNode?.let { root ->
            addTreeNode(root, virtualMemberDNs, showVirtualMembers)
        }
    }
}

/**
 * Recursively adds a TreeNode and its children to the Jewel tree structure.
 * Applies intelligent grouping and virtual member filtering.
 */
private fun org.jetbrains.jewel.foundation.lazy.tree.TreeGeneratorScope<TreeNode>.addTreeNode(
    node: TreeNode,
    virtualMemberDNs: Set<String>,
    showVirtualMembers: Boolean
) {
    // Check if node can have children first (important for lazy loading)
    if (!node.hasChildren()) {
        // Leaf node
        addLeaf(node, id = node.id)
    } else {
        // Parent node - may or may not have loaded children yet
        val children = node.children

        addNode(node, id = node.id) {
            // Only add children if they've been loaded
            if (children != null && children.isNotEmpty()) {
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

                // Group children into 3 categories (intelligent grouping)
                val cnEntries = filteredChildren.filter { it.name.startsWith("cn=", ignoreCase = true) }
                val ouEntries = filteredChildren.filter { it.name.startsWith("ou=", ignoreCase = true) }
                val others = filteredChildren.filter {
                    !it.name.startsWith("cn=", ignoreCase = true) &&
                    !it.name.startsWith("ou=", ignoreCase = true)
                }

                // Render in order: cn, ou, others
                val orderedChildren = cnEntries + ouEntries + others

                orderedChildren.forEach { child ->
                    addTreeNode(child, virtualMemberDNs, showVirtualMembers)
                }
            }
            // If children is null or empty, the node will show as expandable but with no children yet
            // (lazy loading will populate them when expanded)
        }
    }
}

/**
 * Collects all DNs that appear as virtual members anywhere in the tree.
 * This recursively scans the entire tree, including loaded children.
 */
fun collectVirtualMemberDNs(node: TreeNode?): Set<String> {
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
