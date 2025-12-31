package com.moribito.gui.ui.components.editor.highlighting

/**
 * Represents a type of token in a SQL query.
 */
sealed class SqlTokenType {
    /** Keywords: SELECT, FROM, WHERE, AND, OR, NOT, LIKE */
    object Keyword : SqlTokenType()

    /** Identifiers: Column names, Table names */
    object Identifier : SqlTokenType()

    /** String literals */
    object String : SqlTokenType()

    /** Operators: =, !=, <, >, <=, >=, * */
    object Operator : SqlTokenType()

    /** Punctuation: , ( ) */
    object Punctuation : SqlTokenType()

    /** Whitespace characters */
    object Whitespace : SqlTokenType()

    /** Invalid or malformed syntax */
    object Invalid : SqlTokenType()

    override fun toString(): kotlin.String {
        return when (this) {
            Keyword -> "Keyword"
            Identifier -> "Identifier"
            String -> "String"
            Operator -> "Operator"
            Punctuation -> "Punctuation"
            Whitespace -> "Whitespace"
            Invalid -> "Invalid"
        }

    }
}

/**
 * Represents a single token in a SQL query with its position.
 */
data class SqlToken(
    val type: SqlTokenType,
    val text: String,
    val startIndex: Int,
    val endIndex: Int
)
