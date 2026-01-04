package com.moribito.gui.viewmodel

import com.moribito.logging.LogEntry
import com.moribito.logging.LogLevel
import com.moribito.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * ViewModel for managing application logs.
 */
class LogViewModel(
    private val stateHolder: AppStateHolder
) {
    private val logger = Logger.get("LogViewModel")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var autoRefreshJob: Job? = null

    /**
     * Opens the log viewer.
     */
    fun openLogViewer() {
        stateHolder.update { it.copy(isLogViewerOpen = true) }
        loadCurrentLogFile()
        startAutoRefreshLogs()
    }

    /**
     * Closes the log viewer.
     */
    fun closeLogViewer() {
        stateHolder.update { it.copy(isLogViewerOpen = false) }
        stopAutoRefreshLogs()
    }

    /**
     * Starts auto-refreshing logs.
     */
    fun startAutoRefreshLogs() {
        if (autoRefreshJob?.isActive == true) return

        autoRefreshJob = scope.launch {
            while (isActive) {
                loadCurrentLogFile()
                delay(2000) // Refresh every 2 seconds
            }
        }
    }

    /**
     * Stops auto-refreshing logs.
     */
    fun stopAutoRefreshLogs() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * Loads the current log file.
     */
    fun loadCurrentLogFile() {
        val logFile = Logger.getCurrentLogFile()
        
        if (!logFile.exists()) return

        try {
            val lines = logFile.readLines().takeLast(1000) // Show last 1000 lines
            val entries = lines.mapNotNull { line ->
                LogEntry.parse(line)
            }

            stateHolder.update { it.copy(
                logEntries = entries,
                logFilePath = logFile.absolutePath
            )}
        } catch (e: Exception) {
            logger.error("Failed to load log file", e)
        }
    }

    /**
     * Updates the log search query.
     */
    fun updateLogSearchQuery(query: String) {
        stateHolder.update { it.copy(logSearchQuery = query) }
    }

    /**
     * Toggles a log level filter.
     */
    fun toggleLogLevelFilter(level: LogLevel) {
        stateHolder.update { state ->
            val newFilters = state.logLevelFilter.toMutableSet()
            if (newFilters.contains(level)) {
                newFilters.remove(level)
            } else {
                newFilters.add(level)
            }
            state.copy(logLevelFilter = newFilters)
        }
    }

    /**
     * Toggles log auto-scroll.
     */
    fun toggleLogAutoScroll() {
        stateHolder.update { it.copy(logAutoScroll = !it.logAutoScroll) }
    }

    fun cleanup() {
        stopAutoRefreshLogs()
        scope.cancel()
    }
}
