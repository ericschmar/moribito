package com.moribito.gui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.ui.components.Island
import com.moribito.gui.ui.components.query.QueryPanel
import com.moribito.gui.ui.components.record.RecordTable
import com.moribito.gui.ui.components.record.TabbedRecordView
import com.moribito.gui.ui.components.status.WorkspaceStatusBar
import com.moribito.gui.ui.components.tree.TreeView
import com.moribito.gui.ui.components.tree.TreeViewActionBar
import com.moribito.gui.viewmodel.AppState
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.ui.component.Text

/**
 * Workspace screen with 3-panel layout: tree navigation, query panel, and record table.
 * Uses nested split panes for resizable panels following IntelliJ Island design pattern.
 */
@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    state: AppState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(start = AppSpacing.sm, end = AppSpacing.sm)
    ) {
        // Main content area with nested split panes
        Box(modifier = Modifier.weight(1f)) {
            HorizontalSplitPane(
                splitPaneState = rememberSplitPaneState(0.25f), // 25/75 split
            ) {
                // Left panel: Tree navigation (25% width, min 200dp)
                first(minSize = 200.dp) {
                    Island(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = AppSpacing.xs),
                        padding = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TreeViewActionBar(
                                showVirtualMembers = state.showVirtualMembers,
                                onToggleVirtualMembers = {
                                    viewModel.toggleShowVirtualMembers()
                                },
                                isShowingQueryResults = state.isShowingQueryResults,
                                onShowDirectory = {
                                    viewModel.showDirectoryTree()
                                }
                            )
                            TreeView(
                                rootNode = if (state.isShowingQueryResults)
                                    state.queryResultsRoot
                                else
                                    state.treeRoot,
                                selectedNode = state.selectedNode,
                                showVirtualMembers = state.showVirtualMembers,
                                onNodeClick = { node ->
                                    viewModel.selectNode(node)
                                },
                                onNodeDoubleClick = { node ->
                                    viewModel.openPermanentTab(node)
                                },
                                onNodeExpand = { node ->
                                    viewModel.loadNodeChildren(node)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Right panel: Query + Record (75% width, min 400dp)
                second(minSize = 400.dp) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        VerticalSplitPane(
                            splitPaneState = rememberSplitPaneState(0.30f), // 30/70 split
                            modifier = Modifier.weight(1f)
                        ) {
                            // Top panel: Query input (30% height, min 120dp)
                            first(minSize = 120.dp) {
                                Island(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = AppSpacing.xs),
                                    padding = AppSpacing.sm
                                ) {
                                    QueryPanel(
                                        queryText = state.queryText,
                                        onQueryChange = { text ->
                                            viewModel.updateQueryText(text)
                                        },
                                        onFormat = {
                                            viewModel.formatQuery()
                                        },
                                        onRun = {
                                            viewModel.executeQuery()
                                        },
                                        isConnected = state.connectionState is ConnectionState.Connected
                                    )
                                }
                            }

                            // Bottom panel: Record table (70% height, min 200dp)
                            second(minSize = 200.dp) {
                                Island(
                                    modifier = Modifier.fillMaxSize(),
                                    padding = 0.dp
                                ) {
                                    TabbedRecordView(
                                        tabs = state.openTabs,
                                        activeTabId = state.activeTabId,
                                        onTabClick = { tabId -> viewModel.selectTab(tabId) },
                                        onTabDoubleClick = { tabId -> viewModel.makeTabPermanent(tabId) },
                                        onTabClose = { tabId -> viewModel.closeTab(tabId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Status bar
        WorkspaceStatusBar(
            connectionState = state.connectionState,
            centerContent = {
                // Display selected DN in center
                state.selectedNode?.let { node ->
                    Text(text = node.dn, fontSize = 11.sp)
                }
            },
            rightContent = {
                // Display attribute count in right section
                state.selectedEntry?.let { entry ->
                    Text(text = "${entry.attributes.size} attributes", fontSize = 11.sp)
                }
            }
        )
    }
}
