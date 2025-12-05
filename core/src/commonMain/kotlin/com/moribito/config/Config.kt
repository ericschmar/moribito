package com.moribito.config

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Main configuration for the Moribito application.
 */
@Serializable
data class Config(
    val ldap: LdapConfig,
    val pagination: PaginationConfig = PaginationConfig(),
    val retry: RetryConfig = RetryConfig()
) {
    companion object {
        /**
         * Loads configuration from a YAML file.
         *
         * @param path Optional path to config file. If not provided, searches standard locations.
         * @return Pair of Config and the actual path used
         * @throws ConfigException if config file cannot be read or parsed
         */
        suspend fun load(path: String? = null): Pair<Config, String> = withContext(Dispatchers.IO) {
            val configPath = path ?: findConfigFile()
            ?: throw ConfigException("No configuration file found. Please create one using Config.createDefault()")

            try {
                val content = File(configPath).readText()
                val yaml = Yaml.default
                val config = yaml.decodeFromString(serializer(), content)

                // Apply defaults
                val configWithDefaults = config.copy(
                    ldap = config.ldap.withDefaults(),
                    pagination = config.pagination.withDefaults(),
                    retry = config.retry.withDefaults()
                )

                configWithDefaults to configPath
            } catch (e: Exception) {
                throw ConfigException("Failed to load config from $configPath: ${e.message}", e)
            }
        }

        /**
         * Finds a configuration file in standard locations.
         *
         * @return Path to config file, or null if not found
         */
        private fun findConfigFile(): String? {
            val candidates = buildList {
                // Current directory
                add("./config.yaml")
                add("./config.yml")
                add("./moribito.yaml")
                add("./moribito.yml")

                // OS-specific paths
                addAll(getOSSpecificConfigPaths())
            }

            return candidates.firstOrNull { path ->
                Files.exists(Paths.get(path))
            }
        }

        /**
         * Gets OS-specific configuration file paths.
         */
        private fun getOSSpecificConfigPaths(): List<String> {
            val homeDir = System.getProperty("user.home")
            val osName = System.getProperty("os.name").lowercase()

            return when {
                osName.contains("win") -> {
                    val appData = System.getenv("APPDATA") ?: "$homeDir\\AppData\\Roaming"
                    listOf(
                        "$appData\\moribito\\config.yaml",
                        "$appData\\moribito\\config.yml",
                        "$homeDir\\.moribito.yaml",
                        "$homeDir\\.moribito.yml"
                    )
                }

                osName.contains("mac") -> {
                    listOf(
                        "$homeDir/.moribito/config.yaml",
                        "$homeDir/.moribito/config.yml",
                        "$homeDir/Library/Application Support/moribito/config.yaml",
                        "$homeDir/Library/Application Support/moribito/config.yml",
                        "$homeDir/.moribito.yaml",
                        "$homeDir/.moribito.yml",
                        "$homeDir/.config/moribito/config.yaml",
                        "$homeDir/.config/moribito/config.yml"
                    )
                }

                else -> {
                    val xdgConfigHome = System.getenv("XDG_CONFIG_HOME") ?: "$homeDir/.config"
                    listOf(
                        "$xdgConfigHome/moribito/config.yaml",
                        "$xdgConfigHome/moribito/config.yml",
                        "$homeDir/.moribito/config.yaml",
                        "$homeDir/.moribito/config.yml",
                        "$homeDir/.moribito.yaml",
                        "$homeDir/.moribito.yml",
                        "/etc/moribito/config.yaml",
                        "/etc/moribito/config.yml"
                    )
                }
            }
        }

        /**
         * Gets the default config path for the current OS.
         */
        fun getDefaultConfigPath(): String {
            val homeDir = System.getProperty("user.home")
            val osName = System.getProperty("os.name").lowercase()

            return when {
                osName.contains("win") -> {
                    val appData = System.getenv("APPDATA") ?: "$homeDir\\AppData\\Roaming"
                    "$appData\\moribito\\config.yaml"
                }

                osName.contains("mac") -> {
                    "$homeDir/.moribito/config.yaml"
                }

                else -> {
                    val xdgConfigHome = System.getenv("XDG_CONFIG_HOME") ?: "$homeDir/.config"
                    "$xdgConfigHome/moribito/config.yaml"
                }
            }
        }

        /**
         * Creates a default configuration file.
         *
         * @param path Optional path. If not provided, uses OS-specific default location.
         * @throws ConfigException if file already exists or cannot be created
         */
        suspend fun createDefault(path: String? = null): String = withContext(Dispatchers.IO) {
            val configPath = path ?: getDefaultConfigPath()
            val configFile = File(configPath)

            if (configFile.exists()) {
                throw ConfigException("Configuration file already exists at $configPath")
            }

            // Create parent directories
            configFile.parentFile?.mkdirs()

            val defaultConfig = Config(
                ldap = LdapConfig(
                    host = "localhost",
                    port = 389,
                    baseDN = "dc=example,dc=com",
                    useSSL = false,
                    useTLS = false,
                    bindUser = "",
                    bindPass = "",
                    savedConnections = emptyList(),
                    selectedConnection = -1
                ),
                pagination = PaginationConfig(),
                retry = RetryConfig()
            )

            val yaml = Yaml.default
            val content = buildString {
                appendLine("# Moribito Configuration")
                appendLine("# Created at: $configPath")
                appendLine("# Edit this file with your LDAP server details")
                appendLine()
                append(yaml.encodeToString(serializer(), defaultConfig))
            }

            configFile.writeText(content)
            configPath
        }
    }

    /**
     * Saves this configuration to a file.
     *
     * @param path Optional path. If not provided, uses OS-specific default location.
     * @throws ConfigException if file cannot be written
     */
    suspend fun save(path: String? = null): String = withContext(Dispatchers.IO) {
        val configPath = path ?: getDefaultConfigPath()
        val configFile = File(configPath)

        // Create parent directories
        configFile.parentFile?.mkdirs()

        try {
            val yaml = Yaml.default
            val content = buildString {
                appendLine("# Moribito Configuration")
                appendLine("# Last updated: ${System.currentTimeMillis()}")
                appendLine()
                append(yaml.encodeToString(serializer(), this@Config))
            }

            configFile.writeText(content)
            configPath
        } catch (e: Exception) {
            throw ConfigException("Failed to save config to $configPath: ${e.message}", e)
        }
    }

    /**
     * Gets the currently active LDAP connection settings.
     */
    fun getActiveConnection(): LdapConnection {
        // If no saved connections or selected connection is -1, use default
        if (ldap.savedConnections.isEmpty() || ldap.selectedConnection < 0) {
            return LdapConnection(
                name = "Default",
                host = ldap.host,
                port = ldap.port,
                baseDN = ldap.baseDN,
                useSSL = ldap.useSSL,
                useTLS = ldap.useTLS,
                bindUser = ldap.bindUser,
                bindPass = ldap.bindPass
            )
        }

        // Validate selected connection index
        val index = if (ldap.selectedConnection >= ldap.savedConnections.size) {
            0
        } else {
            ldap.selectedConnection
        }

        val saved = ldap.savedConnections[index]
        return LdapConnection(
            name = saved.name,
            host = saved.host,
            port = saved.port,
            baseDN = saved.baseDN,
            useSSL = saved.useSSL,
            useTLS = saved.useTLS,
            bindUser = saved.bindUser,
            bindPass = saved.bindPass
        )
    }

    /**
     * Sets the active connection by index.
     */
    fun setActiveConnection(index: Int): Config {
        if (index < 0 || index >= ldap.savedConnections.size) {
            return copy(ldap = ldap.copy(selectedConnection = -1))
        }

        val saved = ldap.savedConnections[index]
        return copy(
            ldap = ldap.copy(
                host = saved.host,
                port = saved.port,
                baseDN = saved.baseDN,
                useSSL = saved.useSSL,
                useTLS = saved.useTLS,
                bindUser = saved.bindUser,
                bindPass = saved.bindPass,
                selectedConnection = index
            )
        )
    }

    /**
     * Adds a saved connection.
     */
    fun addSavedConnection(connection: SavedConnection): Config {
        return copy(
            ldap = ldap.copy(
                savedConnections = ldap.savedConnections + connection
            )
        )
    }

    /**
     * Removes a saved connection by index.
     */
    fun removeSavedConnection(index: Int): Config {
        if (index < 0 || index >= ldap.savedConnections.size) {
            return this
        }

        val newConnections = ldap.savedConnections.toMutableList().apply {
            removeAt(index)
        }

        val newSelectedConnection = when {
            ldap.selectedConnection == index -> -1
            ldap.selectedConnection > index -> ldap.selectedConnection - 1
            else -> ldap.selectedConnection
        }

        return copy(
            ldap = ldap.copy(
                savedConnections = newConnections,
                selectedConnection = newSelectedConnection
            )
        )
    }

    /**
     * Updates a saved connection by index.
     */
    fun updateSavedConnection(index: Int, connection: SavedConnection): Config {
        if (index < 0 || index >= ldap.savedConnections.size) {
            return this
        }

        val newConnections = ldap.savedConnections.toMutableList().apply {
            set(index, connection)
        }

        var result = copy(ldap = ldap.copy(savedConnections = newConnections))

        // If this is the currently selected connection, update the active settings
        if (ldap.selectedConnection == index) {
            result = result.setActiveConnection(index)
        }

        return result
    }

    /**
     * Validates and repairs the configuration.
     *
     * @return List of warning messages for issues that were repaired
     */
    fun validateAndRepair(): Pair<Config, List<String>> {
        val warnings = mutableListOf<String>()
        var result = this

        // Check if selected connection index is out of bounds
        if (ldap.savedConnections.isNotEmpty() &&
            ldap.selectedConnection >= ldap.savedConnections.size
        ) {
            warnings.add(
                "Selected connection index ${ldap.selectedConnection} was invalid " +
                        "(only ${ldap.savedConnections.size} connections exist). Reset to first connection."
            )
            result = result.copy(ldap = ldap.copy(selectedConnection = 0))
        }

        return result to warnings
    }
}

/**
 * LDAP connection configuration.
 */
@Serializable
data class LdapConfig(
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSSL: Boolean = false,
    val useTLS: Boolean = false,
    val bindUser: String = "",
    val bindPass: String = "",
    val savedConnections: List<SavedConnection> = emptyList(),
    val selectedConnection: Int = -1
) {
    fun withDefaults(): LdapConfig {
        return copy(
            port = if (port == 0) {
                if (useSSL) 636 else 389
            } else port
        )
    }
}

/**
 * Saved LDAP connection profile.
 */
@Serializable
data class SavedConnection(
    val name: String,
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSSL: Boolean = false,
    val useTLS: Boolean = false,
    val bindUser: String = "",
    val bindPass: String = ""
)

/**
 * Active LDAP connection (runtime representation).
 */
data class LdapConnection(
    val name: String,
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSSL: Boolean,
    val useTLS: Boolean,
    val bindUser: String,
    val bindPass: String
)

/**
 * Pagination configuration.
 */
@Serializable
data class PaginationConfig(
    val pageSize: Int = 50
) {
    fun withDefaults(): PaginationConfig {
        return copy(
            pageSize = if (pageSize <= 0) 50 else pageSize
        )
    }
}

/**
 * Retry configuration for LDAP operations.
 */
@Serializable
data class RetryConfig(
    val enabled: Boolean = true,
    val maxAttempts: Int = 3,
    val initialDelayMs: Int = 500,
    val maxDelayMs: Int = 5000
) {
    fun withDefaults(): RetryConfig {
        return copy(
            maxAttempts = if (maxAttempts <= 0) 3 else maxAttempts,
            initialDelayMs = if (initialDelayMs <= 0) 500 else initialDelayMs,
            maxDelayMs = if (maxDelayMs <= 0) 5000 else maxDelayMs
        )
    }
}

/**
 * Exception thrown for configuration-related errors.
 */
class ConfigException(message: String, cause: Throwable? = null) : Exception(message, cause)
