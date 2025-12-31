package com.moribito.ldap.query

/**
 * Converts a [SqlQuery] into LDAP search parameters.
 */
class LdapQueryConverter {

    fun convertToLdapFilter(node: WhereNode?): String {
        if (node == null) return "(objectClass=*)"
        
        return when (node) {
            is WhereNode.Comparison -> {
                val value = if (node.operator == ComparisonOperator.LIKE) {
                    node.value.replace("%", "*").replace("_", "?")
                } else {
                    node.value
                }
                
                when (node.operator) {
                    ComparisonOperator.EQUALS -> "(${node.field}=$value)"
                    ComparisonOperator.NOT_EQUALS -> "(!(${node.field}=$value))"
                    ComparisonOperator.GREATER_EQUALS -> "(${node.field}>=$value)"
                    ComparisonOperator.LESS_EQUALS -> "(${node.field}<=$value)"
                    ComparisonOperator.GREATER_THAN -> "(${node.field}>=$value)" // LDAP doesn't support >
                    ComparisonOperator.LESS_THAN -> "(${node.field}<=$value)" // LDAP doesn't support <
                    ComparisonOperator.LIKE -> "(${node.field}=$value)"
                }
            }
            is WhereNode.Binary -> {
                val left = convertToLdapFilter(node.left)
                val right = convertToLdapFilter(node.right)
                val op = when (node.operator) {
                    LogicalOperator.AND -> "&"
                    LogicalOperator.OR -> "|"
                }
                "($op$left$right)"
            }
            is WhereNode.Not -> {
                "(!${convertToLdapFilter(node.node)})"
            }
        }
    }

    /**
     * Converts a dot-separated "table" name to a DN.
     * e.g., "ou.people" -> "ou=people"
     * This is a heuristic and might need to be more sophisticated.
     * If the 'from' already looks like a DN (contains =), it returns it as is.
     */
    fun convertFromToDn(from: String, baseDn: String): String {
        if (from == "*" || from.lowercase() == "root") return baseDn
        if (from.contains("=")) {
            return if (from.endsWith(baseDn)) from else if (baseDn.isEmpty()) from else "$from,$baseDn"
        }
        
        val components = from.split(".")
        val dn = if (components.size >= 2 && components.size % 2 == 0) {
            // Group by 2: ["ou", "people", "dc", "example"] -> "ou=people,dc=example"
            components.chunked(2).joinToString(",") { "${it[0]}=${it[1]}" }
        } else {
            // Fallback for odd number or single component
            components.joinToString(",") { "ou=$it" }
        }
        
        return if (baseDn.isEmpty()) dn else "$dn,$baseDn"
    }
}
