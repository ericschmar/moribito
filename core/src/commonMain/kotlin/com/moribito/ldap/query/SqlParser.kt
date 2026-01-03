package com.moribito.ldap.query

import com.moribito.logging.Logger

class SqlParserException(message: String) : Exception(message)

/**
 * A simple tokenizer and recursive descent parser for the SQL-like LDAP query language.
 */
class SqlParser(private val input: String) {
    private var pos = 0
    private var tokens = mutableListOf<Token>()
    private var currentTokenIdx = 0
    private val logger = Logger.get("SqlParser")

    enum class TokenType {
        SELECT, FROM, WHERE, AND, OR, NOT,
        ASTERISK, COMMA, EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUALS, LESS_THAN, LESS_EQUALS, LIKE,
        LPAREN, RPAREN,
        IDENTIFIER, STRING, EOF
    }

    data class Token(val type: TokenType, val value: String, val start: Int)

    private fun tokenize() {
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val startPos = i
            when {
                c.isWhitespace() -> i++
                c == '*' -> { tokens.add(Token(TokenType.ASTERISK, "*", startPos)); i++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",", startPos)); i++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", startPos)); i++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", startPos)); i++ }
                c == '=' -> { tokens.add(Token(TokenType.EQUALS, "=", startPos)); i++ }
                c == '!' && i + 1 < input.length && input[i + 1] == '=' -> {
                    tokens.add(Token(TokenType.NOT_EQUALS, "!=", startPos)); i += 2
                }
                c == '>' -> {
                    if (i + 1 < input.length && input[i + 1] == '=') {
                        tokens.add(Token(TokenType.GREATER_EQUALS, ">=", startPos)); i += 2
                    } else {
                        tokens.add(Token(TokenType.GREATER_THAN, ">", startPos)); i++
                    }
                }
                c == '<' -> {
                    if (i + 1 < input.length && input[i + 1] == '=') {
                        tokens.add(Token(TokenType.LESS_EQUALS, "<=", startPos)); i += 2
                    } else {
                        tokens.add(Token(TokenType.LESS_THAN, "<", startPos)); i++
                    }
                }
                c == '"' || c == '\'' -> {
                    val quote = c
                    i++
                    val stringStart = i
                    while (i < input.length && input[i] != quote) i++
                    if (i == input.length) throw SqlParserException("Unterminated string")
                    tokens.add(Token(TokenType.STRING, input.substring(stringStart, i), startPos))
                    i++
                }
                c.isLetter() || c.isDigit() || c == '_' || c == '.' || c == '-' -> {
                    // Note: . and - are allowed in identifiers for DNs and LDAP attributes
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] in "._-")) i++
                    val value = input.substring(startPos, i)
                    when (value.uppercase()) {
                        "SELECT" -> tokens.add(Token(TokenType.SELECT, value, startPos))
                        "FROM" -> tokens.add(Token(TokenType.FROM, value, startPos))
                        "WHERE" -> tokens.add(Token(TokenType.WHERE, value, startPos))
                        "AND" -> tokens.add(Token(TokenType.AND, value, startPos))
                        "OR" -> tokens.add(Token(TokenType.OR, value, startPos))
                        "NOT" -> tokens.add(Token(TokenType.NOT, value, startPos))
                        "LIKE" -> tokens.add(Token(TokenType.LIKE, value, startPos))
                        else -> tokens.add(Token(TokenType.IDENTIFIER, value, startPos))
                    }
                }
                else -> throw SqlParserException("Unexpected character: $c")
            }
        }
        tokens.add(Token(TokenType.EOF, "", input.length))
    }

    fun parse(): SqlQuery {
        tokenize()
        expect(TokenType.SELECT)
        val attributes = parseAttributes()
        expect(TokenType.FROM)
        val from = parseFromClause()

        var where: WhereNode? = null
        if (match(TokenType.WHERE)) {
            where = parseExpression()
        }

        expect(TokenType.EOF)
        return SqlQuery(attributes, from, where)
    }

    private fun parseFromClause(): String {
        // FROM clause can be:
        // - A simple identifier: "root", "*", "ou.scientists"
        // - A quoted string: "dc=example,dc=com"
        // - A DN: dc=example,dc=com (now tokenized as separate tokens)

        val token = peek()
        if (token.type == TokenType.STRING) {
            return nextToken().value
        }

        // Reconstruct DN from tokens until WHERE or EOF
        val parts = mutableListOf<String>()
        while (true) {
            val current = peek()
            when (current.type) {
                TokenType.WHERE, TokenType.EOF -> break
                TokenType.IDENTIFIER -> parts.add(nextToken().value)
                TokenType.EQUALS -> parts.add(nextToken().value)
                TokenType.COMMA -> parts.add(nextToken().value)
                TokenType.ASTERISK -> parts.add(nextToken().value)
                else -> throw SqlParserException("Unexpected token in FROM clause: ${current.type}")
            }
        }

        return parts.joinToString("")
    }

    /**
     * Finds the SQL context at the given cursor position.
     * Useful for autocomplete.
     */
    fun findContextAt(cursorPosition: Int): Pair<QueryContext, String> {
        try {
            tokenize()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Tokenize error: ${e.message}")
        }

        var currentContext = QueryContext.NONE
        
        //println("[DEBUG_LOG] findContextAt cursor=$cursorPosition, tokens=${tokens.size}")
        for (token in tokens) {
            //println("[DEBUG_LOG] Token: ${token.type} at ${token.start}")
            // If the token starts AFTER the cursor, we stop and use the context from previous tokens
            if (token.start > cursorPosition) break
            
            when (token.type) {
                TokenType.SELECT -> currentContext = QueryContext.SQL_SELECT
                TokenType.FROM -> currentContext = QueryContext.SQL_FROM
                TokenType.WHERE -> currentContext = QueryContext.SQL_WHERE
                else -> {}
            }
            
            if (token.start == cursorPosition && token.type != TokenType.EOF) break
        }

        // Determine the query (word being typed at cursor)
        val sub = if (cursorPosition <= input.length) input.substring(0, cursorPosition) else input
        val separators = charArrayOf(' ', '\n', '\t', '\r', '(', ')', '&', '|', '=', ',', '<', '>', '.')
        val lastSeparator = sub.lastIndexOfAny(separators)
        val query = if (lastSeparator == -1) sub.trim() else sub.substring(lastSeparator + 1).trim()

        return currentContext to query
    }

    private fun parseAttributes(): List<String> {
        if (match(TokenType.ASTERISK)) return listOf("*")
        val attrs = mutableListOf<String>()
        attrs.add(expect(TokenType.IDENTIFIER).value)
        while (match(TokenType.COMMA)) {
            attrs.add(expect(TokenType.IDENTIFIER).value)
        }
        return attrs
    }

    private fun parseExpression(): WhereNode {
        return parseOr()
    }

    private fun parseOr(): WhereNode {
        var node = parseAnd()
        while (match(TokenType.OR)) {
            node = WhereNode.Binary(node, LogicalOperator.OR, parseAnd())
        }
        return node
    }

    private fun parseAnd(): WhereNode {
        var node = parsePrimary()
        while (match(TokenType.AND)) {
            node = WhereNode.Binary(node, LogicalOperator.AND, parsePrimary())
        }
        return node
    }

    private fun parsePrimary(): WhereNode {
        return when {
            match(TokenType.NOT) -> WhereNode.Not(parsePrimary())
            match(TokenType.LPAREN) -> {
                val node = parseExpression()
                expect(TokenType.RPAREN)
                node
            }
            else -> parseComparison()
        }
    }

    private fun parseComparison(): WhereNode {
        val field = expect(TokenType.IDENTIFIER).value
        val opToken = nextToken()
        val operator = when (opToken.type) {
            TokenType.EQUALS -> ComparisonOperator.EQUALS
            TokenType.NOT_EQUALS -> ComparisonOperator.NOT_EQUALS
            TokenType.GREATER_THAN -> ComparisonOperator.GREATER_THAN
            TokenType.GREATER_EQUALS -> ComparisonOperator.GREATER_EQUALS
            TokenType.LESS_THAN -> ComparisonOperator.LESS_THAN
            TokenType.LESS_EQUALS -> ComparisonOperator.LESS_EQUALS
            TokenType.LIKE -> ComparisonOperator.LIKE
            else -> throw SqlParserException("Expected comparison operator, got ${opToken.type}")
        }
        val valueToken = expect(TokenType.IDENTIFIER, TokenType.STRING, TokenType.ASTERISK)
        return WhereNode.Comparison(field, operator, valueToken.value)
    }

    private fun match(type: TokenType): Boolean {
        if (peek().type == type) {
            currentTokenIdx++
            return true
        }
        return false
    }

    private fun expect(vararg types: TokenType): Token {
        val token = nextToken()
        if (token.type !in types) {
            throw SqlParserException("Expected ${types.joinToString(" or ")}, got ${token.type} (${token.value})")
        }
        return token
    }

    private fun nextToken(): Token {
        if (currentTokenIdx >= tokens.size) return Token(TokenType.EOF, "", input.length)
        return tokens[currentTokenIdx++]
    }

    private fun peek(): Token {
        if (currentTokenIdx >= tokens.size) return Token(TokenType.EOF, "", input.length)
        return tokens[currentTokenIdx]
    }
}
