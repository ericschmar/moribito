package com.moribito.ldap.query

class SqlParserException(message: String) : Exception(message)

/**
 * A simple tokenizer and recursive descent parser for the SQL-like LDAP query language.
 */
class SqlParser(private val input: String) {
    private var pos = 0
    private var tokens = mutableListOf<Token>()
    private var currentTokenIdx = 0

    enum class TokenType {
        SELECT, FROM, WHERE, AND, OR, NOT,
        ASTERISK, COMMA, EQUALS, NOT_EQUALS, GREATER_THAN, GREATER_EQUALS, LESS_THAN, LESS_EQUALS, LIKE,
        LPAREN, RPAREN,
        IDENTIFIER, STRING, EOF
    }

    data class Token(val type: TokenType, val value: String)

    private fun tokenize() {
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c == '*' -> { tokens.add(Token(TokenType.ASTERISK, "*")); i++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",")); i++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(")); i++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")")); i++ }
                c == '=' -> { tokens.add(Token(TokenType.EQUALS, "=")); i++ }
                c == '!' && i + 1 < input.length && input[i + 1] == '=' -> {
                    tokens.add(Token(TokenType.NOT_EQUALS, "!=")); i += 2
                }
                c == '>' -> {
                    if (i + 1 < input.length && input[i + 1] == '=') {
                        tokens.add(Token(TokenType.GREATER_EQUALS, ">=")); i += 2
                    } else {
                        tokens.add(Token(TokenType.GREATER_THAN, ">")); i++
                    }
                }
                c == '<' -> {
                    if (i + 1 < input.length && input[i + 1] == '=') {
                        tokens.add(Token(TokenType.LESS_EQUALS, "<=")); i += 2
                    } else {
                        tokens.add(Token(TokenType.LESS_THAN, "<")); i++
                    }
                }
                c == '"' || c == '\'' -> {
                    val quote = c
                    val start = ++i
                    while (i < input.length && input[i] != quote) i++
                    if (i == input.length) throw SqlParserException("Unterminated string")
                    tokens.add(Token(TokenType.STRING, input.substring(start, i)))
                    i++
                }
                c.isLetter() || c.isDigit() || c == '_' || c == '.' || c == '-' || c == '=' -> {
                    // Note: . and = and - are allowed in identifiers for DNs and LDAP attributes
                    val start = i
                    while (i < input.length && (input[i].isLetterOrDigit() || input[i] in "._-=")) i++
                    val value = input.substring(start, i)
                    when (value.uppercase()) {
                        "SELECT" -> tokens.add(Token(TokenType.SELECT, value))
                        "FROM" -> tokens.add(Token(TokenType.FROM, value))
                        "WHERE" -> tokens.add(Token(TokenType.WHERE, value))
                        "AND" -> tokens.add(Token(TokenType.AND, value))
                        "OR" -> tokens.add(Token(TokenType.OR, value))
                        "NOT" -> tokens.add(Token(TokenType.NOT, value))
                        "LIKE" -> tokens.add(Token(TokenType.LIKE, value))
                        else -> tokens.add(Token(TokenType.IDENTIFIER, value))
                    }
                }
                else -> throw SqlParserException("Unexpected character: $c")
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
    }

    fun parse(): SqlQuery {
        tokenize()
        expect(TokenType.SELECT)
        val attributes = parseAttributes()
        expect(TokenType.FROM)
        val from = expect(TokenType.IDENTIFIER, TokenType.STRING).value
        
        var where: WhereNode? = null
        if (match(TokenType.WHERE)) {
            where = parseExpression()
        }
        
        expect(TokenType.EOF)
        return SqlQuery(attributes, from, where)
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
        val valueToken = expect(TokenType.IDENTIFIER, TokenType.STRING)
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
        if (currentTokenIdx >= tokens.size) return Token(TokenType.EOF, "")
        return tokens[currentTokenIdx++]
    }

    private fun peek(): Token {
        if (currentTokenIdx >= tokens.size) return Token(TokenType.EOF, "")
        return tokens[currentTokenIdx]
    }
}
