package com.moribito.gui.viewmodel

import com.moribito.config.BindCredential
import com.moribito.ldap.*
import com.moribito.ldap.LdapConfig as ClientLdapConfig
import com.moribito.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing LDAP connection and tree navigation.
 */
class LdapViewModel(
    private val stateHolder: AppStateHolder,
    private val configViewModel: ConfigViewModel
) {
    private val logger = Logger.get("LdapViewModel")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var client: ILdapClient? = null

    /**
     * Returns the current LDAP client.
     */
    fun getClient(): ILdapClient? = client

    /**
     * Connects to the LDAP server with current configuration.
     */
    fun connect(credential: BindCredential? = null) {
        logger.info("connect() called")
        
        val currentConn = configViewModel.getCurrentConnection()
        val credentials = currentConn.effectiveBindCredentials
        
        if (credential == null && credentials.size > 1) {
            logger.info("Multiple credentials found, showing selection dialog")
            stateHolder.update { it.copy(
                showBindDnSelection = true,
                connectionForSelection = currentConn
            )}
            return
        }

        scope.launch {
            try {
                logger.info("Starting connection process...")
                stateHolder.update { it.copy(
                    connectionState = ConnectionState.Connecting,
                    loadingState = LoadingState.Loading("Connecting to LDAP server..."),
                    showBindDnSelection = false
                )}

                val selectedCredential = credential 
                    ?: credentials.firstOrNull { it.isDefault } 
                    ?: credentials.firstOrNull()
                
                if (selectedCredential == null) {
                    throw IllegalStateException("No credentials configured for connection: ${currentConn.name}")
                }

                val ldapConfig = ClientLdapConfig(
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

                // Use mock client if host is "mock"
                val ldapClient = if (currentConn.host.equals("mock", ignoreCase = true)) {
                    MockLdapClient(ldapConfig, selectedCredential)
                } else {
                    LdapClient(ldapConfig, selectedCredential)
                }

                ldapClient.connect()
                client = ldapClient

                val root = ldapClient.buildTree()

                stateHolder.update { it.copy(
                    connectionState = ConnectionState.Connected,
                    loadingState = LoadingState.Success(if (currentConn.host.equals("mock", ignoreCase = true)) "Connected to Mock LDAP" else "Connected successfully"),
                    treeRoot = root,
                    currentView = AppView.Workspace,
                    currentCredential = selectedCredential
                )}

                // Inspect schema
                val schema = ldapClient.inspectSchema()
                stateHolder.update { it.copy(
                    schema = schema,
                    attributeViewerSchema = schema
                ) }

                delay(3000)
                stateHolder.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                val errorMsg = "Connection failed: ${e.message}"
                logger.error("ERROR during connection: $errorMsg", e)
                stateHolder.update { it.copy(
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
        client?.close()
        client = null
        
        closeAllTabs()
        stateHolder.update { it.copy(
            connectionState = ConnectionState.Disconnected,
            currentView = AppView.Configuration,
            treeRoot = null,
            selectedNode = null,
            selectedEntry = null,
            schema = null,
            isShowingQueryResults = false,
            queryResults = emptyList(),
            queryResultsRoot = null
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
        val finalName = configViewModel.saveConnection(
            selectedConnectionName,
            name,
            host,
            port,
            baseDN,
            useSsl,
            useTls,
            bindCredentials = bindCredentials
        )
        connect(credential)
        return finalName
    }

    /**
     * Navigates to a specific view.
     */
    fun navigateTo(view: AppView) {
        stateHolder.update { it.copy(currentView = view) }
    }

    /**
     * Loads children for a tree node.
     */
    fun loadNodeChildren(node: TreeNode) {
        logger.info("loadNodeChildren called for node: ${node.dn}, isLoaded: ${node.isLoaded}")
        println("LdapViewModel: loadNodeChildren - node.id=${node.id}, node.dn=${node.dn}")

        val ldapClient = client ?: run {
            logger.warn("loadNodeChildren: client is null, returning")
            return
        }

        scope.launch {
            try {
                stateHolder.update { it.copy(
                    loadingState = LoadingState.Loading("Loading children...")
                )}

                val showVirtualMembers = stateHolder.value.showVirtualMembers
                val updatedNode = ldapClient.loadChildrenWithMembers(node, showVirtualMembers)

                println("LdapViewModel: Loaded children for ${node.dn}, got ${updatedNode.children?.size ?: 0} children, isLoaded=${updatedNode.isLoaded}")
                println("LdapViewModel: updatedNode.id=${updatedNode.id}, original node.id=${node.id} - IDs match: ${updatedNode.id == node.id}")

                stateHolder.update { state ->
                    val newTreeRoot = if (state.isShowingQueryResults) {
                        updateNodeInTree(state.queryResultsRoot, updatedNode)
                    } else {
                        updateNodeInTree(state.treeRoot, updatedNode)
                    }

                    println("LdapViewModel: Updated tree root, new root has children: ${newTreeRoot?.children != null}")

                    if (state.isShowingQueryResults) {
                        state.copy(
                            queryResultsRoot = newTreeRoot,
                            loadingState = LoadingState.Idle
                        )
                    } else {
                        state.copy(
                            treeRoot = newTreeRoot,
                            loadingState = LoadingState.Idle
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Error loading children for ${node.dn}", e)
                stateHolder.update { it.copy(
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

        val ldapClient = client ?: return

        scope.launch {
            try {
                stateHolder.update { it.copy(
                    selectedNode = node,
                    loadingState = LoadingState.Loading("Loading entry details...")
                )}

                val entry = ldapClient.getEntry(node.dn)

                stateHolder.update { it.copy(
                    selectedEntry = entry,
                    loadingState = LoadingState.Idle
                )}
            } catch (e: Exception) {
                stateHolder.update { it.copy(
                    loadingState = LoadingState.Failed("Failed to load entry: ${e.message}"),
                    errorMessage = "Failed to load entry: ${e.message}"
                )}
            }
        }
    }

    /**
     * Opens a tab for the given tree node.
     */
    fun openTab(node: TreeNode) {
        val dn = node.dn
        val displayName = extractDisplayName(dn)
        
        scope.launch {
            try {
                val existingPermanentTab = stateHolder.value.openTabs
                    .filterIsInstance<RecordTab.EntryTab>()
                    .find { !it.isTemporary && it.dn == dn }
                
                if (existingPermanentTab != null) {
                    stateHolder.update { it.copy(activeTabId = existingPermanentTab.id) }
                    return@launch
                }
                
                val newTab = RecordTab.EntryTab(
                    dn = dn,
                    displayName = displayName,
                    entry = null,
                    isTemporary = true,
                    loadingState = LoadingState.Loading("Loading entry...")
                )
                
                stateHolder.update { state ->
                    val tabs = state.openTabs.filter { !it.isTemporary }
                    state.copy(
                        openTabs = tabs + newTab,
                        activeTabId = newTab.id
                    )
                }

                val ldapClient = client ?: return@launch

                val entry = ldapClient.getEntry(dn)

                stateHolder.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.id == newTab.id && tab is RecordTab.EntryTab) {
                            tab.copy(entry = entry, loadingState = LoadingState.Idle)
                        } else tab
                    }
                    state.copy(openTabs = updatedTabs)
                }
            } catch (e: Exception) {
                logger.error("Failed to open tab for $dn", e)
            }
        }
    }

    /**
     * Makes a temporary tab permanent.
     */
    fun makeTabPermanent(tabId: String) {
        stateHolder.update { state ->
            val updatedTabs = state.openTabs.map { tab ->
                if (tab.id == tabId && tab.isTemporary && tab is RecordTab.EntryTab) {
                    tab.copy(isTemporary = false)
                } else tab
            }
            state.copy(openTabs = updatedTabs)
        }
    }

    /**
     * Opens a permanent tab for a node.
     */
    fun openPermanentTab(node: TreeNode) {
        val dn = node.dn
        val displayName = extractDisplayName(dn)

        scope.launch {
            try {
                val existingPermanentTab = stateHolder.value.openTabs
                    .filterIsInstance<RecordTab.EntryTab>()
                    .find { !it.isTemporary && it.dn == dn }

                if (existingPermanentTab != null) {
                    stateHolder.update { it.copy(activeTabId = existingPermanentTab.id) }
                    return@launch
                }

                val newTab = RecordTab.EntryTab(
                    dn = dn,
                    displayName = displayName,
                    entry = null,
                    isTemporary = false,
                    loadingState = LoadingState.Loading("Loading entry...")
                )

                stateHolder.update { state ->
                    state.copy(
                        openTabs = state.openTabs + newTab,
                        activeTabId = newTab.id
                    )
                }

                val ldapClient = client ?: return@launch

                val entry = ldapClient.getEntry(dn)

                stateHolder.update { state ->
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.id == newTab.id && tab is RecordTab.EntryTab) {
                            tab.copy(entry = entry, loadingState = LoadingState.Idle)
                        } else tab
                    }
                    state.copy(openTabs = updatedTabs)
                }
            } catch (e: Exception) {
                logger.error("Failed to open permanent tab for $dn", e)
            }
        }
    }

    /**
     * Selects an active tab.
     */
    fun selectTab(tabId: String) {
        stateHolder.update { it.copy(activeTabId = tabId) }
    }

    /**
     * Closes a tab.
     */
    fun closeTab(tabId: String) {
        stateHolder.update { state ->
            val updatedTabs = state.openTabs.filter { it.id != tabId }
            val newActiveTabId = if (state.activeTabId == tabId) {
                updatedTabs.lastOrNull()?.id
            } else {
                state.activeTabId
            }
            state.copy(
                openTabs = updatedTabs,
                activeTabId = newActiveTabId
            )
        }
    }

    /**
     * Closes all tabs.
     */
    fun closeAllTabs() {
        stateHolder.update { it.copy(
            openTabs = emptyList(),
            activeTabId = null
        )}
    }

    fun extractDisplayName(dn: String): String {
        return dn.substringBefore(",").substringAfter("=")
    }

    fun handleGraphNodeClick(node: TreeNode) {
        selectNode(node)
    }

    fun openDirectoryGraph() {
        stateHolder.update { state ->
            val existingTab = state.openTabs.find { it is RecordTab.GraphTab }
            if (existingTab != null) {
                state.copy(activeTabId = existingTab.id)
            } else {
                val newTab = RecordTab.GraphTab()
                state.copy(
                    openTabs = state.openTabs + newTab,
                    activeTabId = newTab.id
                )
            }
        }
    }

    fun showDirectoryTree() {
        stateHolder.update { it.copy(isShowingQueryResults = false) }
    }

    fun inspectSchema() {
        val ldapClient = client ?: return

        scope.launch {
            try {
                stateHolder.update { it.copy(
                    isInspectingSchema = true,
                    isAttributeViewerLoading = true,
                    schemaInspectionProgress = 0.1f,
                    schemaInspectionStatus = "Starting schema inspection..."
                )}

                delay(500)
                stateHolder.update { it.copy(
                    schemaInspectionProgress = 0.3f,
                    schemaInspectionStatus = "Querying Root DSE..."
                )}

                val schema = ldapClient.inspectSchema()

                stateHolder.update { it.copy(
                    schemaInspectionProgress = 0.8f,
                    schemaInspectionStatus = "Parsing schema definitions..."
                )}

                delay(500)

                stateHolder.update { it.copy(
                    schema = schema,
                    attributeViewerSchema = schema,
                    isInspectingSchema = false,
                    isAttributeViewerLoading = false,
                    schemaInspectionProgress = 1.0f,
                    schemaInspectionStatus = if (schema.isFromSchemaInspection) "Schema inspection complete" else "Schema inspection not supported by server"
                )}

                delay(3000)
                stateHolder.update { it.copy(schemaInspectionStatus = null) }

            } catch (e: Exception) {
                stateHolder.update { it.copy(
                    isInspectingSchema = false,
                    isAttributeViewerLoading = false,
                    schemaInspectionStatus = "Schema inspection failed: ${e.message}"
                )}
                delay(5000)
                stateHolder.update { it.copy(schemaInspectionStatus = null) }
            }
        }
    }

    fun loadOuAttributes(ouDn: String) {
        val ldapClient = client ?: return

        scope.launch {
            try {
                stateHolder.update { it.copy(isAttributeViewerLoading = true) }
                val schema = ldapClient.getAttributesInOu(ouDn)
                
                stateHolder.update { it.copy(
                    attributeViewerSchema = schema,
                    isAttributeViewerLoading = false
                )}
            } catch (e: Exception) {
                logger.error("Failed to load attributes for OU: $ouDn", e)
                stateHolder.update { it.copy(isAttributeViewerLoading = false) }
            }
        }
    }

    fun toggleAttributeViewer(open: Boolean? = null) {
        stateHolder.update { it.copy(isAttributeViewerOpen = open ?: !it.isAttributeViewerOpen) }
    }

    fun toggleAttributeSort() {
        stateHolder.update { it.copy(attributeSortAscending = !it.attributeSortAscending) }
    }

    fun cancelBindDnSelection() {
        stateHolder.update { it.copy(showBindDnSelection = false, connectionForSelection = null) }
    }

    fun navigateToConfiguration() {
        stateHolder.update { it.copy(currentView = AppView.Configuration) }
    }

    fun openConfigurationWindow() {
        stateHolder.update { it.copy(isConfigurationWindowOpen = true) }
    }

    fun closeConfigurationWindow() {
        stateHolder.update { it.copy(isConfigurationWindowOpen = false) }
    }

    fun toggleShowVirtualMembers() {
        val ldapClient = client ?: return

        scope.launch {
            try {
                val newShowValue = !stateHolder.value.showVirtualMembers

                stateHolder.update { state ->
                    state.copy(
                        showVirtualMembers = newShowValue,
                        treeRoot = state.treeRoot?.let { clearLoadedFlags(it) },
                        loadingState = LoadingState.Loading("Updating tree view...")
                    )
                }

                val root = stateHolder.value.treeRoot
                if (root != null) {
                    val updatedRoot = ldapClient.loadChildrenWithMembers(root, newShowValue)
                    
                    stateHolder.update { it.copy(
                        treeRoot = updatedRoot,
                        loadingState = LoadingState.Success()
                    )}
                }

                delay(2000)
                stateHolder.update { it.copy(loadingState = LoadingState.Idle) }
            } catch (e: Exception) {
                logger.error("Failed to toggle virtual members", e)
                stateHolder.update { it.copy(
                    loadingState = LoadingState.Failed("Failed to update tree view: ${e.message}")
                )}
            }
        }
    }

    private fun clearLoadedFlags(node: TreeNode): TreeNode {
        return node.copy(
            isLoaded = false,
            children = node.children?.map { clearLoadedFlags(it) }
        )
    }

    private fun updateNodeInTree(root: TreeNode?, updatedNode: TreeNode): TreeNode? {
        if (root == null) return null

        println("updateNodeInTree: Checking root.id=${root.id} vs updatedNode.id=${updatedNode.id}, root.dn=${root.dn}")

        if (root.id == updatedNode.id) {
            println("updateNodeInTree: Found matching node! id=${updatedNode.id}, dn=${updatedNode.dn}, children=${updatedNode.children?.size}")
            return updatedNode
        }

        val updatedChildren = root.children?.map { child ->
            updateNodeInTree(child, updatedNode) ?: child
        }

        return root.copy(children = updatedChildren)
    }

    fun cleanup() {
        scope.cancel()
        client?.close()
    }
}
