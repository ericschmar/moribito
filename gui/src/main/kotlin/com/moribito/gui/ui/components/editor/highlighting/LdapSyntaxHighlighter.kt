package com.moribito.gui.ui.components.editor.highlighting

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Parse states for the LDAP filter lexer.
 */
private enum class ParseState {
    NEUTRAL,       // Looking for next token
    IN_ATTRIBUTE,  // Reading attribute name (before =)
    IN_COMPARISON, // Reading comparison operator (=, >=, etc.)
    IN_VALUE,      // Reading value (after =)
    AFTER_ESCAPE   // Just saw backslash in value
}

/**
 * Parse an LDAP filter string into a list of tokens.
 *
 * Supports LDAP filter syntax including:
 * - Logical operators: &, |, !
 * - Parentheses: (, )
 * - Attributes: cn, uid, objectClass, etc.
 * - Comparison operators: =, >=, <=, ~=, :=
 * - Values with wildcards
 * - Escape sequences
 *
 * @param text The LDAP filter string to parse
 * @return List of tokens with their types and positions
 */
fun parseLdapFilter(text: String): List<LdapToken> {
    if (text.isBlank()) return emptyList()

    val tokens = mutableListOf<LdapToken>()
    var state = ParseState.NEUTRAL
    var currentTokenStart = 0
    val currentText = StringBuilder()
    var previousChar = ' '

    fun addToken(type: LdapTokenType, tokenText: String, index: Int) {
        if (tokenText.isNotEmpty()) {
            tokens.add(LdapToken(type, tokenText, index, index + tokenText.length))
        }
    }

    fun addCurrentToken(type: LdapTokenType, endIndex: Int) {
        if (currentText.isNotEmpty()) {
            addToken(type, currentText.toString(), currentTokenStart)
            currentText.clear()
        }
    }

    text.forEachIndexed { index, char ->
        when (state) {
            ParseState.NEUTRAL -> {
                when (char) {
                    '(' -> {
                        addToken(LdapTokenType.Parenthesis, "(", index)
                    }
                    ')' -> {
                        addToken(LdapTokenType.Parenthesis, ")", index)
                    }
                    '&', '|', '!' -> {
                        // Logical operators only valid after opening parenthesis
                        if (previousChar == '(') {
                            addToken(LdapTokenType.LogicalOperator, char.toString(), index)
                        } else {
                            // Start of attribute or invalid
                            state = ParseState.IN_ATTRIBUTE
                            currentTokenStart = index
                            currentText.append(char)
                        }
                    }
                    in 'a'..'z', in 'A'..'Z', in '0'..'9', '_', '-' -> {
                        state = ParseState.IN_ATTRIBUTE
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    ' ', '\t', '\n', '\r' -> {
                        // Skip whitespace
                    }
                }
            }

            ParseState.IN_ATTRIBUTE -> {
                when {
                    char in charArrayOf('=', '>', '<', '~', ':') -> {
                        // End attribute, start comparison operator
                        addCurrentToken(LdapTokenType.Attribute, index)
                        state = ParseState.IN_COMPARISON
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    char.isLetterOrDigit() || char == '-' || char == '_' || char == '.' -> {
                        currentText.append(char)
                    }
                    char == ')' -> {
                        // Attribute ended without operator (invalid, but handle gracefully)
                        addCurrentToken(LdapTokenType.Attribute, index)
                        addToken(LdapTokenType.Parenthesis, ")", index)
                        state = ParseState.NEUTRAL
                    }
                    char in charArrayOf(' ', '\t', '\n', '\r') -> {
                        // Skip whitespace in attributes
                    }
                    else -> {
                        // Invalid character in attribute
                        currentText.append(char)
                    }
                }
            }

            ParseState.IN_COMPARISON -> {
                when (char) {
                    '=' -> {
                        // Complete comparison operator (e.g., >=, <=, ~=, :=)
                        currentText.append(char)
                        addCurrentToken(LdapTokenType.ComparisonOperator, index + 1)
                        state = ParseState.IN_VALUE
                        currentTokenStart = index + 1
                    }
                    in 'a'..'z', in 'A'..'Z', in '0'..'9', '*', '\\', ' ', '-', '_', '.' -> {
                        // If we only had a single char operator (like =), we're now in value
                        if (currentText.length == 1 && currentText[0] == '=') {
                            addCurrentToken(LdapTokenType.ComparisonOperator, index)
                            state = ParseState.IN_VALUE
                            currentTokenStart = index
                            currentText.append(char)
                        } else {
                            currentText.append(char)
                        }
                    }
                    ')' -> {
                        // Comparison operator ended, no value
                        addCurrentToken(LdapTokenType.ComparisonOperator, index)
                        addToken(LdapTokenType.Parenthesis, ")", index)
                        state = ParseState.NEUTRAL
                    }
                    else -> {
                        currentText.append(char)
                    }
                }
            }

            ParseState.IN_VALUE -> {
                when (char) {
                    ')' -> {
                        // End of value
                        addCurrentToken(LdapTokenType.Value, index)
                        addToken(LdapTokenType.Parenthesis, ")", index)
                        state = ParseState.NEUTRAL
                    }
                    '\\' -> {
                        state = ParseState.AFTER_ESCAPE
                        currentText.append(char)
                    }
                    else -> {
                        currentText.append(char)
                    }
                }
            }

            ParseState.AFTER_ESCAPE -> {
                // After escape, accept any character and return to IN_VALUE
                currentText.append(char)
                state = ParseState.IN_VALUE
            }
        }

        previousChar = char
    }

    // Handle any remaining token
    when (state) {
        ParseState.IN_ATTRIBUTE -> addCurrentToken(LdapTokenType.Attribute, text.length)
        ParseState.IN_COMPARISON -> addCurrentToken(LdapTokenType.ComparisonOperator, text.length)
        ParseState.IN_VALUE, ParseState.AFTER_ESCAPE -> addCurrentToken(LdapTokenType.Value, text.length)
        ParseState.NEUTRAL -> {
            // Nothing to do
        }
    }

    // Post-process to detect wildcards in values
    return tokens.flatMap { token ->
        if (token.type is LdapTokenType.Value && token.text.contains('*')) {
            // Split value into parts with wildcards highlighted separately
            val parts = mutableListOf<LdapToken>()
            var currentIndex = token.startIndex
            val valueText = token.text
            var i = 0

            while (i < valueText.length) {
                if (valueText[i] == '*') {
                    parts.add(LdapToken(LdapTokenType.Wildcard, "*", currentIndex, currentIndex + 1))
                    currentIndex++
                    i++
                } else {
                    // Collect non-wildcard characters
                    val start = i
                    while (i < valueText.length && valueText[i] != '*') {
                        i++
                    }
                    val segment = valueText.substring(start, i)
                    parts.add(LdapToken(LdapTokenType.Value, segment, currentIndex, currentIndex + segment.length))
                    currentIndex += segment.length
                }
            }
            parts
        } else {
            listOf(token)
        }
    }
}

