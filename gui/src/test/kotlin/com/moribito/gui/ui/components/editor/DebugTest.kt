package com.moribito.gui.ui.components.editor

import com.moribito.ldap.LdapAttribute
import com.moribito.ldap.LdapSchema
import com.moribito.ldap.query.QueryContext
import kotlin.test.Test

class DebugTest {
    @Test
    fun debugAutocomplete() {
        val schema = LdapSchema(
            attributes = listOf(LdapAttribute("cn", "Common Name", null)),
            containerDns = listOf("ou=people,dc=example,dc=com")
        )
        val state = AutocompleteState(schema)
        
        val testCases = listOf(
            "select * from " to 14,
            "SELECT * FROM " to 14,
            "select * from" to 13,
            "select * from ou" to 16
        )
        
        testCases.forEach { (text, pos) ->
            state.update(text, pos)
            val suggestionsStr = state.suggestions.joinToString { it.insertValue }
            val logMessage = "Text: '$text', Pos: $pos, Len: ${text.length} -> Context: ${state.context}, Query: '${state.query}', Visible: ${state.isVisible}, Suggestions: ${state.suggestions.size} ($suggestionsStr)"
            System.err.println("[DEBUG_LOG] $logMessage")
        }
    }
}
