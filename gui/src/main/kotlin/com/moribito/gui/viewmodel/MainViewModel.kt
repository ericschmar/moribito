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
        println("[MainViewModel] connect() called")
        scope.launch {
            try {
                println("[MainViewModel] Starting connection process...")
                _state.update { it.copy(
                    connectionState = ConnectionState.Connecting,
                    loadingState = LoadingState.Loading("Connecting to LDAP server...")
                )}
                println("[MainViewModel] State updated to Connecting")

                // Get the current connection from the list
                val currentConn = getCurrentConnection()
                println("[MainViewModel] Current connection: host=${currentConn.host}, port=${currentConn.port}, baseDN=${currentConn.baseDN}")

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
                println("[MainViewModel] Creating LDAP client...")
                val client = LdapClient(ldapConfig)
                println("[MainViewModel] Calling client.connect()...")
                client.connect()
                println("[MainViewModel] Client connected successfully!")
                ldapClient = client

                // Build initial tree
                println("[MainViewModel] Building tree...")
                val root = client.buildTree()
                println("[MainViewModel] Tree built: ${root.dn}")

                println("[MainViewModel] Updating state to Connected and switching to Workspace view...")
                _state.update { it.copy(
                    connectionState = ConnectionState.Connected,
                    loadingState = LoadingState.Success("Connected successfully"),
                    treeRoot = root,
                    currentView = AppView.Workspace
                )}
                println("[MainViewModel] State updated! Current view should now be: ${_state.value.currentView}")

                // Clear success message after a delay
                delay(3000)
                _state.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                val errorMsg = "Connection failed: ${e.message}"
                println("[MainViewModel] ERROR during connection: $errorMsg")
                e.printStackTrace()
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
        closeAllTabs()
        _state.update { it.copy(
            connectionState = ConnectionState.Disconnected,
            currentView = AppView.Configuration,
            treeRoot = null,
            selectedNode = null,
            selectedEntry = null,
            queryResults = emptyList(),
            queryResultsRoot = null,
            isShowingQueryResults = false
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
                println("=== LOADING CHILDREN ===")
                println("Node DN: ${node.dn}")
                println("Node name: ${node.name}")
                println("isShowingQueryResults: ${_state.value.isShowingQueryResults}")
                
                _state.update { it.copy(
                    loadingState = LoadingState.Loading("Loading children...")
                )}

                val showVirtualMembers = _state.value.showVirtualMembers
                val updatedNode = client.loadChildrenWithMembers(node, showVirtualMembers)
                
                println("Children loaded: ${updatedNode.children?.size ?: 0}")
                updatedNode.children?.forEachIndexed { index, child ->
                    println("  [$index] ${child.name} (${child.dn})")
                }
                println("=======================")

                // Update the appropriate tree based on which mode we're in
                _state.update { state ->
                    if (state.isShowingQueryResults) {
                        state.copy(
                            queryResultsRoot = updateNodeInTree(state.queryResultsRoot, updatedNode),
                            loadingState = LoadingState.Idle
                        )
                    } else {
                        state.copy(
                            treeRoot = updateNodeInTree(state.treeRoot, updatedNode),
                            loadingState = LoadingState.Idle
                        )
                    }
                }
            } catch (e: Exception) {
                println("=== ERROR LOADING CHILDREN ===")
                println("Error: ${e.message}")
                e.printStackTrace()
                println("==============================")
                
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
        openTab(node)
        
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
     * Opens a tab for the given tree node.
     * Single-click behavior: Replace temporary tab or create new temporary tab.
     */
    fun openTab(node: TreeNode) {
        val dn = node.dn
        val displayName = extractDisplayName(dn)
        
        scope.launch {
            try {
                // Check if already open in permanent tab
                val existingPermanentTab = _state.value.openTabs
                    .find { !it.isTemporary && it.dn == dn }
                
                if (existingPermanentTab != null) {
                    // Just activate it
                    _state.update { it.copy(activeTabId = existingPermanentTab.id) }
                    return@launch
                }
                
                // Create new temporary tab (or replace existing temporary)
                val newTab = RecordTab(
                    dn = dn,
                    displayName = displayName,
                    entry = null,
                    isTemporary = true,
                    loadingState = LoadingState.Loading("Loading entry...")
                )
                
                _state.update { state ->
                    val tabs = state.openTabs.filter { !it.isTemporary } // Remove old temporary
                    state.copy(
                        openTabs = tabs + newTab,
                        activeTabId = newTab.id
                    )
                }
                
                // Load entry asynchronously
                val entry = ldapClient?.getEntry(dn)
                
                _state.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.id == newTab.id) {
                            tab.copy(entry = entry, loadingState = LoadingState.Idle)
                        } else {
                            tab
                        }
                    }
                    state.copy(openTabs = updatedTabs)
                }
                
            } catch (e: Exception) {
                _state.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.dn == dn) {
                            tab.copy(loadingState = LoadingState.Failed("Failed to load: ${e.message}"))
                        } else {
                            tab
                        }
                    }
                    state.copy(openTabs = updatedTabs, errorMessage = "Failed to load entry: ${e.message}")
                }
            }
        }
    }

    /**
     * Converts a temporary tab to permanent.
     * Double-click behavior on tab.
     */
    fun makeTabPermanent(tabId: String) {
        _state.update { state ->
            val updatedTabs = state.openTabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(isTemporary = false)
                } else {
                    tab
                }
            }
            state.copy(openTabs = updatedTabs)
        }
    }

    /**
     * Opens a permanent tab directly (double-click on tree node).
     */
    fun openPermanentTab(node: TreeNode) {
        val dn = node.dn
        
        // Check if temporary tab exists for this DN
        val tempTab = _state.value.openTabs.find { it.isTemporary && it.dn == dn }
        if (tempTab != null) {
            makeTabPermanent(tempTab.id)
            return
        }
        
        // Check if already open as permanent
        val existingTab = _state.value.openTabs.find { !it.isTemporary && it.dn == dn }
        if (existingTab != null) {
            _state.update { it.copy(activeTabId = existingTab.id) }
            return
        }
        
        // Check tab limit
        if (_state.value.openTabs.size >= 10) {
            _state.update { it.copy(errorMessage = "Maximum 10 tabs allowed. Please close some tabs.") }
            return
        }
        
        // Create new permanent tab
        val displayName = extractDisplayName(dn)
        
        scope.launch {
            try {
                val newTab = RecordTab(
                    dn = dn,
                    displayName = displayName,
                    entry = null,
                    isTemporary = false,
                    loadingState = LoadingState.Loading("Loading entry...")
                )
                
                _state.update { state ->
                    state.copy(
                        openTabs = state.openTabs + newTab,
                        activeTabId = newTab.id
                    )
                }
                
                // Load entry asynchronously
                val entry = ldapClient?.getEntry(dn)
                
                _state.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.id == newTab.id) {
                            tab.copy(entry = entry, loadingState = LoadingState.Idle)
                        } else {
                            tab
                        }
                    }
                    state.copy(openTabs = updatedTabs)
                }
                
            } catch (e: Exception) {
                _state.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.dn == dn) {
                            tab.copy(loadingState = LoadingState.Failed("Failed to load: ${e.message}"))
                        } else {
                            tab
                        }
                    }
                    state.copy(openTabs = updatedTabs, errorMessage = "Failed to load entry: ${e.message}")
                }
            }
        }
    }

    /**
     * Activates a tab by ID.
     */
    fun selectTab(tabId: String) {
        _state.update { it.copy(activeTabId = tabId) }
    }

    /**
     * Closes a tab by ID.
     */
    fun closeTab(tabId: String) {
        _state.update { state ->
            val tabs = state.openTabs.filter { it.id != tabId }
            
            // Determine new active tab
            val newActiveId = when {
                state.activeTabId != tabId -> state.activeTabId // Different tab was active
                tabs.isEmpty() -> null // No tabs left
                else -> tabs.last().id // Activate rightmost remaining tab
            }
            
            state.copy(
                openTabs = tabs,
                activeTabId = newActiveId
            )
        }
    }

    /**
     * Closes all tabs (called on disconnect).
     */
    fun closeAllTabs() {
        _state.update { it.copy(
            openTabs = emptyList(),
            activeTabId = null
        )}
    }

    /**
     * Extracts a display name from a DN (first RDN component).
     */
    private fun extractDisplayName(dn: String): String {
        val firstRdn = dn.split(',').firstOrNull() ?: dn
        return firstRdn.trim()
    }

    /**
     * Updates the query text in the state.
     */
    fun updateQueryText(text: String) {
        _state.update { it.copy(queryText = text) }
    }

    /**
     * Formats the current query text.
     */
    fun formatQuery() {
        val currentQuery = _state.value.queryText
        if (currentQuery.isBlank()) return

        scope.launch {
            try {
                val formatted = formatLdapFilter(currentQuery)
                _state.update { it.copy(queryText = formatted) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    errorMessage = "Format error: ${e.message}"
                )}
            }
        }
    }

    /**
     * Formats and validates LDAP filter syntax.
     */
    private fun formatLdapFilter(filter: String): String {
        var formatted = filter.trim().replace(Regex("\\s+"), " ")

        // Basic prettification
        formatted = formatted
            .replace("(&", "(&\n  ")
            .replace("(|", "(|\n  ")
            .replace("(!", "(!\n  ")
            .replace(")(", ")\n  (")

        // Validation
        val openCount = formatted.count { it == '(' }
        val closeCount = formatted.count { it == ')' }

        if (openCount != closeCount) {
            throw IllegalArgumentException(
                "Unbalanced parentheses: $openCount open, $closeCount close"
            )
        }

        if (!formatted.startsWith("(")) {
            throw IllegalArgumentException("LDAP filter must start with '('")
        }

        return formatted
    }

    /**
     * Executes a custom LDAP query using the current query text.
     */
    fun executeQuery() {
        val client = ldapClient ?: return
        val filter = _state.value.queryText

        if (filter.isBlank()) {
            _state.update { it.copy(errorMessage = "Query filter cannot be empty") }
            return
        }

        scope.launch {
            try {
                _state.update { it.copy(
                    loadingState = LoadingState.Loading("Executing query...")
                )}

                val results = if (filter.trim().uppercase().startsWith("SELECT")) {
                    client.executeSqlQuery(filter)
                } else {
                    client.customSearch(filter)
                }
                
                // Log query results
                println("=== QUERY RESULTS ===")
                println("Filter: $filter")
                println("Found ${results.size} results:")
                results.forEachIndexed { index, entry ->
                    println("  [$index] DN: ${entry.dn}")
                    println("      Attributes: ${entry.attributes.keys.joinToString(", ")}")
                    entry.attributes.forEach { (key, values) ->
                        println("        $key: ${values.joinToString(", ")}")
                    }
                }
                println("===================")
                
                // Convert results to tree nodes
                val resultNodes = results.map { it.toTreeNode() }
                val resultsRoot = TreeNode(
                    dn = "search:results",
                    name = "Query Results (${results.size})",
                    children = resultNodes,
                    isLoaded = true
                )

                _state.update { it.copy(
                    queryResults = results,
                    queryResultsRoot = resultsRoot,
                    isShowingQueryResults = true,
                    loadingState = LoadingState.Success("Found ${results.size} result(s)")
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
     * Switches from query results view back to directory browsing.
     */
    fun showDirectoryTree() {
        _state.update {
            it.copy(isShowingQueryResults = false)
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
     * Toggles the display of virtual member children in the tree view.
     * Clears the isLoaded flag on all nodes and eagerly reloads the root to get virtual members.
     */
    fun toggleShowVirtualMembers() {
        val client = ldapClient ?: return

        scope.launch {
            try {
                val newShowValue = !_state.value.showVirtualMembers

                // Clear loaded flags and update the toggle
                _state.update { state ->
                    state.copy(
                        showVirtualMembers = newShowValue,
                        treeRoot = state.treeRoot?.let { clearLoadedFlags(it) },
                        loadingState = LoadingState.Loading("Updating tree view...")
                    )
                }

                // Eagerly reload root node with new settings
                val rootNode = _state.value.treeRoot
                if (rootNode != null) {
                    val updatedRoot = client.loadChildrenWithMembers(rootNode, newShowValue)
                    _state.update { state ->
                        state.copy(
                            treeRoot = updatedRoot,
                            loadingState = LoadingState.Idle
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    loadingState = LoadingState.Failed("Failed to update tree: ${e.message}")
                )}
            }
        }
    }

    /**
     * Recursively clears the isLoaded flag on all nodes to force reload.
     */
    private fun clearLoadedFlags(node: TreeNode): TreeNode {
        return node.copy(
            isLoaded = false,
            children = node.children?.map { clearLoadedFlags(it) }
        )
    }

    /**
     * Recursively updates a node in the tree.
     */
    private fun updateNodeInTree(root: TreeNode?, updatedNode: TreeNode): TreeNode? {
        if (root == null) return null
        if (root.id == updatedNode.id) return updatedNode

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
