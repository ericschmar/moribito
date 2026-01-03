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
     * Converts a dot-separated "table" name or DN to a proper base DN.
     * 
     * Examples:
     * - "ou.people" -> "ou=people,<baseDn>"
     * - "dc=example" (when baseDn is "dc=example,dc=com") -> "dc=example,dc=com"
     * - "ou=scientists,dc=example,dc=com" -> "ou=scientists,dc=example,dc=com"
     * - "*" or "root" -> baseDn
     */
    fun convertFromToDn(from: String, baseDn: String): String {
        // Special cases: * or root means search from baseDN
        if (from == "*" || from.lowercase() == "root") return baseDn
        
        // If it contains '=', it's already in DN format
        if (from.contains("=")) {
            // If it already ends with the baseDN, it's a complete DN
            if (from.endsWith(baseDn)) {
                return from
            }
            
            // If the baseDN equals the from clause or ends with ",$from",
            // the user is specifying a component that's already in baseDN
            if (baseDn == from || baseDn.contains(from)) {
                return baseDn
            }
            
            // Otherwise, append to baseDN (if baseDN is not empty)
            return if (baseDn.isEmpty()) from else "$from,$baseDn"
        }
        
        // Handle dot-separated notation (e.g., "ou.people" or "ou.scientists.dc.example")
        val components = from.split(".")
        val dn = if (components.size >= 2 && components.size % 2 == 0) {
            // Group by 2: ["ou", "people", "dc", "example"] -> "ou=people,dc=example"
            components.chunked(2).joinToString(",") { "${it[0]}=${it[1]}" }
        } else {
            // Fallback for odd number or single component: assume it's an OU
            components.joinToString(",") { "ou=$it" }
        }
        
        return if (baseDn.isEmpty()) dn else "$dn,$baseDn"
    }
}
