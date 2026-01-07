package com.moribito.ldap

import com.moribito.ldap.query.LdapQueryConverter
import com.moribito.ldap.query.SqlParser
import io.github.serpro69.kfaker.Faker
import kotlinx.coroutines.delay

/**
 * A mock implementation of LdapClient for testing and development.
 * Generates a large tree of fake data.
 */
class MockLdapClient(
    private val config: LdapConfig,
    private val credential: com.moribito.config.BindCredential? = null,
    private val nodeCount: Int = 10000
) : ILdapClient {
    private val faker = Faker()
    private val rootNode: TreeNode
    private val entries = mutableMapOf<String, Entry>()
    private val childrenMap = mutableMapOf<String, MutableList<String>>()
    private var isConnected = false

    init {
        rootNode = generateTree()
    }

    override suspend fun connect() {
        delay(500) // Simulate network delay
        isConnected = true
    }

    override fun isConnected(): Boolean = isConnected

    override fun close() {
        isConnected = false
    }

    private fun generateTree(): TreeNode {
        val rootDn = config.baseDN
        val rootEntry = createEntry(rootDn, "dc", "com")
        entries[rootDn] = rootEntry
        
        val root = TreeNode(
            dn = rootDn,
            name = "dc=com",
            children = null,
            isLoaded = false
        )

        // Generate structure:
        // dc=com
        //   ou=people
        //     uid=user1...
        //   ou=groups
        //     cn=group1...
        //   ou=computers
        //     cn=comp1...
        
        val ous = listOf("people", "groups", "computers", "printers", "servers")
        
        ous.forEach { ou ->
            val ouDn = "ou=$ou,$rootDn"
            val ouEntry = createEntry(ouDn, "ou", ou)
            entries[ouDn] = ouEntry
            addChild(rootDn, ouDn)
            
            // Distribute nodes among OUs
            val count = when(ou) {
                "people" -> (nodeCount * 0.6).toInt()
                "groups" -> (nodeCount * 0.1).toInt()
                else -> (nodeCount * 0.1).toInt()
            }
            
            generateChildren(ouDn, count, ou)
        }
        
        return root
    }
    
    private fun generateChildren(parentDn: String, count: Int, type: String) {
        for (i in 1..count) {
            val name = when(type) {
                "people" -> faker.name.name().replace(" ", ".").lowercase()
                "groups" -> "group-$i"
                else -> "item-$i"
            }
            
            val rdnAttribute = if (type == "people") "uid" else "cn"
            val dn = "$rdnAttribute=$name,$parentDn"
            
            val entry = createEntry(dn, rdnAttribute, name)
            entries[dn] = entry
            addChild(parentDn, dn)
        }
    }
    
    private fun createEntry(dn: String, rdnAttr: String, rdnValue: String): Entry {
        val attributes = mutableMapOf<String, List<String>>()
        attributes["objectClass"] = listOf("top", "person", "organizationalPerson", "inetOrgPerson")
        attributes[rdnAttr] = listOf(rdnValue)
        
        if (rdnAttr == "uid") {
            attributes["cn"] = listOf(faker.name.name())
            attributes["sn"] = listOf(faker.name.lastName())
            attributes["mail"] = listOf(faker.internet.email())
            attributes["telephoneNumber"] = listOf(faker.phoneNumber.phoneNumber())
        } else if (rdnAttr == "cn") {
             attributes["description"] = listOf(faker.lorem.words())
        }
        
        return Entry(dn, attributes)
    }
    
    private fun addChild(parentDn: String, childDn: String) {
        childrenMap.getOrPut(parentDn) { mutableListOf() }.add(childDn)
    }

    override suspend fun search(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String>
    ): List<Entry> {
        delay(100) // Simulate latency
        
        // Simple implementation: if baseDN matches exactly, return it.
        // If scope is ONE_LEVEL, return children.
        // If scope is SUBTREE, return all descendants (simplified to children for now or need recursion)
        
        val result = mutableListOf<Entry>()
        
        when (scope) {
            SearchScope.BASE -> {
                entries[baseDN]?.let { result.add(it) }
            }
            SearchScope.ONE_LEVEL -> {
                childrenMap[baseDN]?.forEach { childDn ->
                    entries[childDn]?.let { result.add(it) }
                }
            }
            else -> {
                 // Subtree - simplified to just children for this mock to avoid massive recursion in simple search
                 // For a real mock we might want to traverse, but for "getEntry" or "getChildren" usage this suffices
                 entries[baseDN]?.let { result.add(it) }
                 childrenMap[baseDN]?.forEach { childDn ->
                    entries[childDn]?.let { result.add(it) }
                }
            }
        }
        
        return result
    }

    override suspend fun searchPaged(
        baseDN: String,
        filter: String,
        scope: SearchScope,
        attributes: List<String>,
        pageSize: Int,
        cookie: ByteArray?
    ): SearchPage {
        val allResults = search(baseDN, filter, scope, attributes)
        
        // Simple pagination logic
        val offset = if (cookie != null && cookie.isNotEmpty()) {
             String(cookie).toIntOrNull() ?: 0
        } else 0
        
        val pageEntries = allResults.drop(offset).take(pageSize)
        val nextOffset = offset + pageEntries.size
        val hasMore = nextOffset < allResults.size
        val nextCookie = if (hasMore) nextOffset.toString().toByteArray() else null
        
        return SearchPage(
            entries = pageEntries,
            hasMore = hasMore,
            cookie = nextCookie,
            pageSize = pageSize,
            totalCount = allResults.size
        )
    }

    override suspend fun getChildren(dn: String): List<TreeNode> {
        delay(50) // Simulate latency
        val searchDN = if (dn.isEmpty()) config.baseDN else dn
        val childrenDns = childrenMap[searchDN] ?: emptyList()

        println("MockLdapClient.getChildren: dn='$dn', searchDN='$searchDN', found ${childrenDns.size} children")
        println("MockLdapClient.getChildren: childrenMap keys = ${childrenMap.keys.take(10)}")

        return childrenDns.map { childDn ->
            val rdn = childDn.split(",").first()
            TreeNode(
                dn = childDn,
                name = rdn,
                children = null,
                isLoaded = false
            )
        }
    }

    override suspend fun getChildrenPaged(dn: String, pageSize: Int, cookie: ByteArray?): SearchPage {
        delay(50) // Simulate latency
        val searchDN = if (dn.isEmpty()) config.baseDN else dn
        val childrenDns = childrenMap[searchDN] ?: emptyList()
        
        val offset = if (cookie != null && cookie.isNotEmpty()) {
            String(cookie).toIntOrNull() ?: 0
        } else 0
        
        val pagedDns = childrenDns.drop(offset).take(pageSize)
        val hasMore = offset + pagedDns.size < childrenDns.size
        val nextCookie = if (hasMore) (offset + pagedDns.size).toString().toByteArray() else null
        
        val entries = pagedDns.mapNotNull { entries[it] }
        
        return SearchPage(
            entries = entries,
            hasMore = hasMore,
            cookie = nextCookie,
            pageSize = pageSize,
            totalCount = childrenDns.size
        )
    }

    override suspend fun getEntry(dn: String): Entry {
        delay(50) // Simulate latency
        return entries[dn] ?: throw LdapException("Entry not found: $dn", resultCode = LdapException.RESULT_NO_SUCH_OBJECT)
    }
    
    override suspend fun buildTree(): TreeNode {
        delay(50) // Simulate latency
        return TreeNode(
            dn = config.baseDN,
            name = config.baseDN.split(",").first(),
            children = null,
            isLoaded = false
        )
    }

    override suspend fun buildTreeFromDN(startDN: String): TreeNode {
        delay(50) // Simulate latency
        return TreeNode(
            dn = startDN,
            name = startDN.split(",").first(),
            children = null,
            isLoaded = false
        )
    }

    override suspend fun loadChildrenWithMembers(node: TreeNode, includeVirtualMembers: Boolean): TreeNode {
        return loadChildrenPaged(node, includeVirtualMembers, pageSize = 50)
    }

    override suspend fun loadChildrenPaged(
        node: TreeNode,
        includeVirtualMembers: Boolean,
        pageSize: Int,
        cookie: ByteArray?
    ): TreeNode {
        val searchPage = getChildrenPaged(node.dn, pageSize, cookie)
        val hierarchicalChildren = searchPage.entries.map { entry ->
            TreeNode(
                dn = entry.dn,
                name = entry.dn.split(",").first(),
                children = null,
                isLoaded = false
            )
        }

        return node.copy(
            children = (node.children?.filter { !it.isLoadMoreNode } ?: emptyList()) + hierarchicalChildren,
            isLoaded = true,
            nextPageCookie = searchPage.cookie
        )
    }
    
    override suspend fun customSearch(filter: String): List<Entry> {
        return search(
            baseDN = config.baseDN,
            filter = filter,
            scope = SearchScope.SUBTREE,
            attributes = listOf("*")
        )
    }

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
    
    override suspend fun inspectSchema(): LdapSchema {
        delay(500) // Simulate latency
        return LdapSchema(
            attributes = listOf(
                LdapAttribute("cn", "String"),
                LdapAttribute("sn", "String"),
                LdapAttribute("uid", "String"),
                LdapAttribute("mail", "String"),
                LdapAttribute("objectClass", "String")
            ),
            containerDns = listOf(config.baseDN),
            isFromSchemaInspection = true
        )
    }

    override suspend fun getAttributesInOu(ouDn: String): LdapSchema {
        delay(100) // Simulate latency
        // For mock, just return the same schema as inspectSchema
        return inspectSchema()
    }
}
