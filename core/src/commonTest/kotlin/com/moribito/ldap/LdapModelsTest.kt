package com.moribito.ldap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull

class EntryTest {
    @Test
    fun `getAttributeValue returns first value`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf(
                "cn" to listOf("test", "test2"),
                "mail" to listOf("test@example.com")
            )
        )

        assertEquals("test", entry.getAttributeValue("cn"))
        assertEquals("test@example.com", entry.getAttributeValue("mail"))
    }

    @Test
    fun `getAttributeValue returns null for missing attribute`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf("cn" to listOf("test"))
        )

        assertNull(entry.getAttributeValue("mail"))
    }

    @Test
    fun `getAttributeValues returns all values`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf("cn" to listOf("test", "test2"))
        )

        assertEquals(listOf("test", "test2"), entry.getAttributeValues("cn"))
    }

    @Test
    fun `getAttributeValues returns empty list for missing attribute`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf("cn" to listOf("test"))
        )

        assertEquals(emptyList(), entry.getAttributeValues("mail"))
    }

    @Test
    fun `hasAttribute returns true for existing attribute`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf("cn" to listOf("test"))
        )

        assertTrue(entry.hasAttribute("cn"))
    }

    @Test
    fun `hasAttribute returns false for missing attribute`() {
        val entry = Entry(
            dn = "cn=test,dc=example,dc=com",
            attributes = mapOf("cn" to listOf("test"))
        )

        assertFalse(entry.hasAttribute("mail"))
    }
}

class TreeNodeTest {
    @Test
    fun `withChildren creates loaded node`() {
        val node = TreeNode(
            dn = "ou=users,dc=example,dc=com",
            name = "ou=users",
            children = null,
            isLoaded = false
        )

        val children = listOf(
            TreeNode("cn=user1,ou=users,dc=example,dc=com", "cn=user1", null, false)
        )

        val loadedNode = node.withChildren(children)

        assertEquals(children, loadedNode.children)
        assertTrue(loadedNode.isLoaded)
    }

    @Test
    fun `hasChildren returns true when children exist`() {
        val node = TreeNode(
            dn = "ou=users,dc=example,dc=com",
            name = "ou=users",
            children = listOf(
                TreeNode("cn=user1,ou=users,dc=example,dc=com", "cn=user1", null, false)
            ),
            isLoaded = true
        )

        assertTrue(node.hasChildren())
    }

    @Test
    fun `hasChildren returns false when children is null`() {
        val node = TreeNode(
            dn = "ou=users,dc=example,dc=com",
            name = "ou=users",
            children = null,
            isLoaded = false
        )

        assertFalse(node.hasChildren())
    }

    @Test
    fun `hasChildren returns false when children is empty`() {
        val node = TreeNode(
            dn = "ou=users,dc=example,dc=com",
            name = "ou=users",
            children = emptyList(),
            isLoaded = true
        )

        assertFalse(node.hasChildren())
    }
}

class SearchScopeTest {
    @Test
    fun `toInt returns correct values`() {
        assertEquals(0, SearchScope.BASE.toInt())
        assertEquals(1, SearchScope.ONE_LEVEL.toInt())
        assertEquals(2, SearchScope.SUBTREE.toInt())
    }
}

class SearchPageTest {
    @Test
    fun `equals compares byte arrays correctly`() {
        val cookie1 = byteArrayOf(1, 2, 3)
        val cookie2 = byteArrayOf(1, 2, 3)
        val cookie3 = byteArrayOf(1, 2, 4)

        val page1 = SearchPage(
            entries = emptyList(),
            hasMore = true,
            cookie = cookie1,
            pageSize = 50
        )

        val page2 = SearchPage(
            entries = emptyList(),
            hasMore = true,
            cookie = cookie2,
            pageSize = 50
        )

        val page3 = SearchPage(
            entries = emptyList(),
            hasMore = true,
            cookie = cookie3,
            pageSize = 50
        )

        assertEquals(page1, page2)
        assertFalse(page1 == page3)
    }

    @Test
    fun `equals handles null cookies`() {
        val page1 = SearchPage(
            entries = emptyList(),
            hasMore = false,
            cookie = null,
            pageSize = 50
        )

        val page2 = SearchPage(
            entries = emptyList(),
            hasMore = false,
            cookie = null,
            pageSize = 50
        )

        assertEquals(page1, page2)
    }
}

class LdapExceptionTest {
    @Test
    fun `isRetryableResultCode identifies retryable codes`() {
        assertTrue(LdapException.isRetryableResultCode(LdapException.RESULT_BUSY))
        assertTrue(LdapException.isRetryableResultCode(LdapException.RESULT_UNAVAILABLE))
        assertTrue(LdapException.isRetryableResultCode(LdapException.RESULT_UNWILLING_TO_PERFORM))
        assertTrue(LdapException.isRetryableResultCode(LdapException.RESULT_SERVER_DOWN))
        assertTrue(LdapException.isRetryableResultCode(LdapException.RESULT_CONNECT_ERROR))
    }

    @Test
    fun `isRetryableResultCode identifies non-retryable codes`() {
        assertFalse(LdapException.isRetryableResultCode(LdapException.RESULT_SUCCESS))
        assertFalse(LdapException.isRetryableResultCode(LdapException.RESULT_NO_SUCH_OBJECT))
        assertFalse(LdapException.isRetryableResultCode(LdapException.RESULT_INVALID_CREDENTIALS))
        assertFalse(LdapException.isRetryableResultCode(LdapException.RESULT_INSUFFICIENT_ACCESS_RIGHTS))
    }
}
