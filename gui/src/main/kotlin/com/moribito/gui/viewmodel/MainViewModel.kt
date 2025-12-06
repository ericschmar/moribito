package com.moribito.gui.viewmodel

import com.moribito.config.Config
import com.moribito.ldap.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Main ViewModel for the Moribito GUI application.
 *
 * Manages application state, LDAP connection, and business logic.
 */
class MainViewModel(private var config: Config) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var ldapClient: LdapClient? = null

    // Application state
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    /**
     * Updates the LDAP configuration.
     */
    fun updateConfig(
        host: String,
        port: Int,
        baseDN: String,
        useSSL: Boolean,
        useTLS: Boolean,
        bindUser: String,
        bindPass: String
    ) {
        config = config.copy(
            ldap = config.ldap.copy(
                host = host,
                port = port,
                baseDN = baseDN,
                useSSL = useSSL,
                useTLS = useTLS,
                bindUser = bindUser,
                bindPass = bindPass
            )
        )
    }

    /**
     * Connects to the LDAP server with current configuration.
     */
    fun connect() {
        scope.launch {
            try {
                _state.update { it.copy(
                    connectionState = ConnectionState.Connecting,
                    loadingState = LoadingState.Loading("Connecting to LDAP server...")
                )}

                // Map config.ldap to LdapClient config
                val ldapConfig = com.moribito.ldap.LdapConfig(
                    host = config.ldap.host,
                    port = config.ldap.port,
                    baseDN = config.ldap.baseDN,
                    useSSL = config.ldap.useSSL,
                    useTLS = config.ldap.useTLS,
                    bindUser = config.ldap.bindUser,
                    bindPass = config.ldap.bindPass,
                    retryEnabled = config.retry.enabled,
                    maxRetries = config.retry.maxAttempts,
                    initialDelayMs = config.retry.initialDelayMs,
                    maxDelayMs = config.retry.maxDelayMs
                )

                // Create new LDAP client
                val client = LdapClient(ldapConfig)
                client.connect()
                ldapClient = client

                // Build initial tree
                val root = client.buildTree()

                _state.update { it.copy(
                    connectionState = ConnectionState.Connected,
                    loadingState = LoadingState.Success("Connected successfully"),
                    treeRoot = root,
                    currentView = AppView.Tree
                )}

                // Clear success message after a delay
                delay(3000)
                _state.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                val errorMsg = "Connection failed: ${e.message}"
                _state.update { it.copy(
                    connectionState = ConnectionState.Error(errorMsg),
                    loadingState = LoadingState.Failed(errorMsg),
                    errorMessage = errorMsg
                )}
            }
        }
    }

    /**
     * Disconnects from the LDAP server.
     */
    fun disconnect() {
        ldapClient?.close()
        ldapClient = null
        _state.update { it.copy(
            connectionState = ConnectionState.Disconnected,
            currentView = AppView.Configuration,
            treeRoot = null,
            selectedNode = null,
            selectedEntry = null,
            queryResults = emptyList()
        )}
    }

    /**
     * Navigates to a specific view.
     */
    fun navigateTo(view: AppView) {
        _state.update { it.copy(currentView = view) }
    }

    /**
     * Loads children for a tree node.
     */
    fun loadNodeChildren(node: TreeNode) {
        val client = ldapClient ?: return

        scope.launch {
            try {
                _state.update { it.copy(
                    loadingState = LoadingState.Loading("Loading children...")
                )}

                val updatedNode = client.loadChildren(node)

                // Update the tree with the new node
                _state.update { state ->
                    state.copy(
                        treeRoot = updateNodeInTree(state.treeRoot, updatedNode),
                        loadingState = LoadingState.Idle
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    loadingState = LoadingState.Failed("Failed to load children: ${e.message}"),
                    errorMessage = "Failed to load children: ${e.message}"
                )}
            }
        }
    }

    /**
     * Selects a node and loads its entry details.
     */
    fun selectNode(node: TreeNode) {
        val client = ldapClient ?: return

        scope.launch {
            try {
                _state.update { it.copy(
                    selectedNode = node,
                    loadingState = LoadingState.Loading("Loading entry details...")
                )}

                val entry = client.getEntry(node.dn)

                _state.update { it.copy(
                    selectedEntry = entry,
                    currentView = AppView.Record,
                    loadingState = LoadingState.Idle
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    loadingState = LoadingState.Failed("Failed to load entry: ${e.message}"),
                    errorMessage = "Failed to load entry: ${e.message}"
                )}
            }
        }
    }

    /**
     * Executes a custom LDAP query.
     */
    fun executeQuery(filter: String) {
        val client = ldapClient ?: return

        if (filter.isBlank()) {
            _state.update { it.copy(errorMessage = "Query filter cannot be empty") }
            return
        }

        scope.launch {
            try {
                _state.update { it.copy(
                    queryText = filter,
                    loadingState = LoadingState.Loading("Executing query...")
                )}

                val results = client.customSearch(filter)

                _state.update { it.copy(
                    queryResults = results,
                    loadingState = LoadingState.Success("Found ${results.size} result(s)"),
                    currentView = AppView.Query
                )}

                // Clear success message after a delay
                delay(3000)
                _state.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    loadingState = LoadingState.Failed("Query failed: ${e.message}"),
                    errorMessage = "Query failed: ${e.message}"
                )}
            }
        }
    }

    /**
     * Selects a query result entry for viewing.
     */
    fun selectQueryResult(entry: Entry) {
        _state.update { it.copy(
            selectedEntry = entry,
            currentView = AppView.Record
        )}
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Clears the current success message.
     */
    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Recursively updates a node in the tree.
     */
    private fun updateNodeInTree(root: TreeNode?, updatedNode: TreeNode): TreeNode? {
        if (root == null) return null
        if (root.dn == updatedNode.dn) return updatedNode

        val updatedChildren = root.children?.map { child ->
            updateNodeInTree(child, updatedNode) ?: child
        }

        return root.copy(children = updatedChildren)
    }

    /**
     * Cleans up resources when ViewModel is no longer needed.
     */
    fun cleanup() {
        ldapClient?.close()
        scope.cancel()
    }
}
