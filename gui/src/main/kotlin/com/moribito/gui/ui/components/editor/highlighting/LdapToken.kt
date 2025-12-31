package com.moribito.gui.ui.components.editor.highlighting

/**
 * Represents a type of token in an LDAP filter.
 */
sealed class LdapTokenType {
    /** Logical operators: &, |, ! */
    object LogicalOperator : LdapTokenType()

    /** Parentheses: (, ) */
    object Parenthesis : LdapTokenType()

    /** Attribute names: cn, uid, objectClass, etc. */
    object Attribute : LdapTokenType()

    /** Comparison operators: =, >=, <=, ~=, := */
    object ComparisonOperator : LdapTokenType()

    /** Values after comparison operators */
    object Value : LdapTokenType()

    /** Wildcard character * in values */
    object Wildcard : LdapTokenType()

    /** Whitespace characters */
    object Whitespace : LdapTokenType()

    /** Invalid or malformed syntax */
    object Invalid : LdapTokenType()
}

/**
 * Represents a single token in an LDAP filter with its position.
 */
data class LdapToken(
    val type: LdapTokenType,
    val text: String,
    val startIndex: Int,
    val endIndex: Int
)
