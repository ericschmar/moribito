package com.moribito.gui.viewmodel

import com.moribito.config.*
import com.moribito.config.LdapConfig as ConfigLdapConfig
import com.moribito.ldap.*
import com.moribito.logging.LogLevel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Main ViewModel for the Moribito GUI application.
 * Now acts as a facade delegating to specialized ViewModels.
 */
class MainViewModel(
    private val configViewModel: ConfigViewModel,
    private val ldapViewModel: LdapViewModel,
    private val queryViewModel: QueryViewModel,
    private val logViewModel: LogViewModel,
    private val stateHolder: AppStateHolder
) {
    val state: StateFlow<AppState> = stateHolder.state

    // Config delegations
    fun getConfig(): RootConfig = configViewModel.getConfig()
    fun getCurrentConnection(): ConfigLdapConfig = configViewModel.getCurrentConnection()
    fun getCurrentConnectionIndex(): Int = configViewModel.getCurrentConnectionIndex()
    fun setCurrentConnection(index: Int) = configViewModel.setCurrentConnection(index)
    fun getRecentConnections(limit: Int = 3): List<ConfigLdapConfig> = configViewModel.getRecentConnections(limit)
    fun updateConfig(name: String, host: String, port: Int, baseDN: String, useSsl: Boolean, useTls: Boolean, bindCredentials: List<BindCredential>? = null) = 
        configViewModel.updateConfig(name, host, port, baseDN, useSsl, useTls, bindCredentials)
    fun saveConnection(selectedConnectionName: String, name: String, host: String, port: Int, baseDN: String, useSsl: Boolean, useTls: Boolean, bindCredentials: List<BindCredential>? = null): String = 
        configViewModel.saveConnection(selectedConnectionName, name, host, port, baseDN, useSsl, useTls, bindCredentials)
    fun saveLicenseKey(licenseKey: String) = configViewModel.saveLicenseKey(licenseKey)
    fun getSavedLicenseKey(): String? = configViewModel.getSavedLicenseKey()
    fun verifyLicense(licenseKey: String) = configViewModel.verifyLicense(licenseKey)
    fun activateTrial() = configViewModel.activateTrial()
    fun refreshAccessStatus() = configViewModel.refreshAccessStatus()
    fun addBindCredential(connectionName: String): BindCredential = configViewModel.addBindCredential(connectionName)
    fun deleteBindCredential(connectionName: String, credentialId: String) = configViewModel.deleteBindCredential(connectionName, credentialId)
    fun updateBindCredential(connectionName: String, updatedCredential: BindCredential) = configViewModel.updateBindCredential(connectionName, updatedCredential)
    fun addConnection(): ConfigLdapConfig = configViewModel.addConnection()
    fun deleteConnection(name: String) = configViewModel.deleteConnection(name)
    fun getConnectionIndexByName(name: String): Int = configViewModel.getConnectionIndexByName(name)

    // LDAP delegations
    fun connect(credential: BindCredential? = null) = ldapViewModel.connect(credential)
    fun disconnect() = ldapViewModel.disconnect()
    fun reconnect() = ldapViewModel.reconnect()
    fun saveAndConnect(selectedConnectionName: String, name: String, host: String, port: Int, baseDN: String, useSsl: Boolean, useTls: Boolean, bindCredentials: List<BindCredential>? = null, credential: BindCredential? = null): String = 
        ldapViewModel.saveAndConnect(selectedConnectionName, name, host, port, baseDN, useSsl, useTls, bindCredentials, credential)
    fun navigateTo(view: AppView) = ldapViewModel.navigateTo(view)
    fun loadNodeChildren(node: TreeNode) = ldapViewModel.loadNodeChildren(node)
    fun selectNode(node: TreeNode) = ldapViewModel.selectNode(node)
    fun openTab(node: TreeNode) = ldapViewModel.openTab(node)
    fun makeTabPermanent(tabId: String) = ldapViewModel.makeTabPermanent(tabId)
    fun openPermanentTab(node: TreeNode) = ldapViewModel.openPermanentTab(node)
    fun selectTab(tabId: String) = ldapViewModel.selectTab(tabId)
    fun handleGraphNodeClick(node: TreeNode) = ldapViewModel.handleGraphNodeClick(node)
    fun openDirectoryGraph() = ldapViewModel.openDirectoryGraph()
    fun closeTab(tabId: String) = ldapViewModel.closeTab(tabId)
    fun closeAllTabs() = ldapViewModel.closeAllTabs()
    fun extractDisplayName(dn: String): String = ldapViewModel.extractDisplayName(dn)
    fun showDirectoryTree() = ldapViewModel.showDirectoryTree()
    fun inspectSchema() = ldapViewModel.inspectSchema()
    fun loadOuAttributes(ouDn: String) = ldapViewModel.loadOuAttributes(ouDn)
    fun toggleAttributeViewer(open: Boolean? = null) = ldapViewModel.toggleAttributeViewer(open)
    fun toggleAttributeSort() = ldapViewModel.toggleAttributeSort()
    fun cancelBindDnSelection() = ldapViewModel.cancelBindDnSelection()
    fun navigateToConfiguration() = ldapViewModel.navigateToConfiguration()
    fun openConfigurationWindow() = ldapViewModel.openConfigurationWindow()
    fun closeConfigurationWindow() = ldapViewModel.closeConfigurationWindow()
    fun toggleShowVirtualMembers() = ldapViewModel.toggleShowVirtualMembers()

    // Query delegations
    fun updateQueryText(text: String) = queryViewModel.updateQueryText(text)
    fun formatQuery() = queryViewModel.formatQuery()
    fun formatLdapFilter(filter: String): String = queryViewModel.formatLdapFilter(filter)
    fun executeQuery() = queryViewModel.executeQuery()
    fun selectQueryResult(entry: Entry) = queryViewModel.selectQueryResult(entry)

    // Log delegations
    fun openLogViewer() = logViewModel.openLogViewer()
    fun closeLogViewer() = logViewModel.closeLogViewer()
    fun startAutoRefreshLogs() = logViewModel.startAutoRefreshLogs()
    fun stopAutoRefreshLogs() = logViewModel.stopAutoRefreshLogs()
    fun loadCurrentLogFile() = logViewModel.loadCurrentLogFile()
    fun updateLogSearchQuery(query: String) = logViewModel.updateLogSearchQuery(query)
    fun toggleLogLevelFilter(level: LogLevel) = logViewModel.toggleLogLevelFilter(level)
    fun toggleLogAutoScroll() = logViewModel.toggleLogAutoScroll()

    fun cleanup() {
        configViewModel.cleanup()
        ldapViewModel.cleanup()
        queryViewModel.cleanup()
        logViewModel.cleanup()
    }

    fun clearError() {
        stateHolder.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        stateHolder.update { it.copy(successMessage = null) }
    }
}
