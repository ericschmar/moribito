package com.moribito.gui.ui.components.editor.highlighting

import androidx.compose.ui.graphics.Color
import com.moribito.gui.ui.components.editor.drawing.LineSegment

/**
 * Parse states for the SQL lexer.
 */
private enum class SqlParseState {
    NEUTRAL,       // Looking for next token
    IN_IDENTIFIER, // Reading identifier
    IN_STRING_SINGLE, // Reading single-quoted string
    IN_STRING_DOUBLE, // Reading double-quoted string
    IN_OPERATOR    // Reading operator
}

/**
 * Parse a SQL query string into a list of tokens.
 *
 * Supports SQL syntax including:
 * - Keywords: SELECT, FROM, WHERE, AND, OR, NOT, LIKE
 * - Identifiers
 * - String literals
 * - Operators: =, !=, <, >, <=, >=, *
 * - Punctuation: , ( )
 *
 * @param text The SQL query string to parse
 * @return List of tokens with their types and positions
 */
fun parseSqlQuery(text: String): List<SqlToken> {
    if (text.isBlank()) return emptyList()

    val tokens = mutableListOf<SqlToken>()
    var state = SqlParseState.NEUTRAL
    var currentTokenStart = 0
    val currentText = StringBuilder()

    fun addToken(type: SqlTokenType, tokenText: String, index: Int) {
        if (tokenText.isNotEmpty()) {
            tokens.add(SqlToken(type, tokenText, index, index + tokenText.length))
        }
    }

    fun addCurrentToken(type: SqlTokenType) {
        if (currentText.isNotEmpty()) {
            addToken(type, currentText.toString(), currentTokenStart)
            currentText.clear()
        }
    }

    fun isKeyword(text: String): Boolean {
        return when (text.uppercase()) {
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "LIKE" -> true
            else -> false
        }
    }

    text.forEachIndexed { index, char ->
        when (state) {
            SqlParseState.NEUTRAL -> {
                when {
                    char.isWhitespace() -> {
                        // Skip whitespace
                    }
                    char == '\'' -> {
                        state = SqlParseState.IN_STRING_SINGLE
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    char == '"' -> {
                        state = SqlParseState.IN_STRING_DOUBLE
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    char.isLetter() || char == '_' || char == '.' -> {
                        state = SqlParseState.IN_IDENTIFIER
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    char in charArrayOf('=', '!', '<', '>', '*') -> {
                        state = SqlParseState.IN_OPERATOR
                        currentTokenStart = index
                        currentText.append(char)
                    }
                    char in charArrayOf(',', '(', ')') -> {
                        addToken(SqlTokenType.Punctuation, char.toString(), index)
                    }
                    else -> {
                        addToken(SqlTokenType.Invalid, char.toString(), index)
                    }
                }
            }

            SqlParseState.IN_IDENTIFIER -> {
                if (char.isLetterOrDigit() || char == '_' || char == '.' || char == '-') {
                    currentText.append(char)
                } else {
                    // End of identifier
                    val tokenText = currentText.toString()
                    val type = if (isKeyword(tokenText)) SqlTokenType.Keyword else SqlTokenType.Identifier
                    addCurrentToken(type)
                    
                    // Re-process current char in NEUTRAL state
                    state = SqlParseState.NEUTRAL
                    when {
                        char.isWhitespace() -> {} // Just end identifier
                        char == '\'' -> {
                            state = SqlParseState.IN_STRING_SINGLE
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char == '"' -> {
                            state = SqlParseState.IN_STRING_DOUBLE
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char.isLetter() || char == '_' || char == '.' -> {
                            state = SqlParseState.IN_IDENTIFIER
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char in charArrayOf('=', '!', '<', '>', '*') -> {
                            state = SqlParseState.IN_OPERATOR
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char in charArrayOf(',', '(', ')') -> {
                            addToken(SqlTokenType.Punctuation, char.toString(), index)
                        }
                        else -> {
                             addToken(SqlTokenType.Invalid, char.toString(), index)
                        }
                    }
                }
            }

            SqlParseState.IN_STRING_SINGLE -> {
                currentText.append(char)
                if (char == '\'') {
                    addCurrentToken(SqlTokenType.String)
                    state = SqlParseState.NEUTRAL
                }
            }

            SqlParseState.IN_STRING_DOUBLE -> {
                currentText.append(char)
                if (char == '"') {
                    addCurrentToken(SqlTokenType.String)
                    state = SqlParseState.NEUTRAL
                }
            }

            SqlParseState.IN_OPERATOR -> {
                if (char == '=' && (currentText.toString() == "!" || currentText.toString() == "<" || currentText.toString() == ">")) {
                    currentText.append(char)
                    addCurrentToken(SqlTokenType.Operator)
                    state = SqlParseState.NEUTRAL
                } else {
                    // Previous operator ended
                    addCurrentToken(SqlTokenType.Operator)
                    state = SqlParseState.NEUTRAL
                    
                    // Handle current char
                    when {
                        char.isWhitespace() -> {}
                        char == '\'' -> {
                            state = SqlParseState.IN_STRING_SINGLE
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char == '"' -> {
                            state = SqlParseState.IN_STRING_DOUBLE
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char.isLetter() || char == '_' || char == '.' -> {
                            state = SqlParseState.IN_IDENTIFIER
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char in charArrayOf('=', '!', '<', '>', '*') -> {
                            state = SqlParseState.IN_OPERATOR
                            currentTokenStart = index
                            currentText.append(char)
                        }
                        char in charArrayOf(',', '(', ')') -> {
                            addToken(SqlTokenType.Punctuation, char.toString(), index)
                        }
                        else -> {
                            addToken(SqlTokenType.Invalid, char.toString(), index)
                        }
                    }
                }
            }
        }
    }

    // Handle any remaining token
    when (state) {
        SqlParseState.IN_IDENTIFIER -> {
            val tokenText = currentText.toString()
            val type = if (isKeyword(tokenText)) SqlTokenType.Keyword else SqlTokenType.Identifier
            addCurrentToken(type)
        }
        SqlParseState.IN_OPERATOR -> addCurrentToken(SqlTokenType.Operator)
        SqlParseState.IN_STRING_SINGLE, SqlParseState.IN_STRING_DOUBLE -> addCurrentToken(SqlTokenType.String) // Unterminated string
        SqlParseState.NEUTRAL -> {}
    }

    return tokens
}

/**
 * Syntax highlighting layer for SQL queries.
 *
 * @param text The SQL query text to highlight
 * @param colorScheme Map of token types to colors for syntax highlighting
 */
class SqlSyntaxHighlightLayer(
    private val text: String,
    private val colorScheme: Map<SqlTokenType, Color>
) : HighlightLayer(zIndex = 0) {
    /**
     * Parse the text and convert tokens to line segments.
     * Computed lazily and cached.
     */
    override val segments: List<LineSegment> by lazy {
        // Skip highlighting for very large texts (performance optimization)
        if (text.length > 10000) {
            emptyList()
        } else {
            parseSqlQuery(text).map { token ->
                LineSegment(
                    startIndex = token.startIndex,
                    endIndex = token.endIndex,
                    color = colorScheme[token.type] ?: colorScheme[SqlTokenType.Whitespace]!!
                )
            }
        }
    }
}
