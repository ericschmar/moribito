package com.moribito.gui.ui.components.editor

import androidx.compose.runtime.*
import com.moribito.ldap.LdapSchema
import com.moribito.ldap.query.QueryContext
import com.moribito.ldap.query.SqlParser

data class AutocompleteSuggestion(
    val displayValue: String,
    val insertValue: String,
    val description: String? = null,
    val isContainer: Boolean = false
)

@Stable
class AutocompleteState(
    var schema: LdapSchema? = null
) {
    var isVisible by mutableStateOf(false)
    var suggestions by mutableStateOf(emptyList<AutocompleteSuggestion>())
    var selectedIndex by mutableStateOf(0)
    var context by mutableStateOf(QueryContext.NONE)
    var query by mutableStateOf("")

    fun updateSchema(LdapSchema: LdapSchema?) {
        this.schema = LdapSchema
    }

    fun update(text: String, cursorPosition: Int) {
        val detected = detectContext(text, cursorPosition)
        context = detected.first
        query = detected.second

        if (context == QueryContext.NONE) {
            isVisible = false
            suggestions = emptyList()
            return
        }

        // Special case: don't show for LDAP filter if we're just at the beginning and haven't typed anything
        if (context == QueryContext.LDAP_FILTER && query.isEmpty() && text.isEmpty()) {
            isVisible = false
            suggestions = emptyList()
            return
        }

        suggestions = fetchSuggestions(context, query)
        
        // Don't show if the query is exactly a keyword being typed
        val keywords = setOf("SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "LIKE")
        val isKeyword = query.uppercase() in keywords
        
        isVisible = suggestions.isNotEmpty() && !isKeyword
        selectedIndex = 0
    }

    private fun detectContext(text: String, cursorPosition: Int): Pair<QueryContext, String> {
        if (cursorPosition < 0 || cursorPosition > text.length) return QueryContext.NONE to ""

        val trimmedText = text.trimStart()
        
        // If it starts with a paren, it's definitely an LDAP filter
        if (trimmedText.startsWith("(")) {
            val sub = text.substring(0, cursorPosition)
            val separators = charArrayOf(' ', '\n', '\t', '\r', '(', ')', '&', '|', '=', ',', '<', '>', '.')
            val lastSeparator = sub.lastIndexOfAny(separators)
            val query = if (lastSeparator == -1) sub.trim() else sub.substring(lastSeparator + 1).trim()
            return QueryContext.LDAP_FILTER to query
        }

        // Check if it's likely SQL
        val firstWord = trimmedText.substringBefore(' ').substringBefore('\n').uppercase()
        val isSql = firstWord.isNotEmpty() && "SELECT".startsWith(firstWord)
        
        if (isSql) {
            return SqlParser(text).findContextAt(cursorPosition)
        } else {
            // Default to LDAP filter (might be a simple attr=val)
            val sub = text.substring(0, cursorPosition)
            val separators = charArrayOf(' ', '\n', '\t', '\r', '(', ')', '&', '|', '=', ',', '<', '>', '.')
            val lastSeparator = sub.lastIndexOfAny(separators)
            val query = if (lastSeparator == -1) sub.trim() else sub.substring(lastSeparator + 1).trim()
            return QueryContext.LDAP_FILTER to query
        }
    }

    private fun fetchSuggestions(context: QueryContext, query: String): List<AutocompleteSuggestion> {
        val currentSchema = schema ?: return emptyList()
        
        return when (context) {
            QueryContext.SQL_SELECT, QueryContext.SQL_WHERE, QueryContext.LDAP_FILTER -> {
                val matches = currentSchema.attributes
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .map { AutocompleteSuggestion(it.name, it.name, it.description) }
                
                // Prioritize startsWith
                matches.sortedByDescending { it.insertValue.startsWith(query, ignoreCase = true) }
            }
            QueryContext.SQL_FROM -> {
                val matches = currentSchema.containerDns
                    .filter { it.contains(query, ignoreCase = true) }
                    .map { 
                        val shortName = it.split(',').firstOrNull() ?: it
                        AutocompleteSuggestion(it, shortName, isContainer = true)
                    }
                
                matches.sortedByDescending { it.displayValue.startsWith(query, ignoreCase = true) }
            }
            else -> emptyList()
        }
    }
}
