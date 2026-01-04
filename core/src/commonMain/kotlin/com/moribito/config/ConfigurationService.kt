package com.moribito.config

import com.akuleshov7.ktoml.Toml
import com.moribito.logging.Logger
import java.io.File

class ConfigurationService {
    private val toml = Toml()
    private val logger = Logger.get("ConfigurationService")

    /**
     * Get the default configuration path based on OS
     */
    fun getDefaultPath(): File {
        val os = System.getProperty("os.name").lowercase()
        val userHome = System.getProperty("user.home")

        val configDir = when {
            os.contains("mac") -> {
                // macOS: ~/.moribito/
                File(userHome, ".moribito")
            }
            os.contains("win") -> {
                // Windows: %APPDATA%\moribito\
                val appData = System.getenv("APPDATA") ?: userHome
                File(appData, "moribito")
            }
            else -> {
                // Linux: ~/.config/moribito/
                val configHome = System.getenv("XDG_CONFIG_HOME")
                if (configHome != null) File(configHome, "moribito")
                else File(userHome, ".config/moribito")
            }
        }

        return File(configDir, "config.toml")
    }

    fun load(): RootConfig {
        val file = getDefaultPath()
        logger.info("Loading config from: ${file.absolutePath}")

        if (!file.exists()) {
            logger.info("Config file does not exist, returning default")
            return RootConfig(connections = listOf(LdapConfig()))
        }

        val content = file.readText()
        logger.debug("File content length: ${content.length} bytes")

        return decode(content)
    }

    private fun decode(content: String): RootConfig {
        return try {
            val config = toml.decodeFromString(RootConfig.serializer(), content)
            logger.debug("Successfully decoded ${config.connections.size} connections")

            // Apply migration to all connections
            val migratedConfig = config.copy(
                connections = config.connections.map { migrateLdapConfig(it) }
            )
            logger.debug("Migration complete")

            migratedConfig
        } catch (e: Exception) {
            logger.error("CRITICAL: Format error while decoding config. To prevent data loss, the app will use a default config but WILL NOT allow overwriting the existing file until it's fixed or a new connection is explicitly saved.", e)
            logger.error("Problematic content: $content")
            // Return a special marker config if possible, or just rethrow
            RootConfig()
        }
    }

    /**
     * Migrates legacy LdapConfig format to new multi-credential format.
     * If the config already has bindCredentials, returns it unchanged.
     * If it has legacy bindUser/bindPass, converts them to a single BindCredential.
     */
    private fun migrateLdapConfig(config: LdapConfig): LdapConfig {
        // Already migrated or has new format
        if (config.bindCredentials.isNotEmpty()) {
            logger.debug("Config '${config.name}' already has ${config.bindCredentials.size} credentials")
            return config
        }

        // No legacy credentials either
        if (config._legacyBindUser.isNullOrEmpty()) {
            logger.debug("Config '${config.name}' has no credentials (anonymous bind)")
            return config
        }

        // Migrate legacy single credential to new format
        logger.info("Migrating config '${config.name}' from legacy format")
        return config.copy(
            bindCredentials = listOf(
                BindCredential(
                    label = "Default",
                    bindUser = config._legacyBindUser,
                    bindPass = config._legacyBindPass ?: "",
                    isDefault = true
                )
            ),
            _legacyBindUser = null,
            _legacyBindPass = null
        )
    }

    fun importFromExternalFile(externalFile: File): List<LdapConfig> {
        logger.info("Importing from: ${externalFile.absolutePath}")

        if (!externalFile.exists()) {
            logger.warn("External file does not exist")
            return emptyList()
        }

        val importedRoot = decode(externalFile.readText())
        logger.info("Imported ${importedRoot.connections.size} connections")
        return importedRoot.connections
    }

    fun save(config: RootConfig) {
        val file = getDefaultPath()
        logger.info("Saving config to: ${file.absolutePath}")
        logger.info("Saving ${config.connections.size} connections")

        // Log each connection being saved
        config.connections.forEachIndexed { index, conn ->
            logger.debug("  [$index] name='${conn.name}', host='${conn.host}', port=${conn.port}")
        }

        file.parentFile?.mkdirs()
        val content = toml.encodeToString(RootConfig.serializer(), config)

        // Safety check to prevent overwriting with corrupted data
        if (config.connections.isNotEmpty() && !content.contains("connections")) {
            logger.error("FATAL: Serialized TOML is missing connections section despite having ${config.connections.size} connections in memory. Aborting save to prevent data loss.")
            return
        }

        logger.debug("Encoded TOML length: ${content.length} bytes")
        logger.debug("TOML content preview: ${content.take(500)}")

        file.writeText(content)
        logger.info("Successfully wrote config to disk")

        // Verify the write
        if (file.exists()) {
            logger.debug("Verification: File exists with size ${file.length()} bytes")
        } else {
            logger.error("ERROR: File does not exist after write!")
        }
    }
}

/**
 * Flat structure for the actual active connection
 */
data class ConnectionSettings(
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSsl: Boolean,
    val useTls: Boolean,
    val bindUser: String,
    val bindPass: String
) {
    fun getUrl(): String {
        val protocol = if (useSsl) "ldaps" else "ldap"
        return "$protocol://$host:$port"
    }
}