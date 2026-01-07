package com.moribito.ldap

import com.moribito.ldap.query.LdapQueryConverter
import com.moribito.ldap.query.SqlParser
import com.moribito.logging.Logger
import kotlinx.coroutines.*
import org.ldaptive.*
import org.ldaptive.ssl.SslConfig
import kotlin.time.Duration.Companion.milliseconds

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
) : ILdapClient {
    private var connectionFactory: DefaultConnectionFactory? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logger = Logger.get("LdapClient")

    /**
     * Establishes connection to the LDAP server.
     *
     * @throws LdapException if connection fails
     */
    override suspend fun connect() = withContext(Dispatchers.IO) {
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
                message = "Failed to connect to LDAP server ldap${if (config.useSSL) "s" else ""}://${config.host}:${config.port}: ${e.message}",
                cause = e,
                resultCode = e.resultCode?.value(),
                isRetryable = isRetryableError(e)
            )
        } catch (e: Exception) {
            throw LdapException(
                message = "Failed to connect to LDAP server ldap${if (config.useSSL) "s" else ""}://${config.host}:${config.port}: ${e.message}",
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
    override suspend fun search(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String>
    ): List<Entry> = withRetry {
        withContext(Dispatchers.IO) {
            val factory = connectionFactory ?: throw LdapException("Not connected to LDAP server")

            try {
                logger.debug("Searching BaseDN=$baseDN, Filter=$filter, Scope=$scope, Attributes=$attributes")

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

                logger.debug("Search result: success=${result.isSuccess}, code=${result.resultCode}, count=${result.entries.size}, message=${result.diagnosticMessage}")

                if (!result.isSuccess) {
                    throw LdapException(
                        message = "Search failed: ${result.diagnosticMessage}",
                        resultCode = result.resultCode.value(),
                        isRetryable = isRetryableError(result)
                    )
                }

                val entries = result.entries.map { it.toEntry() }
                logger.debug("Converted ${entries.size} entries: ${entries.map { it.dn }}")

                entries
            } catch (e: LdapException) {
                logger.error("Search failed with LdapException", e)
                throw e
            } catch (e: org.ldaptive.LdapException) {
                logger.error("Search failed with org.ldaptive.LdapException", e)
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
    override suspend fun searchPaged(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String>,
        pageSize: Int,
        cookie: ByteArray?
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
                val responseControl =
                    result.getControl(org.ldaptive.control.PagedResultsControl.OID) as? org.ldaptive.control.PagedResultsControl
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
    override suspend fun getChildren(dn: String): List<TreeNode> {
        val searchDN = if (dn.isEmpty()) config.baseDN else dn

        logger.debug("Getting children of DN=$searchDN")

        val entries = search(
            baseDN = searchDN,
            filter = "(objectClass=*)",
            scope = SearchScope.ONE_LEVEL,
            attributes = listOf("dn")
        )

        logger.debug("Found ${entries.size} children: ${entries.map { it.dn }}")

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
     * Gets immediate children of a DN with pagination.
     *
     * @param dn The parent DN (uses baseDN if empty)
     * @param pageSize Number of results per page
     * @param cookie Paging cookie from previous request
     * @return SearchPage containing entries and next cookie
     * @throws LdapException if operation fails
     */
    override suspend fun getChildrenPaged(dn: String, pageSize: Int, cookie: ByteArray?): SearchPage {
        val searchDN = if (dn.isEmpty()) config.baseDN else dn

        logger.debug("Getting paged children of DN=$searchDN, pageSize=$pageSize")

        return searchPaged(
            baseDN = searchDN,
            filter = "(objectClass=*)",
            scope = SearchScope.ONE_LEVEL,
            attributes = listOf("dn"),
            pageSize = pageSize,
            cookie = cookie
        )
    }

    /**
     * Retrieves a specific LDAP entry with all its attributes.
     *
     * @param dn The distinguished name of the entry
     * @return The entry with all attributes
     * @throws LdapException if entry not found or operation fails
     */
    override suspend fun getEntry(dn: String): Entry {
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
    override suspend fun buildTree(): TreeNode {
        return TreeNode(
            dn = config.baseDN,
            name = extractName(config.baseDN, ""),
            children = null,
            isLoaded = false
        )
    }

    override suspend fun buildTreeFromDN(startDN: String): TreeNode {
        return TreeNode(
            dn = startDN,
            name = extractName(startDN, ""),
            children = null,
            isLoaded = false
        )
    }

    override suspend fun loadChildrenWithMembers(node: TreeNode, includeVirtualMembers: Boolean): TreeNode {
        return loadChildrenPaged(node, includeVirtualMembers, pageSize = 1000) // Default to large page for backward compatibility
    }

    /**
     * Loads a page of children for a tree node, including virtual member children if enabled.
     *
     * @param node The node to load children for
     * @param includeVirtualMembers Whether to include group members as virtual children
     * @param pageSize Number of results per page
     * @param cookie Paging cookie from previous request
     * @return The node with loaded page of children
     * @throws LdapException if operation fails
     */
    override suspend fun loadChildrenPaged(
        node: TreeNode,
        includeVirtualMembers: Boolean,
        pageSize: Int,
        cookie: ByteArray?
    ): TreeNode {
        // Get hierarchical children
        val searchPage = getChildrenPaged(node.dn, pageSize, cookie)
        val hierarchicalChildren = searchPage.entries.map { entry ->
            TreeNode(
                dn = entry.dn,
                name = extractName(entry.dn, node.dn),
                children = null,
                isLoaded = false
            )
        }.toMutableList()

        // If virtual members are enabled and we're on the first page, add member references
        // Note: For now, we only load virtual members on the first page to keep it simple,
        // or we could implement paging for them too if needed.
        if (includeVirtualMembers && cookie == null) {
            try {
                val entry = getEntry(node.dn)
                val memberDNs = mutableListOf<String>()

                // Check for various member attributes
                entry.attributes["member"]?.let { memberDNs.addAll(it) }
                entry.attributes["uniqueMember"]?.let { memberDNs.addAll(it) }
                entry.attributes["memberOf"]?.let { memberDNs.addAll(it) }

                // Create virtual TreeNode for each member
                logger.debug("Processing ${memberDNs.size} member DNs for ${node.dn}, hierarchical children: ${hierarchicalChildren.size}")
                memberDNs.forEach { memberDN ->
                    // Check if this member is already in hierarchical children
                    val alreadyExists = hierarchicalChildren.any { it.dn.equals(memberDN, ignoreCase = true) }
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
                        logger.debug("Added virtual member: $memberDN")
                    }
                }
            } catch (e: Exception) {
                // If we can't get members, just continue with hierarchical children
                logger.warn("Could not load members for ${node.dn}: ${e.message}")
            }
        }

        return node.copy(
            children = (node.children?.filter { !it.isLoadMoreNode } ?: emptyList()) + hierarchicalChildren,
            isLoaded = true,
            nextPageCookie = searchPage.cookie
        )
    }

    /**
     * Performs a custom LDAP search with user-provided filter.
     *
     * @param filter The LDAP search filter
     * @return List of matching entries
     * @throws LdapException if search fails
     */
    override suspend fun customSearch(filter: String): List<Entry> {
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
    override suspend fun customSearchPaged(
        filter: String,
        pageSize: Int,
        cookie: ByteArray?
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
    override suspend fun executeSqlQuery(sql: String): List<Entry> {
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
    override suspend fun executeSqlQueryPaged(
        sql: String,
        pageSize: Int,
        cookie: ByteArray?
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
     * Inspects the LDAP server schema and recursively discovers the container tree structure.
     * Collects attribute types from both the schema definition and actual entries.
     * Falls back to discovered attributes if schema inspection is not supported.
     */
    override suspend fun inspectSchema(): LdapSchema = withRetry {
        withContext(Dispatchers.IO) {
            val factory = connectionFactory ?: throw LdapException("Not connected to LDAP server")

            try {
                // 1. Get Root DSE to find subschemaSubentry
                val rootDse = search("", "(objectClass=*)", SearchScope.BASE, listOf("subschemaSubentry")).firstOrNull()
                val subschemaSubentry = rootDse?.getAttributeValue("subschemaSubentry")

                val schemaAttributes = if (subschemaSubentry != null) {
                    // 2. Query the schema entry for attribute type definitions
                    val schemaEntry = search(
                        subschemaSubentry,
                        "(objectClass=*)",
                        SearchScope.BASE,
                        listOf("attributeTypes")
                    ).firstOrNull()
                    val attributeTypes = schemaEntry?.getAttributeValues("attributeTypes")

                    if (attributeTypes != null && attributeTypes.isNotEmpty()) {
                        attributeTypes.mapNotNull { parseAttributeType(it) }
                            .distinctBy { it.name }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                // 3. Recursively discover container tree structure and collect attributes
                val (containers, discoveredAttributes) = try {
                    discoverContainerTree(config.baseDN)
                } catch (e: Exception) {
                    logger.warn("Container discovery failed", e)
                    Pair(emptyList(), emptyList())
                }

                // 4. Merge schema attributes with discovered attributes
                val allAttributes = if (schemaAttributes.isNotEmpty()) {
                    // Use schema attributes as primary source (they have proper types and descriptions)
                    val schemaAttrNames = schemaAttributes.map { it.name.lowercase() }.toSet()
                    val additionalAttrs = discoveredAttributes.filter { 
                        it.name.lowercase() !in schemaAttrNames 
                    }
                    (schemaAttributes + additionalAttrs).sortedBy { it.name }
                } else {
                    // Fall back to discovered attributes if schema inspection not supported
                    discoveredAttributes
                }

                logger.info("Schema inspection complete: ${allAttributes.size} attributes, ${containers.size} containers")
                
                return@withContext LdapSchema(allAttributes, containers, schemaAttributes.isNotEmpty())
            } catch (e: Exception) {
                logger.warn("Schema inspection failed", e)
                return@withContext LdapSchema(emptyList(), emptyList(), false)
            }
        }
    }

    /**
     * Recursively discovers the container tree structure and collects all attributes found.
     * 
     * @param baseDN The base DN to start traversal from
     * @param maxDepth Maximum depth to traverse (prevents infinite recursion)
     * @param maxContainers Maximum number of containers to discover (prevents timeout)
     * @return Pair of (container DNs, discovered attributes)
     */
    private suspend fun discoverContainerTree(
        baseDN: String,
        maxDepth: Int = 20,
        maxContainers: Int = 1000
    ): Pair<List<String>, List<LdapAttribute>> {
        val containers = mutableListOf<String>()
        val attributeNames = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        suspend fun traverse(dn: String, depth: Int) {
            // Safety checks
            if (depth > maxDepth) {
                logger.debug("Max depth $maxDepth reached at $dn")
                return
            }
            if (containers.size >= maxContainers) {
                logger.debug("Max containers $maxContainers reached")
                return
            }
            if (dn.lowercase() in visited) {
                return
            }
            
            visited.add(dn.lowercase())

            // Get immediate children with all their attributes
            val children = try {
                search(
                    baseDN = dn,
                    filter = "(objectClass=*)",
                    scope = SearchScope.ONE_LEVEL,
                    attributes = listOf("*") // Get all user attributes
                )
            } catch (e: Exception) {
                logger.debug("Failed to get children of $dn: ${e.message}")
                return
            }

            logger.debug("Found ${children.size} children for DN: $dn")

            for (child in children) {
                // Collect all attribute names from this entry
                attributeNames.addAll(child.attributes.keys)

                // Check if this is a container by examining the DN prefix
                val firstComponent = child.dn.split(',').firstOrNull()?.trim()?.lowercase() ?: ""
                val isContainer = firstComponent.startsWith("ou=") ||   // Organizational Unit
                                  firstComponent.startsWith("cn=") ||   // Common Name (can be containers)
                                  firstComponent.startsWith("dc=") ||   // Domain Component
                                  firstComponent.startsWith("o=") ||    // Organization
                                  firstComponent.startsWith("l=") ||    // Locality
                                  firstComponent.startsWith("c=")       // Country

                logger.debug("Entry: ${child.dn}, firstComponent: $firstComponent, isContainer: $isContainer")

                if (isContainer) {
                    containers.add(child.dn)
                    traverse(child.dn, depth + 1) // Recurse into container
                }
                // Skip non-container entries (persons, computers, etc.)
            }
        }
        
        // Start traversal from baseDN
        containers.add(baseDN)
        traverse(baseDN, 0)
        
        logger.info("Discovered ${containers.size} containers and ${attributeNames.size} unique attributes")
        
        // Convert attribute names to LdapAttribute objects
        val discoveredAttributes = attributeNames.sorted().map { name ->
            LdapAttribute(name, "Unknown", null)
        }
        
        return Pair(containers.sorted(), discoveredAttributes)
    }

    /**
     * Queries an OU (or any entry) for the attributes present in it and its immediate children.
     */
    override suspend fun getAttributesInOu(ouDn: String): LdapSchema = withRetry {
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

            LdapSchema(attributes, emptyList(), false)
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
    override fun isConnected(): Boolean {
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
