package com.moribito.gui.viewmodel

import com.moribito.ldap.Entry
import com.moribito.ldap.TreeNode

/**
 * Represents the different views in the application.
 */
sealed class AppView {
    object Configuration : AppView()
    object Tree : AppView()
    object Record : AppView()
    object Query : AppView()
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
 * Complete application state.
 */
data class AppState(
    val currentView: AppView = AppView.Configuration,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val loadingState: LoadingState = LoadingState.Idle,
    val treeRoot: TreeNode? = null,
    val selectedNode: TreeNode? = null,
    val selectedEntry: Entry? = null,
    val queryResults: List<Entry> = emptyList(),
    val queryText: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)
