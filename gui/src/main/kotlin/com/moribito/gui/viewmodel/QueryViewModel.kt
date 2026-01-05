package com.moribito.gui.viewmodel

import com.moribito.ldap.*
import com.moribito.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing LDAP queries.
 */
class QueryViewModel(
    private val stateHolder: AppStateHolder,
    private val clientProvider: () -> ILdapClient?
) {
    private val logger = Logger.get("QueryViewModel")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Updates the query text in the state.
     */
    fun updateQueryText(text: String) {
        stateHolder.update { it.copy(queryText = text) }
    }

    /**
     * Formats the current query text.
     */
    fun formatQuery() {
        val currentText = stateHolder.value.queryText
        if (currentText.isBlank()) return

        // Basic SQL-like LDAP query formatting
        // SELECT * FROM baseDN WHERE filter
        val regex = Regex("SELECT\\s+(.+)\\s+FROM\\s+(.+)\\s+WHERE\\s+(.+)", RegexOption.IGNORE_CASE)
        val match = regex.find(currentText)

        if (match != null) {
            val (attrs, base, filter) = match.destructured
            val formattedFilter = formatLdapFilter(filter.trim())
            val newText = "SELECT ${attrs.trim()}\nFROM ${base.trim()}\nWHERE $formattedFilter"
            stateHolder.update { it.copy(queryText = newText) }
        }
    }

    /**
     * Formats an LDAP filter string with indentation.
     */
    fun formatLdapFilter(filter: String): String {
        var indent = 0
        val sb = StringBuilder()
        var i = 0
        while (i < filter.length) {
            when (val c = filter[i]) {
                '(' -> {
                    if (i > 0 && filter[i - 1] != '(') {
                        sb.append("\n" + "  ".repeat(indent))
                    }
                    sb.append(c)
                    indent++
                }
                ')' -> {
                    indent--
                    sb.append(c)
                }
                '&', '|', '!' -> {
                    sb.append(c)
                }
                ' ' -> {
                    if (sb.isNotEmpty() && sb.last() != '\n') sb.append(c)
                }
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Executes the current query.
     */
    fun executeQuery() {
        val client = clientProvider() ?: return

        val filter = stateHolder.value.queryText

        if (filter.isBlank()) {
            stateHolder.update { it.copy(errorMessage = "Query filter cannot be empty") }
            return
        }

        scope.launch {
            try {
                stateHolder.update { it.copy(
                    loadingState = LoadingState.Loading("Executing query...")
                )}

                val results = if (filter.trim().uppercase().startsWith("SELECT")) {
                    client.executeSqlQuery(filter)
                } else {
                    client.customSearch(filter)
                }
                
                // Log query results
                logger.info("Query completed: filter=$filter, found ${results.size} results")
                
                // Convert results to tree nodes
                val resultNodes = results.map { it.toTreeNode() }
                val resultsRoot = TreeNode(
                    dn = stateHolder.value.treeRoot?.dn ?: "",
                    name = "Query Results (${results.size})",
                    children = resultNodes,
                    isLoaded = true
                )

                stateHolder.update { it.copy(
                    queryResults = results,
                    queryResultsRoot = resultsRoot,
                    isShowingQueryResults = true,
                    loadingState = LoadingState.Success("Found ${results.size} result(s)")
                )}

                // Clear success message after a delay
                delay(3000)
                stateHolder.update { it.copy(loadingState = LoadingState.Idle) }

            } catch (e: Exception) {
                logger.error("Query execution failed", e)
                stateHolder.update { it.copy(
                    loadingState = LoadingState.Failed("Query failed: ${e.message}"),
                    errorMessage = "Query failed: ${e.message}"
                )}
            }
        }
    }

    /**
     * Selects a query result entry.
     */
    fun selectQueryResult(entry: Entry) {
        stateHolder.update { it.copy(
            selectedEntry = entry,
        )}
    }

    fun cleanup() {
        scope.cancel()
    }
}
