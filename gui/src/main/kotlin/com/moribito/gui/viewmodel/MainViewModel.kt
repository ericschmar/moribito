package com.moribito.gui.viewmodel

import RootConfig
import LdapConfig as ConfigLdapConfig
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
class MainViewModel(private var config: RootConfig) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var ldapClient: LdapClient? = null

    // Application state
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Track the currently active connection index
    private var currentConnectionIndex: Int = 0

    // Expose current config
    fun getConfig(): RootConfig = config

    // Get the current active connection
    fun getCurrentConnection(): ConfigLdapConfig {
        return config.connections.getOrElse(currentConnectionIndex) {
            ConfigLdapConfig()
        }
    }

    // Get the current connection index
    fun getCurrentConnectionIndex(): Int = currentConnectionIndex

    // Set the active connection by index
    fun setCurrentConnection(index: Int) {
        if (index in config.connections.indices) {
            currentConnectionIndex = index
        }
    }

    /**
     * Returns the most recently used connections.
     * For initial implementation, returns first N connections from the config.
     * TODO: Enhance with actual "recently used" tracking based on timestamps.
     */
    fun getRecentConnections(limit: Int = 3): List<ConfigLdapConfig> {
        return config.connections.take(limit)
    }

    /**
     * Updates the LDAP configuration for the current connection.
     */
    fun updateConfig(
        name: String,
        host: String,
        port: Int,
        baseDN: String,
        useSsl: Boolean,
        useTls: Boolean,
        bindUser: String,
        bindPass: String
    ) {
        println("[MainViewModel] updateConfig called for connection at index $currentConnectionIndex")
        println("[MainViewModel] Updating to: name=$name, host=$host, port=$port")

        val updatedConnection = ConfigLdapConfig(
            name = name,
            host = host,
            port = port,
            baseDN = baseDN,
            useSsl = useSsl,
            useTls = useTls,
            bindUser = bindUser,
            bindPass = bindPass
        )

        val updatedConnections = config.connections.toMutableList()
        if (currentConnectionIndex in updatedConnections.indices) {
            println("[MainViewModel] Updating existing connection at index $currentConnectionIndex")
            updatedConnections[currentConnectionIndex] = updatedConnection
        } else {
            println("[MainViewModel] Adding new connection (index out of bounds)")
            updatedConnections.add(updatedConnection)
            currentConnectionIndex = updatedConnections.size - 1
        }

        config = config.copy(connections = updatedConnections)
        println("[MainViewModel] Config updated. Total connections: ${config.connections.size}")
    }

    /**
     * Saves the current configuration and updates the connection by name.
     * Returns the final connection name.
     */
    fun saveConnection(
        configService: com.moribito.config.ConfigurationService,
        selectedConnectionName: String,
        name: String,
        host: String,
        port: Int,
        baseDN: String,
        useSsl: Boolean,
        useTls: Boolean,
        bindUser: String,
        bindPass: String
    ): String {
        println("[MainViewModel] saveConnection called for: $selectedConnectionName")

        val finalName = name.ifBlank { host }
        println("[MainViewModel] Final name will be: $finalName")

        // Find the connection by the currently selected name
        val connectionIndex = getConnectionIndexByName(selectedConnectionName)
        println("[MainViewModel] Found connection at index: $connectionIndex")

        if (connectionIndex >= 0) {
            // Update existing connection
            setCurrentConnection(connectionIndex)
            updateConfig(
                name = finalName,
                host = host,
                port = port,
                baseDN = baseDN,
                useSsl = useSsl,
                useTls = useTls,
                bindUser = bindUser,
                bindPass = bindPass
            )
        } else {
            println("[MainViewModel] WARNING: Connection not found: $selectedConnectionName")
        }

        // Save the config to disk
        println("[MainViewModel] Saving config to disk...")
        configService.save(config)

        return finalName
    }

    /**
     * Saves and connects to the LDAP server.
     */
    fun saveAndConnect(
        configService: com.moribito.config.ConfigurationService,
        selectedConnectionName: String,
        name: String,
        host: String,
        port: Int,
        baseDN: String,
        useSsl: Boolean,
        useTls: Boolean,
        bindUser: String,
        bindPass: String
    ): String {
        println("[MainViewModel] saveAndConnect called")

        // Save the connection first
        val finalName = saveConnection(
            configService,
            selectedConnectionName,
            name,
            host,
            port,
            baseDN,
            useSsl,
            useTls,
            bindUser,
            bindPass
        )

        // Now connect
        connect()

        return finalName
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

                // Get the current connection from the list
                val currentConn = getCurrentConnection()

                // Map config connection to LdapClient config
                val ldapConfig = LdapConfig(
                    host = currentConn.host,
                    port = currentConn.port,
                    baseDN = currentConn.baseDN,
                    useSSL = currentConn.useSsl,
                    useTLS = currentConn.useTls,
                    bindUser = currentConn.bindUser,
                    bindPass = currentConn.bindPass,
                    retryEnabled = true,
                    maxRetries = 3,
                    initialDelayMs = 500,
                    maxDelayMs = 5000
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
     * Adds a new connection to the configuration.
     */
    fun addConnection(): ConfigLdapConfig {
        val newConnection = ConfigLdapConfig(
            name = "New Connection ${config.connections.size + 1}",
            host = "",
            port = 389,
            baseDN = "",
            useSsl = false,
            useTls = false,
            bindUser = "",
            bindPass = ""
        )

        val updatedConnections = config.connections.toMutableList()
        updatedConnections.add(newConnection)
        config = config.copy(connections = updatedConnections)
        currentConnectionIndex = updatedConnections.size - 1

        return newConnection
    }

    /**
     * Deletes a connection by name.
     */
    fun deleteConnection(name: String) {
        val updatedConnections = config.connections.filterNot { it.name == name }

        if (updatedConnections.isEmpty()) {
            // Keep at least one connection (add a default one)
            config = config.copy(connections = listOf(ConfigLdapConfig()))
            currentConnectionIndex = 0
        } else {
            config = config.copy(connections = updatedConnections)
            // Adjust current index if needed
            if (currentConnectionIndex >= updatedConnections.size) {
                currentConnectionIndex = updatedConnections.size - 1
            }
        }
    }

    /**
     * Finds a connection index by name.
     */
    fun getConnectionIndexByName(name: String): Int {
        return config.connections.indexOfFirst { it.name == name }
    }

    /**
     * Cleans up resources when ViewModel is no longer needed.
     */
    fun cleanup() {
        ldapClient?.close()
        scope.cancel()
    }
}
