package com.moribito.gui.ui.components.editor

import com.moribito.ldap.LdapAttribute
import com.moribito.ldap.LdapSchema
import com.moribito.ldap.query.QueryContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutocompleteStateTest {

    private val schema = LdapSchema(
        attributes = listOf(
            LdapAttribute("cn", "Common Name", null),
            LdapAttribute("sn", "Surname", null),
            LdapAttribute("givenName", "Given Name", null),
            LdapAttribute("mail", "Email", null),
            LdapAttribute("objectClass", "Object Class", null)
        ),
        containerDns = listOf(
            "ou=people,dc=example,dc=com",
            "ou=groups,dc=example,dc=com",
            "cn=users,dc=example,dc=com"
        )
    )

    @Test
    fun testDetectContextSqlSelect() {
        val state = AutocompleteState(schema)
        
        // SELECT |
        state.update("SELECT ", 7)
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertEquals("", state.query)
        assertTrue(state.isVisible)
        assertEquals(5, state.suggestions.size)

        // SELECT c|
        state.update("SELECT c", 8)
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertEquals("c", state.query)
        assertTrue(state.isVisible)
        assertEquals(2, state.suggestions.size) // cn and objectClass
        assertEquals("cn", state.suggestions[0].insertValue) // Prioritized startsWith
    }

    @Test
    fun testDetectContextSqlFrom() {
        val state = AutocompleteState(schema)
        
        // SELECT * FROM |
        state.update("SELECT * FROM ", 14)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertEquals("", state.query)
        assertTrue(state.isVisible)
        assertEquals(3, state.suggestions.size)

        // SELECT * FROM pe|
        state.update("SELECT * FROM pe", 16)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertEquals("pe", state.query)
        assertTrue(state.isVisible)
        assertEquals(1, state.suggestions.size)
        assertEquals("ou=people,dc=example,dc=com", state.suggestions[0].displayValue)
        assertEquals("ou=people", state.suggestions[0].insertValue)
    }

    @Test
    fun testDetectContextSqlWhere() {
        val state = AutocompleteState(schema)
        
        // SELECT * FROM ou=people WHERE |
        val sql = "SELECT * FROM ou=people WHERE "
        state.update(sql, sql.length)
        assertEquals(QueryContext.SQL_WHERE, state.context)
        assertEquals("", state.query)
        assertTrue(state.isVisible)
        assertEquals(5, state.suggestions.size)

        // SELECT * FROM ou=people WHERE m|
        val sql2 = "SELECT * FROM ou=people WHERE m"
        state.update(sql2, sql2.length)
        assertEquals(QueryContext.SQL_WHERE, state.context)
        assertEquals("m", state.query)
        assertTrue(state.isVisible)
        assertEquals(2, state.suggestions.size) // mail and givenName
        assertEquals("mail", state.suggestions[0].insertValue)
    }

    @Test
    fun testDetectContextLdapFilter() {
        val state = AutocompleteState(schema)
        
        // (c|
        state.update("(c", 2)
        assertEquals(QueryContext.LDAP_FILTER, state.context)
        assertEquals("c", state.query)
        assertTrue(state.isVisible)
        assertEquals(2, state.suggestions.size)
        assertEquals("cn", state.suggestions[0].insertValue)

        // (&(objectClass=person)(m|
        val filter = "(&(objectClass=person)(m"
        state.update(filter, filter.length)
        assertEquals(QueryContext.LDAP_FILTER, state.context)
        assertEquals("m", state.query)
        assertTrue(state.isVisible)
        assertEquals(2, state.suggestions.size) // mail and givenName
        assertEquals("mail", state.suggestions[0].insertValue)
    }

    @Test
    fun testNoPopupWhileTypingKeywords() {
        val state = AutocompleteState(schema)
        
        // "S" -> Prefix of SELECT, should not show
        state.update("S", 1)
        assertEquals(QueryContext.NONE, state.context)
        assertTrue(!state.isVisible)
        
        // "SELECT" -> Exactly keyword, should not show
        state.update("SELECT", 6)
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertTrue(!state.isVisible)
        
        // "SELECT " -> Space after keyword, SHOULD show
        state.update("SELECT ", 7)
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertTrue(state.isVisible)
        assertEquals(5, state.suggestions.size)
        
        // "SELECT cn, " -> Another space/comma, should show all
        state.update("SELECT cn, ", 11)
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertTrue(state.isVisible)
        
        // "SELECT * FROM" -> FROM is keyword, should not show
        state.update("SELECT * FROM", 13)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(!state.isVisible)
    }

    @Test
    fun testTrailingFrom() {
        val state = AutocompleteState(schema)
        
        // SELECT * FROM |
        // Note: There's a trailing space here
        state.update("SELECT * FROM ", 14)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(state.isVisible)
        assertEquals(3, state.suggestions.size)
    }

    @Test
    fun testLowercaseSql() {
        val state = AutocompleteState(schema)
        val sql = "select * from "
        state.update(sql, sql.length)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(state.isVisible, "Popup should be visible for lowercase SQL")
        assertEquals(3, state.suggestions.size)
    }

    @Test
    fun testSelectAfterAsterisk() {
        val state = AutocompleteState(schema)
        val sql = "SELECT * "
        state.update(sql, sql.length)
        // This should probably be SQL_SELECT or transition to SQL_FROM
        // Current implementation will stay in SQL_SELECT until FROM is typed
        assertEquals(QueryContext.SQL_SELECT, state.context)
        assertTrue(state.isVisible)
    }

    @Test
    fun testMultipleSpaces() {
        val state = AutocompleteState(schema)
        val sql = "  select   *   from   "
        state.update(sql, sql.length)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(state.isVisible)
    }

    @Test
    fun testCursorInMiddle() {
        val state = AutocompleteState(schema)
        val sql = "SELECT * FROM  WHERE cn='john'"
        // Cursor after FROM (index 14)
        state.update(sql, 14)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(state.isVisible)
    }

    @Test
    fun testNewlineSeparators() {
        val state = AutocompleteState(schema)
        val sql = "SELECT *\nFROM\n"
        state.update(sql, sql.length)
        assertEquals(QueryContext.SQL_FROM, state.context)
        assertTrue(state.isVisible)
    }
}
