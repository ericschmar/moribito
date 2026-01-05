package com.moribito.ldap

/**
 * Common interface for LDAP clients (real and mock implementations).
 * This allows dependency injection and easier testing.
 */
interface ILdapClient : AutoCloseable {
    /**
     * Establishes connection to the LDAP server.
     * @throws LdapException if connection fails
     */
    suspend fun connect()

    /**
     * Checks if the client is currently connected.
     */
    fun isConnected(): Boolean

    /**
     * Performs an LDAP search operation.
     */
    suspend fun search(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String> = emptyList()
    ): List<Entry>

    /**
     * Performs a paginated LDAP search operation.
     */
    suspend fun searchPaged(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String> = emptyList(),
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage

    /**
     * Gets immediate children of a DN.
     */
    suspend fun getChildren(dn: String = ""): List<TreeNode>

    /**
     * Retrieves a specific LDAP entry with all its attributes.
     */
    suspend fun getEntry(dn: String): Entry

    /**
     * Builds the root tree node for the LDAP directory.
     */
    suspend fun buildTree(): TreeNode

    /**
     * Loads children for a tree node, including virtual member children if enabled.
     */
    suspend fun loadChildrenWithMembers(node: TreeNode, includeVirtualMembers: Boolean): TreeNode

    /**
     * Performs a custom LDAP search with user-provided filter.
     */
    suspend fun customSearch(filter: String): List<Entry>

    /**
     * Performs a paginated custom LDAP search.
     */
    suspend fun customSearchPaged(
        filter: String,
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage

    /**
     * Executes a SQL-like query.
     */
    suspend fun executeSqlQuery(sql: String): List<Entry>

    /**
     * Executes a SQL-like query with pagination.
     */
    suspend fun executeSqlQueryPaged(
        sql: String,
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage

    /**
     * Inspects the LDAP server schema and recursively discovers the container tree structure.
     */
    suspend fun inspectSchema(): LdapSchema

    /**
     * Queries an OU (or any entry) for the attributes present in it and its immediate children.
     */
    suspend fun getAttributesInOu(ouDn: String): LdapSchema
}
