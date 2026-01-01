package com.moribito.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {

    @Test
    fun testEffectiveBindCredentialsLegacy() {
        val config = LdapConfig(
            name = "Test",
            host = "localhost",
            port = 389,
            baseDN = "dc=example,dc=com",
            _legacyBindUser = "admin",
            _legacyBindPass = "password"
        )
        
        val effective = config.effectiveBindCredentials
        assertEquals(1, effective.size)
        assertEquals("admin", effective[0].bindUser)
        assertEquals("password", effective[0].bindPass)
        assertTrue(effective[0].isDefault)
    }

    @Test
    fun testEffectiveBindCredentialsNew() {
        val credentials = listOf(
            BindCredential(label = "Admin", bindUser = "cn=admin", bindPass = "admin123"),
            BindCredential(label = "User", bindUser = "cn=user", bindPass = "user123", isDefault = true)
        )
        val config = LdapConfig(
            name = "Test",
            host = "localhost",
            port = 389,
            baseDN = "dc=example,dc=com",
            bindCredentials = credentials
        )
        
        val effective = config.effectiveBindCredentials
        assertEquals(2, effective.size)
        assertEquals("cn=admin", effective[0].bindUser)
        assertEquals("cn=user", effective[1].bindUser)
        assertTrue(effective[1].isDefault)
    }

    @Test
    fun testEffectiveBindCredentialsEmpty() {
        val config = LdapConfig(
            name = "Test",
            host = "localhost",
            port = 389,
            baseDN = "dc=example,dc=com"
        )
        
        val effective = config.effectiveBindCredentials
        assertTrue(effective.isEmpty())
    }
}
