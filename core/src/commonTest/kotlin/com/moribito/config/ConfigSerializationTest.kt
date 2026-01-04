package com.moribito.config

import com.akuleshov7.ktoml.Toml
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigSerializationTest {
    private val toml = Toml()

    @Test
    fun testSerialization() {
        val config = RootConfig(
            connections = listOf(
                LdapConfig(name = "Test Conn", host = "localhost")
            ),
            settings = GeneralSettings(darkMode = false)
        )

        val encoded = toml.encodeToString(RootConfig.serializer(), config)
        println("Encoded TOML:\n$encoded")

        val decoded = toml.decodeFromString(RootConfig.serializer(), encoded)
        assertEquals(1, decoded.connections.size)
        assertEquals("Test Conn", decoded.connections[0].name)
        assertEquals(false, decoded.settings.darkMode)
    }

    @Test
    fun testSerializationWithCredentials() {
        val config = RootConfig(
            connections = listOf(
                LdapConfig(
                    name = "Cred Test",
                    host = "localhost",
                    bindCredentials = listOf(
                        BindCredential(label = "Admin", bindUser = "admin", bindPass = "pass")
                    )
                )
            )
        )

        val encoded = toml.encodeToString(RootConfig.serializer(), config)
        println("Encoded TOML with credentials:\n$encoded")

        val decoded = toml.decodeFromString(RootConfig.serializer(), encoded)
        assertEquals(1, decoded.connections.size)
        assertEquals(1, decoded.connections[0].bindCredentials.size)
        assertEquals("Admin", decoded.connections[0].bindCredentials[0].label)
        assertNotNull(decoded.connections[0].bindCredentials[0].id)
    }

    @Test
    fun testCompatibilityWithOldFormat() {
        // Old format: [settings] then [[connections]]
        val oldToml = """
            [settings]
            default_connection_index = 1
            dark_mode = false
            license_key = "test-key"
            
            [[connections]]
            name = "Old Conn"
            host = "oldhost"
            port = 389
            base_dn = "dc=old"
        """.trimIndent()

        val decoded = toml.decodeFromString(RootConfig.serializer(), oldToml)
        assertEquals(1, decoded.settings.defaultIndex)
        assertEquals(1, decoded.connections.size)
        assertEquals("Old Conn", decoded.connections[0].name)
    }

    @Test
    fun testCompatibilityWithReverseOrder() {
        // Even older format or different order: [[connections]] then [settings]
        val oldToml = """
            [[connections]]
            name = "Old Conn"
            host = "oldhost"
            
            [settings]
            default_connection_index = 1
        """.trimIndent()

        val decoded = toml.decodeFromString(RootConfig.serializer(), oldToml)
        assertEquals(1, decoded.settings.defaultIndex)
        assertEquals(1, decoded.connections.size)
        assertEquals("Old Conn", decoded.connections[0].name)
    }

    private fun assertNotNull(value: Any?) {
        kotlin.test.assertNotNull(value)
    }
}
