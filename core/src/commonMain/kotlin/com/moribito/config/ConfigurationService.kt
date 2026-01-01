package com.moribito.config

import com.akuleshov7.ktoml.Toml
import java.io.File

class ConfigurationService {
    private val toml = Toml()

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
        println("[ConfigurationService] Loading config from: ${file.absolutePath}")

        if (!file.exists()) {
            println("[ConfigurationService] Config file does not exist, returning default")
            return RootConfig(connections = listOf(LdapConfig()))
        }

        val content = file.readText()
        println("[ConfigurationService] File content length: ${content.length} bytes")

        return decode(content)
    }

    private fun decode(content: String): RootConfig {
        return try {
            val config = toml.decodeFromString(RootConfig.serializer(), content)
            println("[ConfigurationService] Successfully decoded ${config.connections.size} connections")

            // Apply migration to all connections
            val migratedConfig = config.copy(
                connections = config.connections.map { migrateLdapConfig(it) }
            )
            println("[ConfigurationService] Migration complete")

            migratedConfig
        } catch (e: Exception) {
            println("[ConfigurationService] Format error: ${e.message}")
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
            println("[ConfigurationService] Config '${config.name}' already has ${config.bindCredentials.size} credentials")
            return config
        }

        // No legacy credentials either
        if (config._legacyBindUser.isNullOrEmpty()) {
            println("[ConfigurationService] Config '${config.name}' has no credentials (anonymous bind)")
            return config
        }

        // Migrate legacy single credential to new format
        println("[ConfigurationService] Migrating config '${config.name}' from legacy format")
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
        println("[ConfigurationService] Importing from: ${externalFile.absolutePath}")

        if (!externalFile.exists()) {
            println("[ConfigurationService] External file does not exist")
            return emptyList()
        }

        val importedRoot = decode(externalFile.readText())
        println("[ConfigurationService] Imported ${importedRoot.connections.size} connections")
        return importedRoot.connections
    }

    fun save(config: RootConfig) {
        val file = getDefaultPath()
        println("[ConfigurationService] Saving config to: ${file.absolutePath}")
        println("[ConfigurationService] Saving ${config.connections.size} connections")

        // Log each connection being saved
        config.connections.forEachIndexed { index, conn ->
            println("[ConfigurationService]   [$index] name='${conn.name}', host='${conn.host}', port=${conn.port}")
        }

        file.parentFile?.mkdirs()
        val content = toml.encodeToString(RootConfig.serializer(), config)

        println("[ConfigurationService] Encoded TOML length: ${content.length} bytes")
        println("[ConfigurationService] TOML content preview:")
        println(content.take(500))

        file.writeText(content)
        println("[ConfigurationService] Successfully wrote config to disk")

        // Verify the write
        if (file.exists()) {
            println("[ConfigurationService] Verification: File exists with size ${file.length()} bytes")
        } else {
            println("[ConfigurationService] ERROR: File does not exist after write!")
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