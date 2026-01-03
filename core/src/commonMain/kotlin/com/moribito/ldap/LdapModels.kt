package com.moribito.ldap

/**
 * Represents an LDAP entry with its distinguished name and attributes.
 */
data class Entry(
    val dn: String,
    val attributes: Map<String, List<String>>
) {
    /**
     * Gets the first value of an attribute, or null if not present.
     */
    fun getAttributeValue(name: String): String? {
        return attributes[name]?.firstOrNull()
    }

    /**
     * Gets all values of an attribute, or empty list if not present.
     */
    fun getAttributeValues(name: String): List<String> {
        return attributes[name] ?: emptyList()
    }

    /**
     * Checks if an attribute exists.
     */
    fun hasAttribute(name: String): Boolean {
        return attributes.containsKey(name)
    }
}

/**
 * Represents a page of search results with pagination information.
 */
data class SearchPage(
    val entries: List<Entry>,
    val hasMore: Boolean,
    val cookie: ByteArray?,
    val pageSize: Int,
    val totalCount: Int = -1 // -1 if unknown
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SearchPage

        if (entries != other.entries) return false
        if (hasMore != other.hasMore) return false
        if (cookie != null) {
            if (other.cookie == null) return false
            if (!cookie.contentEquals(other.cookie)) return false
        } else if (other.cookie != null) return false
        if (pageSize != other.pageSize) return false
        if (totalCount != other.totalCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entries.hashCode()
        result = 31 * result + hasMore.hashCode()
        result = 31 * result + (cookie?.contentHashCode() ?: 0)
        result = 31 * result + pageSize
        result = 31 * result + totalCount
        return result
    }
}

/**
 * Converts an LDAP Entry to a TreeNode for tree navigation.
 */
fun Entry.toTreeNode(): TreeNode {
    // Extract display name from DN (first component)
    val name = dn.split(",").firstOrNull()?.trim() ?: dn

    return TreeNode(
        dn = this.dn,
        name = name,
        children = null,     // Not loaded yet
        isLoaded = false    // Children load on expand
    )
}

/**
 * Represents a node in the LDAP directory tree.
 */
data class TreeNode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val dn: String,
    val name: String,
    val children: List<TreeNode>? = null,
    val isLoaded: Boolean = false,
    val isVirtualMember: Boolean = false
) {
    /**
     * Creates a copy of this node with loaded children.
     */
    fun withChildren(children: List<TreeNode>): TreeNode {
        return copy(children = children, isLoaded = true)
    }

    /**
     * Checks if this node has children (loaded or potentially loadable).
     * Returns true if:
     * - Children are loaded and not empty, OR
     * - Children haven't been loaded yet AND the DN suggests it's a container
     *
     * Leaf node types (assumed to have no children):
     * - uid= (user accounts)
     * - cn= (common names, typically leaf entries unless organizational)
     *
     * Container node types (assumed to have children):
     * - dc= (domain components)
     * - ou= (organizational units)
     * - o= (organizations)
     * - c= (countries)
     */
    fun hasChildren(): Boolean {
        return if (isLoaded) {
            children != null && children.isNotEmpty()
        } else {
            // Check if this is likely a container based on the RDN
            val rdn = name.lowercase()
            when {
                rdn.startsWith("uid=") -> false // User accounts are leaf nodes
                rdn.startsWith("cn=") -> false  // Common names are typically leaf nodes
                rdn.startsWith("dc=") -> true   // Domain components are containers
                rdn.startsWith("ou=") -> true   // Organizational units are containers
                rdn.startsWith("o=") -> true    // Organizations are containers
                rdn.startsWith("c=") -> true    // Countries are containers
                else -> true // Default to assuming it might have children
            }
        }
    }
}

/**
 * LDAP search scope options.
 */
enum class SearchScope {
    /** Search only the base entry */
    BASE,

    /** Search one level below the base entry */
    ONE_LEVEL,

    /** Search the entire subtree */
    SUBTREE;

    /**
     * Converts to the integer value used by LDAP libraries.
     */
    fun toInt(): Int = when (this) {
        BASE -> 0
        ONE_LEVEL -> 1
        SUBTREE -> 2
    }
}

/**
 * Represents an attribute type in the LDAP schema.
 */
data class LdapAttribute(
    val name: String,
    val type: String,
    val description: String? = null
)

/**
 * Represents the LDAP server schema or an OU-specific attribute list.
 */
data class LdapSchema(
    val attributes: List<LdapAttribute>,
    val containerDns: List<String> = emptyList(),
    val isFromSchemaInspection: Boolean = true
)

/**
 * Exception thrown when LDAP operations fail.
 */
class LdapException(
    message: String,
    cause: Throwable? = null,
    val resultCode: Int? = null,
    val isRetryable: Boolean = false
) : Exception(message, cause) {
    companion object {
        // Common LDAP result codes
        const val RESULT_SUCCESS = 0
        const val RESULT_OPERATIONS_ERROR = 1
        const val RESULT_PROTOCOL_ERROR = 2
        const val RESULT_TIME_LIMIT_EXCEEDED = 3
        const val RESULT_SIZE_LIMIT_EXCEEDED = 4
        const val RESULT_AUTH_METHOD_NOT_SUPPORTED = 7
        const val RESULT_STRONG_AUTH_REQUIRED = 8
        const val RESULT_NO_SUCH_OBJECT = 32
        const val RESULT_INVALID_CREDENTIALS = 49
        const val RESULT_INSUFFICIENT_ACCESS_RIGHTS = 50
        const val RESULT_BUSY = 51
        const val RESULT_UNAVAILABLE = 52
        const val RESULT_UNWILLING_TO_PERFORM = 53
        const val RESULT_OTHER = 80
        const val RESULT_SERVER_DOWN = 81
        const val RESULT_CONNECT_ERROR = 91

        /**
         * Checks if a result code indicates a retryable error.
         */
        fun isRetryableResultCode(resultCode: Int): Boolean {
            return when (resultCode) {
                RESULT_BUSY,
                RESULT_UNAVAILABLE,
                RESULT_UNWILLING_TO_PERFORM,
                RESULT_SERVER_DOWN,
                RESULT_CONNECT_ERROR -> true

                else -> false
            }
        }
    }
}
