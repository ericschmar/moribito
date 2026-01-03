package com.moribito.gui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Island
import com.moribito.gui.ui.components.query.QueryPanel
import com.moribito.gui.ui.components.record.RecordTable
import com.moribito.gui.ui.components.record.TabbedRecordView
import com.moribito.gui.ui.components.status.WorkspaceStatusBar
import com.moribito.gui.ui.components.tree.TreeView
import com.moribito.gui.ui.components.tree.TreeViewActionBar
import com.moribito.gui.viewmodel.AppState
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.LoadingState
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.VerticalSplitPane
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

import com.moribito.gui.ui.components.MainToolbar
import com.moribito.gui.ui.components.schema.AttributeViewer

/**
 * Workspace screen with resizable panels following IntelliJ Island design pattern.
 * Tree navigation on the left, Main content (Query + Record) in center,
 * optional Attribute Viewer on the right, and a fixed Toolbar on the far right.
 */
@OptIn(ExperimentalSplitPaneApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WorkspaceScreen(
    viewModel: MainViewModel,
    state: AppState,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(IntelliJColors.baseBackground)
                .padding(start = AppSpacing.sm)
        ) {
            // Main content area with nested split panes
            Box(modifier = Modifier.weight(1f)) {
                HorizontalSplitPane(
                    splitPaneState = rememberSplitPaneState(0.25f), // Left Tree split
                ) {
                    // Left panel: Tree navigation
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
                                        state.errorMessage = null
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
                                    onInspectAttributes = { dn ->
                                        viewModel.loadOuAttributes(dn)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Right section: (Main Content | Attribute Viewer)
                    second(minSize = 400.dp) {
                        if (state.isAttributeViewerOpen) {
                            HorizontalSplitPane(
                                splitPaneState = rememberSplitPaneState(0.7f), // Center/Right split
                            ) {
                                first(minSize = 300.dp) {
                                    MainContentPanel(viewModel, state)
                                }
                                second(minSize = 200.dp) {
                                    Island(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = AppSpacing.xs),
                                        padding = 0.dp
                                    ) {
                                        AttributeViewer(
                                            schema = state.attributeViewerSchema,
                                            sortAscending = state.attributeSortAscending,
                                            onToggleSort = { viewModel.toggleAttributeSort() },
                                            onClose = { viewModel.toggleAttributeViewer(false) },
                                            isLoading = state.isAttributeViewerLoading
                                        )
                                    }
                                }
                            }
                        } else {
                            MainContentPanel(viewModel, state)
                        }
                    }
                }
            }

            // Status bar
            WorkspaceStatusBar(
                connectionState = state.connectionState,
                isInspectingSchema = state.isInspectingSchema,
                schemaInspectionProgress = state.schemaInspectionProgress,
                schemaInspectionStatus = state.schemaInspectionStatus,
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

                    Spacer(modifier = Modifier.width(AppSpacing.xs))

                    // Log viewer button
                    Tooltip(tooltip = { Text("View Logs") }) {
                        IconButton(
                            onClick = { viewModel.openLogViewer() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                key = AllIconsKeys.Actions.Preview,
                                contentDescription = "View Logs",
                                modifier = Modifier.size(16.dp),
                                tint = JewelTheme.globalColors.text.normal
                            )
                        }
                    }
                }
            )
        }

        // Right Toolbar
        MainToolbar(
            onInspectSchema = {
                viewModel.inspectSchema()
                viewModel.toggleAttributeViewer(true)
            },
            onOpenGraph = {
                viewModel.openDirectoryGraph()
            }
        )
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
private fun MainContentPanel(
    viewModel: MainViewModel,
    state: AppState
) {
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
                            println("Executing query")
                            viewModel.executeQuery()
                        },
                        error = state.errorMessage,
                        isConnected = state.connectionState is ConnectionState.Connected,
                        schema = state.schema
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
                        treeRoot = state.treeRoot,
                        onTabClick = { tabId -> viewModel.selectTab(tabId) },
                        onTabDoubleClick = { tabId -> viewModel.makeTabPermanent(tabId) },
                        onTabClose = { tabId -> viewModel.closeTab(tabId) },
                        onNodeClick = { node -> viewModel.handleGraphNodeClick(node) }
                    )
                }
            }
        }
    }
}
