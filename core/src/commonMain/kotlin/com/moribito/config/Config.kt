package com.moribito.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RootConfig(
    // This allows [[connections]] in TOML
    val connections: List<LdapConfig> = emptyList(),
    val settings: GeneralSettings = GeneralSettings()
)

@Serializable
data class LdapConfig(
    val name: String = "Default",
    val host: String = "localhost",
    val port: Int = 389,
    @SerialName("base_dn") val baseDN: String = "dc=example,dc=com",
    @SerialName("use_ssl") val useSsl: Boolean = false,
    @SerialName("use_tls") val useTls: Boolean = false,

    // Legacy fields for backward compatibility
    @SerialName("bind_user") val _legacyBindUser: String? = null,
    @SerialName("bind_pass") val _legacyBindPass: String? = null,

    // New field for multiple bind credentials
    @SerialName("bind_credentials") val bindCredentials: List<BindCredential> = emptyList()
) {
    /**
     * Returns the effective bind credentials, migrating legacy single credential if needed.
     * This provides a unified interface for both old and new configuration formats.
     */
    val effectiveBindCredentials: List<BindCredential>
        get() = if (bindCredentials.isEmpty() && !_legacyBindUser.isNullOrEmpty()) {
            // Migrate legacy single credential to new format
            listOf(
                BindCredential(
                    label = "Default",
                    bindUser = _legacyBindUser,
                    bindPass = _legacyBindPass ?: "",
                    isDefault = true
                )
            )
        } else {
            bindCredentials
        }
}

@Serializable
data class GeneralSettings(
    @SerialName("default_connection_index") val defaultIndex: Int = 0,
    @SerialName("dark_mode") val darkMode: Boolean = true
)