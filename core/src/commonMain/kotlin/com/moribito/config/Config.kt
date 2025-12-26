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
    @SerialName("bind_user") val bindUser: String = "",
    @SerialName("bind_pass") val bindPass: String = ""
)

@Serializable
data class GeneralSettings(
    @SerialName("default_connection_index") val defaultIndex: Int = 0,
    @SerialName("dark_mode") val darkMode: Boolean = true
)