package com.moribito.gui.ui.components.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moribito.gui.ui.components.TabBar
import com.moribito.gui.viewmodel.RecordTab

/**
 * Wrapper component that combines TabBar and RecordTable.
 * Displays tabs for open entries and shows the active entry's details.
 */
@Composable
fun TabbedRecordView(
    tabs: List<RecordTab>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabDoubleClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
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
        RecordTable(
            entry = activeTab?.entry,
            modifier = Modifier.weight(1f)
        )
    }
}
