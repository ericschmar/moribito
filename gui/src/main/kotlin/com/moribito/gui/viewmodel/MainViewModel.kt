package com.moribito.gui.viewmodel

import com.moribito.config.*
import com.moribito.config.LdapConfig as ConfigLdapConfig
import com.moribito.gui.license.LicenseResult
import com.moribito.gui.license.LicenseVerifier
import com.moribito.ldap.*
import com.moribito.logging.Logger
import com.moribito.logging.LogEntry
import com.moribito.logging.LogLevel
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
class MainViewModel(private val configService: ConfigurationService) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var ldapClient: LdapClient? = null
    private val logger = Logger.get("MainViewModel")
    private var autoRefreshJob: Job? = null

    private var config: RootConfig = configService.load()

    // Application state
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // Track the currently active connection index
    private var currentConnectionIndex: Int = 0

    init {
        // Verify saved license in the background on startup
        config.settings.licenseKey?.let { savedLicenseKey ->
            scope.launch {
                logger.info("Verifying saved license key on startup")
                try {
                    val result = LicenseVerifier.verify(savedLicenseKey)
                    _state.update { it.copy(
                        verificationResult = result
                    )}
                    when (result) {
                        is LicenseResult.Success -> {
                            logger.info("License verified successfully: ${result.userEmail}")
                        }
                        is LicenseResult.Invalid -> {
                            logger.warn("Saved license key is invalid")
                        }
                        is LicenseResult.Error -> {
                            logger.error("Error verifying license: ${result.msg}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Exception during license verification", e)
                }
            }
        }
    }

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
            _state.update {
                it.copy(currentConnectionIndex = index)
            }
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
        bindCredentials: List<com.moribito.config.BindCredential>? = null
    ) {
        logger.info("updateConfig called for connection at index $currentConnectionIndex")
        logger.info("Updating to: name=$name, host=$host, port=$port")

        val currentConn = getCurrentConnection()
        val finalCredentials = bindCredentials ?: currentConn.effectiveBindCredentials

        val updatedConnection = ConfigLdapConfig(
            name = name,
            host = host,
            port = port,
            baseDN = baseDN,
            useSsl = useSsl,
            useTls = useTls,
            _legacyBindUser = null,  // Clear legacy fields
            _legacyBindPass = null,
            bindCredentials = finalCredentials
        )

        val updatedConnections = config.connections.toMutableList()
        if (currentConnectionIndex in updatedConnections.indices) {
            logger.info("Updating existing connection at index $currentConnectionIndex")
            updatedConnections[currentConnectionIndex] = updatedConnection
        } else {
            logger.info("Adding new connection (index out of bounds)")
            updatedConnections.add(updatedConnection)
            currentConnectionIndex = updatedConnections.size - 1
        }

        config = config.copy(connections = updatedConnections)
        logger.info("Config updated. Total connections: ${config.connections.size}")
    }

    /**
     * Saves the current configuration and updates the connection by name.
     * Returns the final connection name.
     */
    fun saveConnection(
        selectedConnectionName: String,
        name: String,
        host: String,
        port: Int,
        baseDN: String,
        useSsl: Boolean,
        useTls: Boolean,
        bindCredentials: List<com.moribito.config.BindCredential>? = null
    ): String {
        logger.info("saveConnection called for: $selectedConnectionName")

        val finalName = name.ifBlank { host }
        logger.info("Final name will be: $finalName")

        // Find the connection by the currently selected name
        val connectionIndex = getConnectionIndexByName(selectedConnectionName)
        logger.info("Found connection at index: $connectionIndex")

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
                bindCredentials = bindCredentials
            )
        } else {
            logger.info("WARNING: Connection not found: $selectedConnectionName")
        }

        // Save the config to disk
        logger.info("Saving config to disk...")
        configService.save(config)

        return finalName
    }

    /**
     * Saves the license key to the configuration.
     */
    fun saveLicenseKey(licenseKey: String) {
        logger.info("Saving license key to configuration")
        config = config.copy(
            settings = config.settings.copy(licenseKey = licenseKey)
        )
        configService.save(config)
    }

    /**
     * Gets the saved license key from the configuration.
     */
    fun getSavedLicenseKey(): String? {
        return config.settings.licenseKey
    }

    /**
     * Verifies a license key and updates the state with the result.
     * If verification succeeds, saves the license key to config.
     */
    fun verifyLicense(licenseKey: String) {
        scope.launch {
            logger.info("Verifying license key")
            try {
                val result = LicenseVerifier.verify(licenseKey)
                _state.update { it.copy(verificationResult = result) }
                
                when (result) {
                    is LicenseResult.Success -> {
                        logger.info("License verified successfully: ${result.userEmail}")
                        saveLicenseKey(licenseKey)
                    }
                    is LicenseResult.Invalid -> {
                        logger.warn("License key is invalid")
                    }
                    is LicenseResult.Error -> {
                        logger.error("Error verifying license: ${result.msg}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Exception during license verification", e)
                _state.update { it.copy(verificationResult = LicenseResult.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    /**
     * Saves and connects to the LDAP server.
     */
    fun saveAndConnect(
        selectedConnectionName: String,
        name: String,
        host: String,
        port: Int,
        baseDN: String,
        useSsl: Boolean,
        useTls: Boolean,
        bindCredentials: List<com.moribito.config.BindCredential>? = null,
        credential: com.moribito.config.BindCredential? = null
    ): String {
        logger.info("saveAndConnect called")

        // Save the connection first
        val finalName = saveConnection(
            selectedConnectionName,
            name,
            host,
            port,
            baseDN,
            useSsl,
            useTls,
            bindCredentials = bindCredentials
        )

        // Now connect
        connect(credential)

        return finalName
    }

    /**
     * Adds a new bind credential to a connection.
     */
    fun addBindCredential(connectionName: String): com.moribito.config.BindCredential {
        val connectionIndex = getConnectionIndexByName(connectionName)
        if (connectionIndex < 0) throw IllegalArgumentException("Connection not found: $connectionName")

        val conn = config.connections[connectionIndex]
        val newCredential = com.moribito.config.BindCredential(
            label = "New Credential",
            bindUser = "",
            bindPass = "",
            isDefault = conn.effectiveBindCredentials.isEmpty()
        )

        val updatedCredentials = conn.effectiveBindCredentials.toMutableList()
        updatedCredentials.add(newCredential)

        val updatedConn = conn.copy(
            bindCredentials = updatedCredentials,
            _legacyBindUser = null,
            _legacyBindPass = null
        )

        val updatedConnections = config.connections.toMutableList()
        updatedConnections[connectionIndex] = updatedConn
        config = config.copy(connections = updatedConnections)

        return newCredential
    }

    /**
     * Deletes a bind credential from a connection.
     */
    fun deleteBindCredential(connectionName: String, credentialId: String) {
        val connectionIndex = getConnectionIndexByName(connectionName)
        if (connectionIndex < 0) return

        val conn = config.connections[connectionIndex]
        val updatedCredentials = conn.effectiveBindCredentials.filterNot { it.id == credentialId }

        val updatedConn = conn.copy(
            bindCredentials = updatedCredentials,
            _legacyBindUser = null,
            _legacyBindPass = null
        )

        val updatedConnections = config.connections.toMutableList()
        updatedConnections[connectionIndex] = updatedConn
        config = config.copy(connections = updatedConnections)
    }

    /**
     * Updates a bind credential in a connection.
     */
    fun updateBindCredential(connectionName: String, updatedCredential: com.moribito.config.BindCredential) {
        val connectionIndex = getConnectionIndexByName(connectionName)
        if (connectionIndex < 0) return

        val conn = config.connections[connectionIndex]
        val updatedCredentials = conn.effectiveBindCredentials.map {
            if (it.id == updatedCredential.id) {
                // If this is set to default, unset others
                if (updatedCredential.isDefault) it.copy(isDefault = true) else it.copy(isDefault = false)
                updatedCredential
            } else {
                // If new one is default, this one cannot be
                if (updatedCredential.isDefault) it.copy(isDefault = false) else it
            }
        }

        val updatedConn = conn.copy(
            bindCredentials = updatedCredentials,
            _legacyBindUser = null,
            _legacyBindPass = null
        )

        val updatedConnections = config.connections.toMutableList()
        updatedConnections[connectionIndex] = updatedConn
        config = config.copy(connections = updatedConnections)
    }

    /**
     * Connects to the LDAP server with current configuration.
     * Optionally specify which credential to use.
     */
    fun connect(credential: com.moribito.config.BindCredential? = null) {
        logger.info("connect() called")
        
        val currentConn = getCurrentConnection()
        val credentials = currentConn.effectiveBindCredentials
        
        // If no specific credential is provided and there are multiple, prompt the user
        if (credential == null && credentials.size > 1) {
            logger.info("Multiple credentials found, showing selection dialog")
            _state.update { it.copy(
                showBindDnSelection = true,
                connectionForSelection = currentConn
            )}
            return
        }

        scope.launch {
            try {
                logger.info("Starting connection process...")
                _state.update { it.copy(
                    connectionState = ConnectionState.Connecting,
                    loadingState = LoadingState.Loading("Connecting to LDAP server..."),
                    showBindDnSelection = false
                )}
                logger.info("State updated to Connecting")

                // Get the current connection from the list
                logger.info("Current connection: host=${currentConn.host}, port=${currentConn.port}, baseDN=${currentConn.baseDN}")

                // Use provided credential, or default, or first
                val selectedCredential = credential 
                    ?: credentials.firstOrNull { it.isDefault } 
                    ?: credentials.firstOrNull()
                
                if (selectedCredential == null) {
                    throw IllegalStateException("No credentials configured for connection: ${currentConn.name}")
                }
                logger.info("Using credential: ${selectedCredential.label}")

                // Map config connection to LdapClient config
                val ldapConfig = LdapConfig(
                    host = currentConn.host,
                    port = currentConn.port,
                    baseDN = currentConn.baseDN,
                    useSSL = currentConn.useSsl,
                    useTLS = currentConn.useTls,
                    retryEnabled = true,
                    maxRetries = 3,
                    initialDelayMs = 500,
                    maxDelayMs = 5000
                )

                // Create new LDAP client with credential
                logger.info("Creating LDAP client...")
                val client = LdapClient(ldapConfig, selectedCredential)
                logger.info("Calling client.connect()...")
                client.connect()
                logger.info("Client connected successfully!")
                ldapClient = client

                // Build initial tree
                logger.info("Building tree...")
                val root = client.buildTree()
                logger.info("Tree built: ${root.dn}")

                logger.info("Updating state to Connected and switching to Workspace view...")
                _state.update { it.copy(
                    connectionState = ConnectionState.Connected,
                    loadingState = LoadingState.Success("Connected successfully"),
                    treeRoot = root,
                    currentView = AppView.Workspace,
                    currentCredential = selectedCredential
                )}
                logger.info("State updated! Current view should now be: ${_state.value.currentView}")

                // Start background schema inspection
                inspectSchema()

                // Clear success message after a delay
                delay(3000)
                _state.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                val errorMsg = "Connection failed: ${e.message}"
                logger.info("ERROR during connection: $errorMsg")
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
     * Reconnects to the LDAP server.
     */
    fun reconnect() {
        disconnect()
        connect()
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
                logger.debug("Loading children for node: ${node.dn} (${node.name}), isShowingQueryResults=${_state.value.isShowingQueryResults}")
                
                _state.update { it.copy(
                    loadingState = LoadingState.Loading("Loading children...")
                )}

                val showVirtualMembers = _state.value.showVirtualMembers
                val updatedNode = client.loadChildrenWithMembers(node, showVirtualMembers)
                
                logger.debug("Children loaded: ${updatedNode.children?.size ?: 0} children for ${node.dn}")

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
                logger.error("Error loading children for ${node.dn}", e)
                
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
                    .filterIsInstance<RecordTab.EntryTab>()
                    .find { !it.isTemporary && it.dn == dn }
                
                if (existingPermanentTab != null) {
                    // Just activate it
                    _state.update { it.copy(activeTabId = existingPermanentTab.id) }
                    return@launch
                }
                
                // Create new temporary tab (or replace existing temporary)
                val newTab = RecordTab.EntryTab(
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
                        if (tab.id == newTab.id && tab is RecordTab.EntryTab) {
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
                        if (tab is RecordTab.EntryTab && tab.dn == dn) {
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
                    when (tab) {
                        is RecordTab.EntryTab -> tab.copy(isTemporary = false)
                        is RecordTab.GraphTab -> tab.copy(isTemporary = false)
                    }
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
        val tempTab = _state.value.openTabs
            .filterIsInstance<RecordTab.EntryTab>()
            .find { it.isTemporary && it.dn == dn }
        if (tempTab != null) {
            makeTabPermanent(tempTab.id)
            return
        }
        
        // Check if already open as permanent
        val existingTab = _state.value.openTabs
            .filterIsInstance<RecordTab.EntryTab>()
            .find { !it.isTemporary && it.dn == dn }
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
                val newTab = RecordTab.EntryTab(
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
                        if (tab.id == newTab.id && tab is RecordTab.EntryTab) {
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
                        if (tab is RecordTab.EntryTab && tab.dn == dn) {
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
     * Handles clicking on a node in the directory graph.
     */
    fun handleGraphNodeClick(node: TreeNode) {
        if (!node.isLoaded) {
            loadNodeChildren(node)
        }
        // Optionally select the node too, but don't switch tabs automatically
        // unless we want to view details. For now, let's just load children.
    }
    /**
     * Opens the directory graph view in a new tab.
     */
    fun openDirectoryGraph() {
        // Check if already open
        val existingTab = _state.value.openTabs.find { it is RecordTab.GraphTab }
        if (existingTab != null) {
            _state.update { it.copy(activeTabId = existingTab.id) }
            return
        }
        
        val newTab = RecordTab.GraphTab()
        _state.update { it.copy(
            openTabs = it.openTabs + newTab,
            activeTabId = newTab.id
        )}
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
                logger.info("Query completed: filter=$filter, found ${results.size} results")
                logger.debug("Query result DNs: ${results.map { it.dn }}")
                
                // Convert results to tree nodes
                val resultNodes = results.map { it.toTreeNode() }
                val resultsRoot = TreeNode(
                    dn = _state.value.treeRoot?.dn ?: "",
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
                logger.error("Query execution failed", e)
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
            _legacyBindUser = null,
            _legacyBindPass = null,
            bindCredentials = emptyList()
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
     * Inspects the LDAP server schema in the background.
     */
    fun inspectSchema() {
        val client = ldapClient ?: return

        scope.launch {
            try {
                _state.update { it.copy(
                    isInspectingSchema = true,
                    isAttributeViewerLoading = true,
                    schemaInspectionProgress = 0.1f,
                    schemaInspectionStatus = "Starting schema inspection..."
                )}

                delay(500)
                _state.update { it.copy(
                    schemaInspectionProgress = 0.3f,
                    schemaInspectionStatus = "Querying Root DSE..."
                )}

                val schema = client.inspectSchema()

                _state.update { it.copy(
                    schemaInspectionProgress = 0.8f,
                    schemaInspectionStatus = "Parsing schema definitions..."
                )}

                delay(500)

                _state.update { it.copy(
                    schema = schema,
                    attributeViewerSchema = schema,
                    isInspectingSchema = false,
                    isAttributeViewerLoading = false,
                    schemaInspectionProgress = 1.0f,
                    schemaInspectionStatus = if (schema.isFromSchemaInspection) "Schema inspection complete" else "Schema inspection not supported by server"
                )}

                // Clear status after a delay
                delay(3000)
                _state.update { it.copy(schemaInspectionStatus = null) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    isInspectingSchema = false,
                    isAttributeViewerLoading = false,
                    schemaInspectionStatus = "Schema inspection failed: ${e.message}"
                )}
                delay(5000)
                _state.update { it.copy(schemaInspectionStatus = null) }
            }
        }
    }

    /**
     * Loads attributes for a specific OU.
     */
    fun loadOuAttributes(ouDn: String) {
        val client = ldapClient ?: return

        scope.launch {
            try {
                _state.update { it.copy(
                    isAttributeViewerOpen = true,
                    isAttributeViewerLoading = true,
                    attributeViewerSchema = null, // Clear previous schema to show loading
                )}

                val schema = client.getAttributesInOu(ouDn)

                _state.update { it.copy(
                    attributeViewerSchema = schema,
                    isAttributeViewerLoading = false,
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isAttributeViewerLoading = false,
                    loadingState = LoadingState.Failed("Failed to fetch OU attributes: ${e.message}"),
                    errorMessage = "Failed to fetch OU attributes: ${e.message}"
                )}
            }
        }
    }

    /**
     * Toggles the attribute viewer panel.
     */
    fun toggleAttributeViewer(open: Boolean? = null) {
        _state.update { it.copy(
            isAttributeViewerOpen = open ?: !it.isAttributeViewerOpen
        )}
    }

    /**
     * Toggles the attribute sort order.
     */
    fun toggleAttributeSort() {
        _state.update { it.copy(
            attributeSortAscending = !it.attributeSortAscending
        )}
    }

    /**
     * Cancels the bind DN selection dialog.
     */
    fun cancelBindDnSelection() {
        _state.update { it.copy(showBindDnSelection = false, connectionForSelection = null) }
    }

    /**
     * Switches to the configuration view.
     */
    fun navigateToConfiguration() {
        _state.update { it.copy(currentView = AppView.Configuration) }
    }

    /**
     * Opens the configuration window.
     */
    fun openConfigurationWindow() {
        _state.update { it.copy(isConfigurationWindowOpen = true) }
    }

    /**
     * Closes the configuration window.
     */
    fun closeConfigurationWindow() {
        _state.update { it.copy(isConfigurationWindowOpen = false) }
    }

    // ========== Log Viewer Methods ==========

    /**
     * Opens the log viewer window and loads the current log file.
     */
    fun openLogViewer() {
        _state.update { it.copy(isLogViewerOpen = true) }
        loadCurrentLogFile()
        startAutoRefreshLogs()
    }

    /**
     * Closes the log viewer window.
     */
    fun closeLogViewer() {
        stopAutoRefreshLogs()
        _state.update { it.copy(isLogViewerOpen = false) }
    }

    /**
     * Starts auto-refresh for log entries every 2 seconds.
     */
    private fun startAutoRefreshLogs() {
        stopAutoRefreshLogs()  // Cancel any existing refresh

        autoRefreshJob = scope.launch {
            while (isActive) {
                try {
                    loadCurrentLogFile()
                    delay(2000)  // 2 second refresh interval
                } catch (e: CancellationException) {
                    throw e  // Propagate cancellation
                } catch (e: Exception) {
                    logger.error("Auto-refresh failed", e)
                    delay(2000)  // Still wait before retry
                }
            }
        }
    }

    /**
     * Stops auto-refresh for log entries.
     */
    private fun stopAutoRefreshLogs() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    /**
     * Loads the current session's log file filtered for LdapClient only.
     */
    private fun loadCurrentLogFile() {
        scope.launch {
            try {
                val logFile = Logger.getCurrentLogFile()
                val entries = logFile.readLines()
                    .mapNotNull { LogEntry.parse(it) }
                    .filter { it.component == "LdapClient" }  // Filter for LdapClient logs only

                _state.update { it.copy(
                    logEntries = entries,
                    logFilePath = logFile.absolutePath
                )}

                logger.info("Loaded ${entries.size} LdapClient log entries from ${logFile.absolutePath}")
            } catch (e: Exception) {
                logger.error("Failed to load log file", e)
                _state.update { it.copy(
                    errorMessage = "Failed to load logs: ${e.message}"
                )}
            }
        }
    }

    /**
     * Updates the log search query.
     */
    fun updateLogSearchQuery(query: String) {
        _state.update { it.copy(logSearchQuery = query) }
    }

    /**
     * Toggles a log level filter.
     */
    fun toggleLogLevelFilter(level: LogLevel) {
        _state.update { state ->
            val newFilter = if (level in state.logLevelFilter) {
                state.logLevelFilter - level
            } else {
                state.logLevelFilter + level
            }
            state.copy(logLevelFilter = newFilter)
        }
    }

    /**
     * Toggles auto-scroll for the log viewer.
     */
    fun toggleLogAutoScroll() {
        _state.update { it.copy(logAutoScroll = !it.logAutoScroll) }
    }

    /**
     * Cleans up resources when ViewModel is no longer needed.
     */
    fun cleanup() {
        ldapClient?.close()
        scope.cancel()
    }
}
