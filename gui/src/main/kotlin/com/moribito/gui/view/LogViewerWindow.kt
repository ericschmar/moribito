package com.moribito.gui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Background
import com.moribito.gui.viewmodel.MainViewModel
import com.moribito.logging.LogEntry
import com.moribito.logging.LogLevel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.createEditorTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarStyle
import com.moribito.gui.ui.components.TextField as AppTextField

/**
 * Log viewer window that displays application logs with search and filtering capabilities.
 */
@Composable
fun LogViewerWindow(
    viewModel: MainViewModel,
    onCloseRequest: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val textStyle = JewelTheme.createDefaultTextStyle()
    val editorStyle = JewelTheme.createEditorTextStyle()

    val themeDefinition = JewelTheme.darkThemeDefinition(
        defaultTextStyle = textStyle,
        editorTextStyle = editorStyle
    )

    IntUiTheme(
        theme = themeDefinition,
        styling = ComponentStyling.default()
            .decoratedWindow(
                titleBarStyle = TitleBarStyle.dark(
                    colors = TitleBarColors.dark(
                        backgroundColor = IntelliJColors.baseBackground,
                        borderColor = Color(0xFF2B2D30),
                    )
                )
            ),
    ) {
        val windowState = rememberWindowState(width = 1000.dp, height = 700.dp)
        DecoratedWindow(
            state = windowState,
            onCloseRequest = onCloseRequest,
            title = "Log Viewer - Moribito",
            content = {
                TitleBar(Modifier.newFullscreenControls()) {
                    Text(title, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Background(modifier = Modifier.fillMaxSize()) {
                    LogViewerScreen(viewModel = viewModel)
                }
            },
        )
    }
}

/**
 * Main screen content for the log viewer.
 */
@Composable
private fun LogViewerScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        AppTextField(
            value = state.logSearchQuery,
            onValueChange = { viewModel.updateLogSearchQuery(it) },
            placeholder = "Search logs...",
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (state.logSearchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { viewModel.updateLogSearchQuery("") },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            key = AllIconsKeys.Actions.Close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(12.dp),
                            tint = JewelTheme.contentColor.copy(alpha = 0.6f)
                        )
                    }
                }
            } else null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Level filter checkboxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LogLevel.values().forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = level in state.logLevelFilter,
                        onCheckedChange = { viewModel.toggleLogLevelFilter(level) }
                    )

                    val levelColor = when (level) {
                        LogLevel.ERROR -> Color(0xFFFF6B6B)
                        LogLevel.WARN -> Color(0xFFFECA57)
                        LogLevel.INFO -> Color(0xFF48DBFB)
                        LogLevel.DEBUG -> Color(0xFF9B9B9B)
                    }

                    Text(
                        text = level.displayName,
                        color = levelColor,
                        style = JewelTheme.defaultTextStyle
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log entries list
        Box(modifier = Modifier.weight(1f)) {
            LogEntriesList(
                entries = state.logEntries,
                searchQuery = state.logSearchQuery,
                levelFilter = state.logLevelFilter,
                autoScroll = state.logAutoScroll
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer with auto-scroll toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total entries: ${state.logEntries.size}",
                color = JewelTheme.contentColor.copy(alpha = 0.6f),
                style = JewelTheme.defaultTextStyle
            )
        }
    }
}

/**
 * Scrollable list of log entries with filtering.
 */
@Composable
private fun LogEntriesList(
    entries: List<LogEntry>,
    searchQuery: String,
    levelFilter: Set<LogLevel>,
    autoScroll: Boolean
) {
    val filteredEntries = remember(entries, searchQuery, levelFilter) {
        entries
            .filter { it.level in levelFilter }
            .filter { entry ->
                if (searchQuery.isBlank()) true
                else entry.format().contains(searchQuery, ignoreCase = true)
            }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(filteredEntries.size, autoScroll) {
        if (autoScroll && filteredEntries.isNotEmpty()) {
            listState.animateScrollToItem(filteredEntries.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IntelliJColors.islandBackground)
    ) {
        if (filteredEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entries.isEmpty()) "No log entries" else "No matching entries",
                    color = JewelTheme.contentColor.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEntries) { entry ->
                    LogEntryRow(entry = entry)
                }
            }
        }
    }
}

/**
 * Single row displaying a log entry with color coding.
 */
@Composable
private fun LogEntryRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFFF6B6B)
        LogLevel.WARN -> Color(0xFFFECA57)
        LogLevel.INFO -> Color(0xFF48DBFB)
        LogLevel.DEBUG -> Color(0xFF9B9B9B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Timestamp
        Text(
            text = entry.timestamp.toString().substring(0, 23),
            color = JewelTheme.contentColor.copy(alpha = 0.6f),
            style = JewelTheme.defaultTextStyle,
            modifier = Modifier.width(180.dp)
        )

        // Level badge
        Text(
            text = "[${entry.level.displayName}]",
            color = levelColor,
            style = JewelTheme.defaultTextStyle,
            modifier = Modifier.width(80.dp)
        )

        // Component
        Text(
            text = "[${entry.component}]",
            color = JewelTheme.contentColor.copy(alpha = 0.7f),
            style = JewelTheme.defaultTextStyle,
            modifier = Modifier.width(150.dp)
        )

        // Message
        Text(
            text = entry.message,
            color = JewelTheme.contentColor,
            style = JewelTheme.defaultTextStyle,
            modifier = Modifier.weight(1f)
        )
    }
}
