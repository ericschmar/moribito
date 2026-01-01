package com.moribito.gui.ui.components.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moribito.gui.ui.components.TabBar
import com.moribito.gui.ui.components.graph.DirectoryGraphView
import com.moribito.gui.viewmodel.RecordTab
import com.moribito.ldap.TreeNode

/**
 * Wrapper component that combines TabBar and RecordTable.
 * Displays tabs for open entries and shows the active entry's details.
 */
@Composable
fun TabbedRecordView(
    tabs: List<RecordTab>,
    activeTabId: String?,
    treeRoot: TreeNode?,
    onTabClick: (String) -> Unit,
    onTabDoubleClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNodeClick: (TreeNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Tab bar at top
        TabBar(
            tabs = tabs,
            activeTabId = activeTabId,
            onTabClick = onTabClick,
            onTabDoubleClick = onTabDoubleClick,
            onTabClose = onTabClose
        )

        // Record table for active tab
        val activeTab = tabs.find { it.id == activeTabId }
        when (activeTab) {
            is RecordTab.EntryTab -> {
                RecordTable(
                    entry = activeTab.entry,
                    modifier = Modifier.weight(1f)
                )
            }
            is RecordTab.GraphTab -> {
                DirectoryGraphView(
                    rootNode = treeRoot,
                    onNodeClick = onNodeClick,
                    modifier = Modifier.weight(1f)
                )
            }
            null -> {
                androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}
