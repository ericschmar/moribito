package com.moribito.config

import LdapConfig
import RootConfig
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
            config
        } catch (e: Exception) {
            println("[ConfigurationService] Format error: ${e.message}")
            RootConfig()
        }
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