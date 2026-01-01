package com.moribito.ldap

import org.ldaptive.*
import org.ldaptive.ssl.SslConfig
import org.ldaptive.ssl.X509CredentialConfig
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds
import com.moribito.ldap.query.SqlParser
import com.moribito.ldap.query.LdapQueryConverter

/**
 * Configuration for LDAP client connection and retry behavior.
 * Note: Credentials are now passed separately via BindCredential parameter.
 */
data class LdapConfig(
    val host: String,
    val port: Int,
    val baseDN: String,
    val useSSL: Boolean = false,
    val useTLS: Boolean = false,
    val retryEnabled: Boolean = true,
    val maxRetries: Int = 3,
    val initialDelayMs: Int = 500,
    val maxDelayMs: Int = 5000,
    val connectionTimeoutMs: Int = 30000
)

/**
 * LDAP client with support for SSL/TLS, retry logic, and pagination.
 *
 * This client wraps the Ldaptive library and provides:
 * - Automatic retry with exponential backoff for connection errors
 * - SSL/TLS connection support
 * - Paginated search results
 * - Lazy tree loading for directory browsing
 *
 * @param config Connection configuration (host, port, SSL/TLS settings)
 * @param credential Bind credentials (DN and password) - if null, anonymous bind is used
 */
class LdapClient(
    private val config: LdapConfig,
    private val credential: com.moribito.config.BindCredential? = null
) : AutoCloseable {
    private var connectionFactory: DefaultConnectionFactory? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Establishes connection to the LDAP server.
     *
     * @throws LdapException if connection fails
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            val connectionConfig = ConnectionConfig.builder()
                .url("ldap${if (config.useSSL) "s" else ""}://${config.host}:${config.port}")
                .connectTimeout(java.time.Duration.ofMillis(config.connectionTimeoutMs.toLong()))
                .responseTimeout(java.time.Duration.ofMillis(config.connectionTimeoutMs.toLong()))
                .apply {
                    if (config.useSSL || config.useTLS) {
                        sslConfig(
                            SslConfig.builder()
                                .trustManagers(org.ldaptive.ssl.AllowAnyTrustManager())
                                .build()
                        )
                    }
                    if (config.useTLS && !config.useSSL) {
                        useStartTLS(true)
                    }
                }
                .build()

            val factory = DefaultConnectionFactory(connectionConfig)

            // Test the connection and bind if credentials provided
            factory.connection.use { conn ->
                conn.open()

                if (credential != null && credential.bindUser.isNotEmpty()) {
                    val bindOp = BindOperation(factory)
                    val bindRequest = SimpleBindRequest(credential.bindUser, credential.bindPass)
                    val result = bindOp.execute(bindRequest)

                    if (!result.isSuccess) {
                        throw LdapException(
                            message = "Failed to bind: ${result.diagnosticMessage}",
                            resultCode = result.resultCode.value(),
                            isRetryable = false
                        )
                    }
                }
            }

            connectionFactory = factory
        } catch (e: LdapException) {
            throw e
        } catch (e: org.ldaptive.LdapException) {
            throw LdapException(
                message = "Failed to connect to LDAP server: ${e.message}",
                cause = e,
                resultCode = e.resultCode?.value(),
                isRetryable = isRetryableError(e)
            )
        } catch (e: Exception) {
            throw LdapException(
                message = "Failed to connect to LDAP server: ${e.message}",
                cause = e,
                isRetryable = false
            )
        }
    }

    /**
     * Checks if an error is retryable (connection-related).
     */
    private fun isRetryableError(error: Throwable): Boolean {
        return when (error) {
            is org.ldaptive.LdapException -> {
                error.resultCode?.let { rc ->
                    when (rc) {
                        ResultCode.SERVER_DOWN,
                        ResultCode.CONNECT_ERROR,
                        ResultCode.UNAVAILABLE,
                        ResultCode.BUSY,
                        ResultCode.UNWILLING_TO_PERFORM -> true

                        else -> false
                    }
                } ?: false
            }

            is java.net.SocketException,
            is java.net.ConnectException,
            is java.io.IOException -> true

            else -> {
                val message = error.message?.lowercase() ?: ""
                message.contains("connection closed") ||
                        message.contains("connection reset") ||
                        message.contains("broken pipe") ||
                        message.contains("connection refused") ||
                        message.contains("network is unreachable") ||
                        message.contains("timeout") ||
                        message.contains("server down")
            }
        }
    }

    /**
     * Reconnects to the LDAP server.
     */
    private suspend fun reconnect() = withContext(Dispatchers.IO) {
        connectionFactory = null
        connect()
    }

    /**
     * Executes an operation with retry logic and exponential backoff.
     */
    private suspend fun <T> withRetry(operation: suspend () -> T): T {
        if (!config.retryEnabled) {
            return operation()
        }

        var lastError: Throwable? = null
        var delay = config.initialDelayMs.milliseconds
        val maxDelay = config.maxDelayMs.milliseconds

        repeat(config.maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastError = e

                // Don't retry on last attempt or if error is not retryable
                if (attempt == config.maxRetries || !isRetryableError(e)) {
                    throw if (e is LdapException) e else LdapException(
                        message = "LDAP operation failed: ${e.message}",
                        cause = e,
                        isRetryable = isRetryableError(e)
                    )
                }

                // Try to reconnect for retryable errors
                try {
                    reconnect()
                } catch (reconnectError: Exception) {
                    // If reconnection fails, continue with the original error
                    throw if (e is LdapException) e else LdapException(
                        message = "LDAP operation failed and reconnection failed: ${e.message}",
                        cause = e,
                        isRetryable = false
                    )
                }

                // Wait before retrying with exponential backoff
                delay(delay)
                delay = (delay * 2).coerceAtMost(maxDelay)
            }
        }

        // This should never be reached due to the throw in the catch block
        throw LdapException(
            message = "LDAP operation failed after ${config.maxRetries} retries: ${lastError?.message}",
            cause = lastError,
            isRetryable = false
        )
    }

    /**
     * Performs an LDAP search operation.
     *
     * @param baseDN The base DN to search from
     * @param filter The LDAP search filter
     * @param scope The search scope
     * @param attributes The attributes to retrieve (empty list means all attributes)
     * @return List of matching entries
     * @throws LdapException if search fails
     */
    suspend fun search(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String> = emptyList()
    ): List<Entry> = withRetry {
        withContext(Dispatchers.IO) {
            val factory = connectionFactory ?: throw LdapException("Not connected to LDAP server")

            try {
                println("[LdapClient.search] BaseDN: $baseDN")
                println("[LdapClient.search] Filter: $filter")
                println("[LdapClient.search] Scope: $scope")
                println("[LdapClient.search] Attributes: $attributes")
                
                val searchOp = SearchOperation(factory)
                val searchRequest = SearchRequest.builder()
                    .dn(baseDN)
                    .filter(filter)
                    .scope(scope.toLdaptiveScope())
                    .apply {
                        if (attributes.isNotEmpty()) {
                            returnAttributes(*attributes.toTypedArray())
                        } else {
                            // Default to all user attributes if none specified
                            returnAttributes("*")
                        }
                    }
                    .build()

                val result = searchOp.execute(searchRequest)
                
                println("[LdapClient.search] Result success: ${result.isSuccess}")
                println("[LdapClient.search] Result code: ${result.resultCode}")
                println("[LdapClient.search] Entry count: ${result.entries.size}")
                println("[LdapClient.search] Diagnostic message: ${result.diagnosticMessage}")

                if (!result.isSuccess) {
                    throw LdapException(
                        message = "Search failed: ${result.diagnosticMessage}",
                        resultCode = result.resultCode.value(),
                        isRetryable = isRetryableError(result)
                    )
                }

                val entries = result.entries.map { it.toEntry() }
                println("[LdapClient.search] Converted ${entries.size} entries")
                entries.forEach { entry ->
                    println("[LdapClient.search]   Entry DN: ${entry.dn}")
                }
                
                entries
            } catch (e: LdapException) {
                println("[LdapClient.search] LdapException: ${e.message}")
                throw e
            } catch (e: org.ldaptive.LdapException) {
                println("[LdapClient.search] org.ldaptive.LdapException: ${e.message}")
                throw LdapException(
                    message = "Search failed: ${e.message}",
                    cause = e,
                    resultCode = e.resultCode?.value(),
                    isRetryable = isRetryableError(e)
                )
            }
        }
    }

    /**
     * Performs a paginated LDAP search operation.
     *
     * @param baseDN The base DN to search from
     * @param filter The LDAP search filter
     * @param scope The search scope
     * @param attributes The attributes to retrieve
     * @param pageSize The number of entries per page
     * @param cookie The pagination cookie from previous page (null for first page)
     * @return A SearchPage containing the results and pagination info
     * @throws LdapException if search fails
     */
    suspend fun searchPaged(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String> = emptyList(),
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage = withRetry {
        withContext(Dispatchers.IO) {
            val factory = connectionFactory ?: throw LdapException("Not connected to LDAP server")

            try {
                val searchOp = SearchOperation(factory)
                val pagedControl = org.ldaptive.control.PagedResultsControl(pageSize, cookie, true)

                val searchRequest = SearchRequest.builder()
                    .dn(baseDN)
                    .filter(filter)
                    .scope(scope.toLdaptiveScope())
                    .controls(pagedControl)
                    .apply {
                        if (attributes.isNotEmpty()) {
                            returnAttributes(*attributes.toTypedArray())
                        } else {
                            // Default to all user attributes if none specified
                            returnAttributes("*")
                        }
                    }
                    .build()

                val result = searchOp.execute(searchRequest)

                if (!result.isSuccess) {
                    throw LdapException(
                        message = "Paged search failed: ${result.diagnosticMessage}",
                        resultCode = result.resultCode.value(),
                        isRetryable = isRetryableError(result)
                    )
                }

                val entries = result.entries.map { it.toEntry() }

                // Extract pagination info from response
                val responseControl = result.getControl(org.ldaptive.control.PagedResultsControl.OID) as? org.ldaptive.control.PagedResultsControl
                val nextCookie = responseControl?.cookie
                val hasMore = nextCookie != null && nextCookie.isNotEmpty()

                SearchPage(
                    entries = entries,
                    hasMore = hasMore,
                    cookie = nextCookie,
                    pageSize = pageSize,
                    totalCount = -1 // LDAP doesn't provide total count
                )
            } catch (e: LdapException) {
                throw e
            } catch (e: org.ldaptive.LdapException) {
                throw LdapException(
                    message = "Paged search failed: ${e.message}",
                    cause = e,
                    resultCode = e.resultCode?.value(),
                    isRetryable = isRetryableError(e)
                )
            }
        }
    }

    /**
     * Gets immediate children of a DN.
     *
     * @param dn The parent DN (uses baseDN if empty)
     * @return List of child tree nodes
     * @throws LdapException if operation fails
     */
    suspend fun getChildren(dn: String = ""): List<TreeNode> {
        val searchDN = if (dn.isEmpty()) config.baseDN else dn

        println("[LdapClient.getChildren] Searching for children of: $searchDN")
        println("[LdapClient.getChildren] Filter: (objectClass=*)")
        println("[LdapClient.getChildren] Scope: ONE_LEVEL")

        val entries = search(
            baseDN = searchDN,
            filter = "(objectClass=*)",
            scope = SearchScope.ONE_LEVEL,
            attributes = listOf("dn")
        )

        println("[LdapClient.getChildren] Found ${entries.size} children")
        entries.forEach { entry ->
            println("[LdapClient.getChildren]   - ${entry.dn}")
        }

        return entries.map { entry ->
            TreeNode(
                dn = entry.dn,
                name = extractName(entry.dn, searchDN),
                children = null,
                isLoaded = false
            )
        }
    }

    /**
     * Retrieves a specific LDAP entry with all its attributes.
     *
     * @param dn The distinguished name of the entry
     * @return The entry with all attributes
     * @throws LdapException if entry not found or operation fails
     */
    suspend fun getEntry(dn: String): Entry {
        val entries = search(
            baseDN = dn,
            filter = "(objectClass=*)",
            scope = SearchScope.BASE,
            attributes = listOf("*", "+") // Request all user and operational attributes
        )

        return entries.firstOrNull()
            ?: throw LdapException(
                "Entry not found: $dn",
                resultCode = LdapException.RESULT_NO_SUCH_OBJECT
            )
    }

    /**
     * Builds the root tree node for the LDAP directory.
     *
     * @return The root tree node (not loaded)
     */
    suspend fun buildTree(): TreeNode {
        return TreeNode(
            dn = config.baseDN,
            name = extractName(config.baseDN, ""),
            children = null,
            isLoaded = false
        )
    }

    /**
     * Loads children for a tree node.
     *
     * @param node The node to load children for
     * @return The node with loaded children
     * @throws LdapException if operation fails
     */
    suspend fun loadChildren(node: TreeNode): TreeNode {
        if (node.isLoaded) {
            return node
        }

        val children = getChildren(node.dn)
        return node.withChildren(children)
    }

    /**
     * Loads children for a tree node, including virtual member children if enabled.
     *
     * @param node The node to load children for
     * @param includeVirtualMembers Whether to include group members as virtual children
     * @return The node with loaded children (hierarchical + virtual members if enabled)
     * @throws LdapException if operation fails
     */
    suspend fun loadChildrenWithMembers(node: TreeNode, includeVirtualMembers: Boolean): TreeNode {
        if (node.isLoaded) {
            return node
        }

        // Get hierarchical children
        val hierarchicalChildren = getChildren(node.dn).toMutableList()

        // If virtual members are enabled, add member references as virtual children
        if (includeVirtualMembers) {
            try {
                val entry = getEntry(node.dn)
                val memberDNs = mutableListOf<String>()

                // Check for various member attributes
                entry.attributes["member"]?.let { memberDNs.addAll(it) }
                entry.attributes["uniqueMember"]?.let { memberDNs.addAll(it) }
                entry.attributes["memberOf"]?.let { memberDNs.addAll(it) }

                // Create virtual TreeNode for each member
                println("[LdapClient] Processing ${memberDNs.size} member DNs for ${node.dn}")
                println("[LdapClient] Hierarchical children: ${hierarchicalChildren.map { it.dn }}")
                memberDNs.forEach { memberDN ->
                    // Check if this member is already in hierarchical children
                    val alreadyExists = hierarchicalChildren.any { it.dn.equals(memberDN, ignoreCase = true) }
                    println("[LdapClient] Checking member: $memberDN - alreadyExists: $alreadyExists")
                    if (!alreadyExists) {
                        hierarchicalChildren.add(
                            TreeNode(
                                dn = memberDN,
                                name = extractName(memberDN, node.dn),
                                children = null,
                                isLoaded = false,
                                isVirtualMember = true
                            )
                        )
                        println("[LdapClient] Added virtual member: $memberDN")
                    }
                }
            } catch (e: Exception) {
                // If we can't get members, just return hierarchical children
                println("[LdapClient] Could not load members for ${node.dn}: ${e.message}")
            }
        }

        return node.withChildren(hierarchicalChildren)
    }

    /**
     * Performs a custom LDAP search with user-provided filter.
     *
     * @param filter The LDAP search filter
     * @return List of matching entries
     * @throws LdapException if search fails
     */
    suspend fun customSearch(filter: String): List<Entry> {
        return search(
            baseDN = config.baseDN,
            filter = filter,
            scope = SearchScope.SUBTREE,
            attributes = listOf("*")
        )
    }

    /**
     * Performs a paginated custom LDAP search.
     *
     * @param filter The LDAP search filter
     * @param pageSize The number of entries per page
     * @param cookie The pagination cookie from previous page
     * @return A SearchPage containing the results and pagination info
     * @throws LdapException if search fails
     */
    suspend fun customSearchPaged(
        filter: String,
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage {
        return searchPaged(
            baseDN = config.baseDN,
            filter = filter,
            scope = SearchScope.SUBTREE,
            attributes = listOf("*"),
            pageSize = pageSize,
            cookie = cookie
        )
    }

    /**
     * Executes a SQL-like query.
     * Example: SELECT * FROM ou.people WHERE name = "john"
     */
    suspend fun executeSqlQuery(sql: String): List<Entry> {
        val parser = SqlParser(sql)
        val query = parser.parse()
        val converter = LdapQueryConverter()
        
        return search(
            baseDN = converter.convertFromToDn(query.from, config.baseDN),
            filter = converter.convertToLdapFilter(query.where),
            scope = SearchScope.SUBTREE,
            attributes = query.attributes
        )
    }

    /**
     * Executes a SQL-like query with pagination.
     */
    suspend fun executeSqlQueryPaged(
        sql: String,
        pageSize: Int = 50,
        cookie: ByteArray? = null
    ): SearchPage {
        val parser = SqlParser(sql)
        val query = parser.parse()
        val converter = LdapQueryConverter()
        
        return searchPaged(
            baseDN = converter.convertFromToDn(query.from, config.baseDN),
            filter = converter.convertToLdapFilter(query.where),
            scope = SearchScope.SUBTREE,
            attributes = query.attributes,
            pageSize = pageSize,
            cookie = cookie
        )
    }

    /**
     * Inspects the LDAP server schema.
     * Falls back to OU attribute discovery if schema inspection is not supported.
     */
    suspend fun inspectSchema(): LdapSchema = withRetry {
        withContext(Dispatchers.IO) {
            val factory = connectionFactory ?: throw LdapException("Not connected to LDAP server")

            try {
                // 1. Get Root DSE to find subschemaSubentry
                val rootDse = search("", "(objectClass=*)", SearchScope.BASE, listOf("subschemaSubentry")).firstOrNull()
                val subschemaSubentry = rootDse?.getAttributeValue("subschemaSubentry")

                if (subschemaSubentry != null) {
                    // 2. Query the schema entry
                    val schemaEntry = search(subschemaSubentry, "(objectClass=*)", SearchScope.BASE, listOf("attributeTypes")).firstOrNull()
                    val attributeTypes = schemaEntry?.getAttributeValues("attributeTypes")

                    if (attributeTypes != null && attributeTypes.isNotEmpty()) {
                        val attributes = attributeTypes.mapNotNull { parseAttributeType(it) }
                            .distinctBy { it.name }
                            .sortedBy { it.name }
                        return@withContext LdapSchema(attributes, true)
                    }
                }

                // Fallback: server doesn't support schema inspection
                return@withContext LdapSchema(emptyList(), false)
            } catch (e: Exception) {
                println("[LdapClient] Schema inspection failed: ${e.message}")
                return@withContext LdapSchema(emptyList(), false)
            }
        }
    }

    /**
     * Queries an OU (or any entry) for the attributes present in it and its immediate children.
     */
    suspend fun getAttributesInOu(ouDn: String): LdapSchema = withRetry {
        withContext(Dispatchers.IO) {
            // 1. Get attributes of the OU itself
            val selfEntry = try {
                getEntry(ouDn)
            } catch (e: Exception) {
                null
            }

            // 2. Get attributes of its children
            val childEntries = search(ouDn, "(objectClass=*)", SearchScope.ONE_LEVEL)

            val allEntries = if (selfEntry != null) childEntries + selfEntry else childEntries
            val attributeNames = allEntries.flatMap { it.attributes.keys }.distinct().sorted()

            val attributes = attributeNames.map { name ->
                LdapAttribute(name, "Unknown", null)
            }

            LdapSchema(attributes, false)
        }
    }

    private fun parseAttributeType(definition: String): LdapAttribute? {
        // NAME can be 'name' or ( 'name1' 'name2' )
        val nameRegex = "NAME\\s+(?:'([^']+)'|\\(\\s*((?:'[^']+'\\s*)+)\\))".toRegex()
        val nameMatch = nameRegex.find(definition)

        val name = if (nameMatch != null) {
            if (nameMatch.groupValues[1].isNotEmpty()) {
                nameMatch.groupValues[1]
            } else {
                // It's a list, take the first one
                nameMatch.groupValues[2].trim().split(Regex("\\s+")).firstOrNull()?.removeSurrounding("'") ?: ""
            }
        } else ""

        if (name.isEmpty()) return null

        val syntaxRegex = "SYNTAX\\s+([0-9\\.]+)".toRegex()
        val syntaxMatch = syntaxRegex.find(definition)
        val syntaxOid = syntaxMatch?.groupValues?.get(1) ?: "Unknown"

        val descRegex = "DESC\\s+'([^']+)'".toRegex()
        val descMatch = descRegex.find(definition)
        val description = descMatch?.groupValues?.get(1)

        return LdapAttribute(name, translateSyntax(syntaxOid), description)
    }

    private fun translateSyntax(oid: String): String {
        return when (oid) {
            "1.3.6.1.4.1.1466.115.121.1.15" -> "DirectoryString"
            "1.3.6.1.4.1.1466.115.121.1.12" -> "DistinguishedName"
            "1.3.6.1.4.1.1466.115.121.1.27" -> "Integer"
            "1.3.6.1.4.1.1466.115.121.1.36" -> "NumericString"
            "1.3.6.1.4.1.1466.115.121.1.26" -> "IA5String"
            "1.3.6.1.4.1.1466.115.121.1.7" -> "Boolean"
            "1.3.6.1.4.1.1466.115.121.1.24" -> "GeneralizedTime"
            "1.3.6.1.4.1.1466.115.121.1.53" -> "UtcTime"
            "1.3.6.1.4.1.1466.115.121.1.5" -> "Binary"
            else -> oid
        }
    }

    /**
     * Closes the LDAP connection and releases resources.
     */
    override fun close() {
        scope.cancel()
        connectionFactory = null
    }

    /**
     * Checks if the client is currently connected.
     */
    fun isConnected(): Boolean {
        return connectionFactory != null
    }

    companion object {
        /**
         * Extracts the relative name from a DN.
         */
        private fun extractName(dn: String, baseDN: String): String {
            if (baseDN.isNotEmpty() && dn.endsWith(baseDN, ignoreCase = true)) {
                val relativeDN = dn.removeSuffix(",$baseDN").removeSuffix(baseDN)
                if (relativeDN == baseDN || relativeDN.isEmpty()) {
                    return dn // This is the base DN itself
                }
                // Extract the first component
                val parts = relativeDN.split(",")
                if (parts.isNotEmpty()) {
                    return parts[0].trim()
                }
            }

            // If we can't extract relative name, use the first component of the DN
            val parts = dn.split(",")
            if (parts.isNotEmpty()) {
                return parts[0].trim()
            }

            return dn
        }
    }
}

/**
 * Converts our SearchScope enum to ldaptive's SearchScope.
 */
private fun SearchScope.toLdaptiveScope(): org.ldaptive.SearchScope {
    return when (this) {
        SearchScope.BASE -> org.ldaptive.SearchScope.OBJECT
        SearchScope.ONE_LEVEL -> org.ldaptive.SearchScope.ONELEVEL
        SearchScope.SUBTREE -> org.ldaptive.SearchScope.SUBTREE
    }
}

/**
 * Converts an ldaptive LdapEntry to our Entry model.
 */
private fun LdapEntry.toEntry(): Entry {
    val attributes = mutableMapOf<String, List<String>>()

    for (attribute in this.attributes) {
        attributes[attribute.name] = attribute.stringValues.toList()
    }

    return Entry(
        dn = this.dn,
        attributes = attributes
    )
}

/**
 * Checks if an ldaptive result indicates a retryable error.
 */
private fun isRetryableError(result: SearchResponse): Boolean {
    return when (result.resultCode) {
        ResultCode.SERVER_DOWN,
        ResultCode.CONNECT_ERROR,
        ResultCode.UNAVAILABLE,
        ResultCode.BUSY,
        ResultCode.UNWILLING_TO_PERFORM -> true

        else -> false
    }
}
