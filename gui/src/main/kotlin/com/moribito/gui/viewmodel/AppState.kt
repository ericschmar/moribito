package com.moribito.gui.viewmodel

import com.moribito.gui.license.AccessStatus
import com.moribito.gui.license.LicenseResult
import com.moribito.ldap.Entry
import com.moribito.ldap.TreeNode
import com.moribito.ldap.LdapSchema
import com.moribito.ldap.LdapAttribute
import com.moribito.logging.LogEntry
import com.moribito.logging.LogLevel

/**
 * Represents the different views in the application.
 */
sealed class AppView {
    object Start : AppView()
    object Configuration : AppView()
    object Workspace : AppView()
}

/**
 * Represents the connection state to the LDAP server.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Represents the loading state for async operations.
 */
sealed class LoadingState {
    object Idle : LoadingState()
    data class Loading(val operation: String) : LoadingState()
    data class Success(val message: String? = null) : LoadingState()
    data class Failed(val error: String) : LoadingState()
}

/**
 * Represents a single tab in the record viewer.
 */
sealed interface RecordTab {
    val id: String
    val displayName: String
    val isTemporary: Boolean

    data class EntryTab(
        override val id: String = java.util.UUID.randomUUID().toString(),
        val dn: String,
        override val displayName: String,
        val entry: Entry?,
        override val isTemporary: Boolean = true,
        val loadingState: LoadingState = LoadingState.Idle
    ) : RecordTab

    data class GraphTab(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val displayName: String = "Directory Tree",
        override val isTemporary: Boolean = false
    ) : RecordTab
}

/**
 * Complete application state.
 */
data class AppState(
    val currentView: AppView = AppView.Start,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val currentConnectionIndex: Int = 0,
    val loadingState: LoadingState = LoadingState.Idle,
    val treeRoot: TreeNode? = null,
    val selectedNode: TreeNode? = null,
    val selectedEntry: Entry? = null,
    val queryResults: List<Entry> = emptyList(),
    val queryResultsRoot: TreeNode? = null,
    val isShowingQueryResults: Boolean = false,
    val queryText: String = "SELECT * FROM dc=example WHERE objectClass=*",
    val searchFromDN: String = "",
    var errorMessage: String? = null,
    val successMessage: String? = null,
    val showVirtualMembers: Boolean = false,
    val openTabs: List<RecordTab> = emptyList(),
    val activeTabId: String? = null,
    val schema: LdapSchema? = null,
    val isInspectingSchema: Boolean = false,
    val schemaInspectionProgress: Float = 0f,
    val schemaInspectionStatus: String? = null,
    val isAttributeViewerOpen: Boolean = false,
    val isAttributeViewerLoading: Boolean = false,
    val attributeViewerSchema: LdapSchema? = null,
    val attributeSortAscending: Boolean = true,
    val showBindDnSelection: Boolean = false,
    val connectionForSelection: com.moribito.config.LdapConfig? = null,
    val currentCredential: com.moribito.config.BindCredential? = null,
    val isConfigurationWindowOpen: Boolean = false,
    val isLogViewerOpen: Boolean = false,
    val logEntries: List<LogEntry> = emptyList(),
    val logSearchQuery: String = "",
    val logLevelFilter: Set<LogLevel> = setOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG),
    val logAutoScroll: Boolean = true,
    val logFilePath: String? = null,
    val accessStatus: AccessStatus? = null
)

/**
 * Gets the currently active tab, or null if no tabs are open.
 */
fun AppState.getActiveTab(): RecordTab? =
    openTabs.find { it.id == activeTabId }

/**
 * Gets the temporary tab, or null if there isn't one.
 */
fun AppState.getTemporaryTab(): RecordTab? =
    openTabs.find { it.isTemporary }

/**
 * Checks if a DN is already open in a permanent tab.
 */
fun AppState.hasPermanentTab(dn: String): Boolean =
    openTabs.any { it is RecordTab.EntryTab && !it.isTemporary && it.dn == dn }
