package com.moribito.gui.ui.components

import com.moribito.config.BindCredential
import com.moribito.config.LdapConfig

/**
 * Tree node types for the configuration screen.
 * Represents the hierarchical structure of connections and their bind credentials.
 */
sealed class ConfigTreeNode {
    /**
     * Parent node representing an LDAP connection.
     * Children are the bind credentials associated with this connection.
     */
    data class ConnectionNode(
        val config: LdapConfig
    ) : ConfigTreeNode() {
        val name: String get() = config.name
    }

    /**
     * Child node representing a bind DN credential within a connection.
     */
    data class BindDnNode(
        val parentConfig: LdapConfig,
        val credential: BindCredential
    ) : ConfigTreeNode() {
        val label: String get() = credential.label
    }
}
