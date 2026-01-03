package com.moribito.ldap.query

/**
 * Represents a parsed SQL-like query for LDAP.
 */
data class SqlQuery(
    val attributes: List<String>,
    val from: String,
    val where: WhereNode? = null
)

/**
 * Sealed class hierarchy for the WHERE clause AST.
 */
sealed class WhereNode {
    data class Binary(
        val left: WhereNode,
        val operator: LogicalOperator,
        val right: WhereNode
    ) : WhereNode()

    data class Not(val node: WhereNode) : WhereNode()

    data class Comparison(
        val field: String,
        val operator: ComparisonOperator,
        val value: String
    ) : WhereNode()
}

enum class LogicalOperator {
    AND, OR
}

enum class ComparisonOperator(val symbol: String) {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_EQUALS(">="),
    LESS_THAN("<"),
    LESS_EQUALS("<="),
    LIKE("LIKE")
}

/**
 * Context for autocomplete discovery.
 */
enum class QueryContext {
    SQL_SELECT,
    SQL_FROM,
    SQL_WHERE,
    LDAP_FILTER,
    NONE
}
