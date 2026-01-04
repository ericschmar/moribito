package com.moribito.gui.viewmodel

import com.moribito.config.*
import com.moribito.config.LdapConfig as ConfigLdapConfig
import com.moribito.gui.license.*
import com.moribito.logging.Logger
import kotlinx.coroutines.*

/**
 * ViewModel for managing LDAP configurations and license.
 */
class ConfigViewModel(
    private val stateHolder: AppStateHolder,
    private val configService: ConfigurationService
) {
    private val logger = Logger.get("ConfigViewModel")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var config: RootConfig = configService.load()
    private var currentConnectionIndex: Int = 0

    init {
        refreshAccessStatus()
    }

    /**
     * Re-checks the access status based on current configuration and trial state.
     */
    fun refreshAccessStatus() {
        val status = AccessController.checkAccess(config.settings)
        logger.info("Access status refreshed: $status")
        stateHolder.update { it.copy(accessStatus = status) }
    }

    /**
     * Activates the 14-day free trial.
     */
    fun activateTrial() {
        logger.info("Activating trial")
        val (updatedSettings, status) = AccessController.activateTrial(config.settings)
        config = config.copy(settings = updatedSettings)
        configService.save(config)
        stateHolder.update { it.copy(accessStatus = status) }
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
            stateHolder.update {
                it.copy(currentConnectionIndex = index)
            }
        }
    }

    /**
     * Returns the most recently used connections.
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
            _legacyBindUser = null,
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
        val connectionIndex = getConnectionIndexByName(selectedConnectionName)

        if (connectionIndex >= 0) {
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
            // New connection when list is empty
            setCurrentConnection(0)
            updateConfig(
                name = finalName,
                host = host,
                port = port,
                baseDN = baseDN,
                useSsl = useSsl,
                useTls = useTls,
                bindCredentials = bindCredentials
            )
        }

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
     */
    fun verifyLicense(licenseKey: String) {
        scope.launch {
            logger.info("Verifying license key")
            try {
                val result = LicenseVerifier.verify(licenseKey)
                
                when (result) {
                    is LicenseResult.Success, is LicenseResult.ExpiringSoon -> {
                        val email = if (result is LicenseResult.Success) result.userEmail else (result as LicenseResult.ExpiringSoon).userEmail
                        logger.info("License verified successfully for $email")
                        
                        // Consume trial when valid license is provided
                        val updatedSettings = TrialManager.consumeTrial(config.settings.copy(licenseKey = licenseKey))
                        config = config.copy(settings = updatedSettings)
                        configService.save(config)
                    }
                    is LicenseResult.Expired -> {
                        logger.warn("License key is expired")
                        saveLicenseKey(licenseKey)
                    }
                    is LicenseResult.Invalid -> {
                        logger.warn("License key is invalid")
                    }
                    is LicenseResult.Error -> {
                        logger.error("Error verifying license: ${result.msg}")
                    }
                }
                refreshAccessStatus()
            } catch (e: Exception) {
                logger.error("Exception during license verification", e)
                refreshAccessStatus()
            }
        }
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
        
        configService.save(config)

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
        configService.save(config)
    }

    /**
     * Updates a bind credential for a connection.
     */
    fun updateBindCredential(connectionName: String, updatedCredential: com.moribito.config.BindCredential) {
        val connectionIndex = getConnectionIndexByName(connectionName)
        if (connectionIndex < 0) return

        val conn = config.connections[connectionIndex]
        val updatedCredentials = conn.effectiveBindCredentials.map {
            if (it.id == updatedCredential.id) updatedCredential else it
        }

        val updatedConn = conn.copy(
            bindCredentials = updatedCredentials,
            _legacyBindUser = null,
            _legacyBindPass = null
        )

        val updatedConnections = config.connections.toMutableList()
        updatedConnections[connectionIndex] = updatedConn
        config = config.copy(connections = updatedConnections)
        configService.save(config)
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
        
        stateHolder.update { it.copy(currentConnectionIndex = currentConnectionIndex) }
        
        configService.save(config)

        return newConnection
    }

    /**
     * Deletes a connection by name.
     */
    fun deleteConnection(name: String) {
        val updatedConnections = config.connections.filterNot { it.name == name }

        if (updatedConnections.isEmpty()) {
            config = config.copy(connections = listOf(ConfigLdapConfig()))
            currentConnectionIndex = 0
        } else {
            config = config.copy(connections = updatedConnections)
            if (currentConnectionIndex >= updatedConnections.size) {
                currentConnectionIndex = updatedConnections.size - 1
            }
        }
        stateHolder.update { it.copy(currentConnectionIndex = currentConnectionIndex) }
        configService.save(config)
    }

    /**
     * Finds a connection index by name.
     */
    fun getConnectionIndexByName(name: String): Int {
        return config.connections.indexOfFirst { it.name == name }
    }

    fun cleanup() {
        scope.cancel()
    }
}
