package com.moribito.ldap.query

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlParserTest {

    @Test
    fun testBasicQuery() {
        val parser = SqlParser("SELECT * FROM ou.people WHERE name = 'john'")
        val query = parser.parse()
        
        assertEquals(listOf("*"), query.attributes)
        assertEquals("ou.people", query.from)
        assertNotNull(query.where)
        assertTrue(query.where is WhereNode.Comparison)
        val comparison = query.where as WhereNode.Comparison
        assertEquals("name", comparison.field)
        assertEquals(ComparisonOperator.EQUALS, comparison.operator)
        assertEquals("john", comparison.value)
    }

    @Test
    fun testComplexQuery() {
        val parser = SqlParser("SELECT cn, mail FROM people WHERE (objectClass = person AND status = 'active') OR NOT (uid = 100)")
        val query = parser.parse()
        
        assertEquals(listOf("cn", "mail"), query.attributes)
        assertEquals("people", query.from)
        assertNotNull(query.where)
        
        // Root should be OR
        assertTrue(query.where is WhereNode.Binary)
        val orNode = query.where as WhereNode.Binary
        assertEquals(LogicalOperator.OR, orNode.operator)
        
        // Left should be AND
        assertTrue(orNode.left is WhereNode.Binary)
        val andNode = orNode.left as WhereNode.Binary
        assertEquals(LogicalOperator.AND, andNode.operator)
        
        // Right should be NOT
        assertTrue(orNode.right is WhereNode.Not)
    }

    @Test
    fun testConverter() {
        val converter = LdapQueryConverter()
        
        // Comparison
        assertEquals("(cn=john)", converter.convertToLdapFilter(WhereNode.Comparison("cn", ComparisonOperator.EQUALS, "john")))
        assertEquals("(!(cn=john))", converter.convertToLdapFilter(WhereNode.Comparison("cn", ComparisonOperator.NOT_EQUALS, "john")))
        
        // Binary
        val andNode = WhereNode.Binary(
            WhereNode.Comparison("objectClass", ComparisonOperator.EQUALS, "person"),
            LogicalOperator.AND,
            WhereNode.Comparison("cn", ComparisonOperator.EQUALS, "john")
        )
        assertEquals("(&(objectClass=person)(cn=john))", converter.convertToLdapFilter(andNode))
        
        // Like
        assertEquals("(cn=john*)", converter.convertToLdapFilter(WhereNode.Comparison("cn", ComparisonOperator.LIKE, "john%")))
    }

    @Test
    fun testFromConversion() {
        val converter = LdapQueryConverter()
        val baseDn = "dc=example,dc=com"
        
        assertEquals("ou=people,dc=example,dc=com", converter.convertFromToDn("ou.people", baseDn))
        assertEquals("ou=people,ou=users,dc=example,dc=com", converter.convertFromToDn("ou.people.ou.users", baseDn))
        assertEquals("cn=admin,dc=example,dc=com", converter.convertFromToDn("cn=admin", baseDn))
    }
}
