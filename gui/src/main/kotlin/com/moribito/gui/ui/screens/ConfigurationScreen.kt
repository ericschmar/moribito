package com.moribito.gui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.moribito.config.LdapConfig
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.MainViewModel
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.component.*
import io.github.composefluent.defaultFontFamily
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.CheckmarkCircle
import io.github.composefluent.icons.regular.Circle
import io.github.composefluent.icons.regular.CubeSync
import io.github.composefluent.icons.regular.CubeTree
import io.github.composefluent.icons.regular.ErrorCircle
import io.github.composefluent.icons.regular.LockClosed
import io.github.composefluent.icons.regular.Person
import io.github.composefluent.icons.regular.Play
import io.github.composefluent.icons.regular.Server
import io.github.composefluent.icons.regular.Settings
import org.jetbrains.skia.FontStyle

@Composable
fun ConfigurationScreen(
    viewModel: MainViewModel,
    ldapConfig: LdapConfig,
    connectionState: ConnectionState
) {
    var host by remember { mutableStateOf(ldapConfig.host) }
    var port by remember { mutableStateOf(ldapConfig.port.toString()) }
    var baseDN by remember { mutableStateOf(ldapConfig.baseDN) }
    var useSSL by remember { mutableStateOf(ldapConfig.useSSL) }
    var useTLS by remember { mutableStateOf(ldapConfig.useTLS) }
    var bindUser by remember { mutableStateOf(ldapConfig.bindUser) }
    var bindPass by remember { mutableStateOf(ldapConfig.bindPass) }

    // Validation states
    val isHostValid = host.isNotBlank()
    val isPortValid = port.toIntOrNull() != null
    val isBaseDNValid = baseDN.isNotBlank()

    Mica(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section Title
            Text(
                text = "LDAP Configuration",
                style = FluentTheme.typography.title
            )

            // Connection Status Card
            ConnectionStatusCard(connectionState)

            // Configuration Form Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Host Field
                TextField(
                    value = host,
                    onValueChange = { host = it },
                    header = { Text("Server Host") },
                    placeholder = { Text("ldap.example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Regular.Server,
                            contentDescription = "Host"
                        )
                    },
                    trailing = {
                        if (isHostValid) {
                            Icon(
                                imageVector = Icons.Regular.CheckmarkCircle,
                                contentDescription = "Valid",
                                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Port Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextField(
                        value = port,
                        onValueChange = { port = it },
                        header = { Text("Port") },
                        placeholder = { Text("389") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Regular.Settings,
                                contentDescription = "Port"
                            )
                        },
                        trailing = {
                            if (isPortValid) {
                                Icon(
                                    imageVector = Icons.Regular.CheckmarkCircle,
                                    contentDescription = "Valid",
                                    tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Base DN Field
                TextField(
                    value = baseDN,
                    onValueChange = { baseDN = it },
                    header = { Text("Base DN") },
                    placeholder = { Text("dc=example,dc=com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Regular.CubeTree,
                            contentDescription = "Base DN"
                        )
                    },
                    trailing = {
                        if (isBaseDNValid) {
                            Icon(
                                imageVector = Icons.Regular.CheckmarkCircle,
                                contentDescription = "Valid",
                                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // SSL/TLS Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckBox(
                            checked = useSSL,
                            onCheckStateChange = { newState ->
                                useSSL = newState == true
                                if (useSSL) useTLS = false
                            }
                        )
                        Text("Use SSL")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckBox(
                            checked = useTLS,
                            onCheckStateChange = { newState ->
                                useTLS = newState == true
                                if (useTLS) useSSL = false
                            }
                        )
                        Text("Use TLS")
                    }
                }

                // Bind User Field
                TextField(
                    value = bindUser,
                    onValueChange = { bindUser = it },
                    header = { Text("Bind User") },
                    placeholder = { Text("cn=admin,dc=example,dc=com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Regular.Person,
                            contentDescription = "User"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailing = {
                        if (isBaseDNValid) {
                            Icon(
                                imageVector = Icons.Regular.CheckmarkCircle,
                                contentDescription = "Valid",
                                tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                            )
                        }
                    },
                )

                // Bind Password Field
                TextField(
                    value = bindPass,
                    onValueChange = { bindPass = it },
                    header = { Text("Bind Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Regular.LockClosed,
                            contentDescription = "Password"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                when (connectionState) {
                    is ConnectionState.Connected -> {
                        Button(
                            onClick = { viewModel.disconnect() }
                        ) {
                            Icon(
                                imageVector = Icons.Regular.LockClosed,
                                contentDescription = "Disconnect",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disconnect")
                        }
                    }

                    is ConnectionState.Connecting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            ProgressRing(modifier = Modifier.size(20.dp))
                            Text("Connecting...")
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                val portInt = port.toIntOrNull() ?: 389
                                viewModel.updateConfig(
                                    host = host,
                                    port = portInt,
                                    baseDN = baseDN,
                                    useSSL = useSSL,
                                    useTLS = useTLS,
                                    bindUser = bindUser,
                                    bindPass = bindPass
                                )
                                viewModel.connect()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Regular.Play,
                                contentDescription = "Connect",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(connectionState: ConnectionState) {
    val (statusText, statusIcon) = when (connectionState) {
        is ConnectionState.Connected -> "Connected" to Icons.Regular.CheckmarkCircle
        is ConnectionState.Connecting -> "Connecting..." to Icons.Regular.CubeSync
        is ConnectionState.Disconnected -> "Disconnected" to Icons.Regular.Circle
        is ConnectionState.Error -> "Error: ${connectionState.message}" to Icons.Regular.ErrorCircle
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = "Status",
            modifier = Modifier.size(20.dp)
        )
        Text(text = statusText)
    }
}
