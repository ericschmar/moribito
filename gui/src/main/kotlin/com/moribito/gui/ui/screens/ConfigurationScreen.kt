package com.moribito.gui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moribito.config.ConfigurationService
import com.moribito.config.RootConfig
import com.moribito.gui.theme.*
import com.moribito.gui.ui.components.ActionBar
import com.moribito.gui.ui.components.BindCredentialTable
import com.moribito.gui.ui.components.ConfigTreeNode
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.koin.compose.koinInject
import com.moribito.gui.ui.components.TextField as AppTextField


@OptIn(ExperimentalLayoutApi::class, ExperimentalSplitPaneApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ConfigurationScreen(
    viewModel: MainViewModel,
    ldapConfig: RootConfig,
    connectionState: ConnectionState,
    configService: ConfigurationService = koinInject()
) {
    val appState by viewModel.state.collectAsState()

    // Get the current connection
    val currentConn = viewModel.getCurrentConnection()

    var name by remember { mutableStateOf(currentConn.name) }
    var host by remember { mutableStateOf(currentConn.host) }
    var port by remember { mutableStateOf(currentConn.port.toString()) }
    var baseDN by remember { mutableStateOf(currentConn.baseDN) }
    var useSSL by remember { mutableStateOf(currentConn.useSsl) }
    var useTLS by remember { mutableStateOf(currentConn.useTls) }

    // Bind DN specific states
    var bindLabel by remember { mutableStateOf("") }
    var bindUser by remember { mutableStateOf("") }
    var bindPass by remember { mutableStateOf("") }
    var isBindDefault by remember { mutableStateOf(false) }

    var showPassword by remember { mutableStateOf(false) }

    val state = rememberSplitPaneState(0.3f)
    var configs by remember(ldapConfig) { mutableStateOf(ldapConfig.connections) }
    var selectedConnectionName by remember { mutableStateOf(currentConn.name) }
    var selectedNode by remember { mutableStateOf<ConfigTreeNode?>(ConfigTreeNode.ConnectionNode(currentConn)) }

    // Update form fields when selection changes
    LaunchedEffect(selectedNode) {
        when (val node = selectedNode) {
            is ConfigTreeNode.ConnectionNode -> {
                val conn = node.config
                selectedConnectionName = conn.name
                name = conn.name
                host = conn.host
                port = conn.port.toString()
                baseDN = conn.baseDN
                useSSL = conn.useSsl
                useTLS = conn.useTls
            }

            is ConfigTreeNode.BindDnNode -> {
                val conn = node.parentConfig
                val cred = node.credential
                selectedConnectionName = conn.name
                // Also update connection fields in case we want to see them? 
                // Usually we just show the bind DN fields
                bindLabel = cred.label
                bindUser = cred.bindUser
                bindPass = cred.bindPass
                isBindDefault = cred.isDefault
            }

            null -> {}
        }
    }

    // Validation states
    val isHostValid = host.isNotBlank()
    val isPortValid = port.toIntOrNull()?.let { it in 1..65535 } ?: false
    val isBaseDNValid = baseDN.isNotBlank()
    val isBindUserValid = bindUser.isNotBlank()

    // Show errors only if field has been touched and is invalid
    val showHostError = !isHostValid
    val showPortError = !isPortValid
    val showBaseDNError = !isBaseDNValid
    val showBindUserError = !isBindUserValid

    // Form is valid if required fields are valid based on context
    val isConnectionValid = isHostValid && isPortValid && isBaseDNValid
    val isCredentialValid = isBindUserValid
    val isFormValid = if (selectedNode is ConfigTreeNode.BindDnNode) {
        isConnectionValid && isCredentialValid
    } else {
        isConnectionValid
    }

    // Log validation state changes
    LaunchedEffect(isFormValid, isConnectionValid, isCredentialValid) {
        println("[ConfigurationScreen] Validation state changed:")
        println("  isFormValid = $isFormValid")
        println("  isConnectionValid = $isConnectionValid (host='$host', port='$port', baseDN='$baseDN')")
        println("  isCredentialValid = $isCredentialValid (bindUser='$bindUser')")
    }

    HorizontalSplitPane(
        splitPaneState = state,
        modifier = Modifier.fillMaxSize().background(IntelliJColors.baseBackground)
    ) {
        splitter {
            visiblePart {
                // The actual line
                Box(Modifier.width(1.dp).fillMaxHeight().background(JewelTheme.globalColors.borders.normal))
            }
            handle {
                // The "Hitbox" (8dp wide makes it easy to grab)
                Box(
                    Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .markAsHandle()
                        .pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }
        first(minSize = 200.dp) {
            Column(
                Modifier.fillMaxSize().background(IntelliJColors.baseBackground)
                    .absolutePadding(top = AppSpacing.xs, bottom = AppSpacing.xs),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VerticallyScrollableContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(AppSpacing.xs),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
                    ) {
                        configs.forEach { config ->
                            var isHovered by remember { mutableStateOf(false) }
                            val isSelected = selectedNode is ConfigTreeNode.ConnectionNode &&
                                    (selectedNode as? ConfigTreeNode.ConnectionNode)?.config?.name == config.name

                            // Match TreeNodeItem styling
                            val backgroundColor = when {
                                isSelected && isHovered -> Color(0xFF4A90E2).copy(alpha = 0.4f)
                                isSelected -> Color(0xFF4A90E2).copy(alpha = 0.3f)
                                isHovered -> Color(0xFF4A90E2).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }

                            val borderColor = if (isSelected) {
                                Color(0xFF2E5F8E)
                            } else {
                                Color.Transparent
                            }

                            val textColor = if (isSelected) {
                                JewelTheme.contentColor
                            } else {
                                JewelTheme.contentColor.copy(alpha = 0.6f)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .drawBehind {
                                        if (isSelected) {
                                            drawRoundRect(
                                                color = borderColor,
                                                topLeft = Offset.Zero,
                                                size = size,
                                                cornerRadius = CornerRadius(6.dp.toPx()),
                                                style = Stroke(width = 1.dp.toPx())
                                            )
                                        }
                                    }
                                    .background(backgroundColor, RoundedCornerShape(6.dp))
                                    .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                                    .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                                    .clickable {
                                        selectedConnectionName = config.name
                                        selectedNode = ConfigTreeNode.ConnectionNode(config)
                                    }
                                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = config.name,
                                    fontSize = AppTypography.labelLarge.fontSize,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(AppSpacing.xs))
                                Text(
                                    text = "${config.host}:${config.port}",
                                    fontSize = AppMonospace.small.fontSize,
                                    fontFamily = AppMonospace.small.fontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
                ActionBar(
                    rightContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            IconButton(
                                onClick = {
                                    if (configs.size > 1) {
                                        viewModel.deleteConnection(selectedConnectionName)
                                        configs = viewModel.getConfig().connections
                                        // Select the first connection after deletion
                                        selectedConnectionName = configs.firstOrNull()?.name ?: ""
                                    }
                                }
                            ) {
                                Icon(key = AllIconsKeys.General.Delete, contentDescription = "Delete")
                            }

                            IconButton(
                                onClick = {
                                    val newConn = viewModel.addConnection()
                                    configs = viewModel.getConfig().connections
                                    selectedConnectionName = newConn.name
                                }
                            ) {
                                Icon(key = AllIconsKeys.General.Add, contentDescription = "Add")
                            }
                        }
                    }
                )
            }
        }
        second(minSize = 400.dp) {
            val currentSelectedConnection = configs.find { it.name == selectedConnectionName }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.lg)
                    .background(IntelliJColors.baseBackground)
            ) {
                // Section Title
                Text(
                    text = if (selectedNode is ConfigTreeNode.BindDnNode) "Bind DN Configuration" else "LDAP Configuration",
                    style = AppTypography.titleMedium,
                    modifier = Modifier.padding(bottom = AppSpacing.lg)
                )


                // Connection Form (scrollable)
                VerticallyScrollableContainer(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                    ) {
                        AppTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Connection Name (Optional)",
                            placeholder = "My LDAP Server",
                            leadingIcon = {
                                Icon(
                                    key = AllIconsKeys.General.User,
                                    contentDescription = "Name",
                                    modifier = Modifier.size(12.dp),
                                    tint = JewelTheme.contentColor
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            AppTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = "Server Host",
                                placeholder = "ldap.example.com",
                                leadingIcon = {
                                    Icon(
                                        key = AllIconsKeys.Webreferences.Server,
                                        contentDescription = "Host",
                                        modifier = Modifier.size(12.dp),
                                        tint = JewelTheme.contentColor
                                    )
                                },
                                modifier = Modifier.weight(0.7f),
                                isError = showHostError,
                                errorMessage = "Required",
                                singleLine = true
                            )

                            AppTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = "Port",
                                placeholder = "389",
                                modifier = Modifier.weight(0.3f),
                                isError = showPortError,
                                errorMessage = "Invalid",
                                singleLine = true
                            )
                        }

                        AppTextField(
                            value = baseDN,
                            onValueChange = { baseDN = it },
                            label = "Base DN",
                            placeholder = "dc=example,dc=com",
                            leadingIcon = {
                                Icon(
                                    key = AllIconsKeys.Toolwindows.ToolWindowStructure,
                                    contentDescription = "Base DN",
                                    modifier = Modifier.size(12.dp),
                                    tint = JewelTheme.contentColor
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = showBaseDNError,
                            errorMessage = "Required",
                            singleLine = true
                        )

                        // SSL/TLS Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                Checkbox(
                                    checked = useSSL,
                                    onCheckedChange = { newState ->
                                        useSSL = newState
                                        if (useSSL) useTLS = false
                                    }
                                )
                                Text("Use SSL")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                Checkbox(
                                    checked = useTLS,
                                    onCheckedChange = { newState ->
                                        useTLS = newState
                                        if (useTLS) useSSL = false
                                    }
                                )
                                Text("Use TLS")
                            }
                        }

                        // Bind Credentials Table
                        Text(
                            text = "Bind Credentials",
                            style = AppTypography.labelLarge,
                            color = AppColors.neutral140
                        )

                        currentSelectedConnection?.let { conn ->
                            BindCredentialTable(
                                credentials = conn.effectiveBindCredentials,
                                selectedCredential = (selectedNode as? ConfigTreeNode.BindDnNode)?.credential,
                                onCredentialSelected = { cred ->
                                    selectedNode = ConfigTreeNode.BindDnNode(conn, cred)
                                },
                                onAddCredential = {
                                    val newCred = viewModel.addBindCredential(conn.name)
                                    configs = viewModel.getConfig().connections
                                    newCred // Return the new credential so table can edit it inline
                                },
                                onDeleteCredential = { cred ->
                                    viewModel.deleteBindCredential(conn.name, cred.id)
                                    configs = viewModel.getConfig().connections
                                    selectedNode =
                                        ConfigTreeNode.ConnectionNode(viewModel.getConfig().connections.find { it.name == conn.name }
                                            ?: conn)
                                },
                                onUpdateCredential = { cred ->
                                    viewModel.updateBindCredential(conn.name, cred)
                                    configs = viewModel.getConfig().connections
                                },
                                onConnect = { cred ->
                                    // Save everything before connecting to ensure changes are picked up
                                    var targetCredential = cred
                                    if (selectedNode is ConfigTreeNode.BindDnNode) {
                                        val node = selectedNode as ConfigTreeNode.BindDnNode
                                        val updatedCred = node.credential.copy(
                                            label = bindLabel,
                                            bindUser = bindUser,
                                            bindPass = bindPass,
                                            isDefault = isBindDefault
                                        )
                                        viewModel.updateBindCredential(node.parentConfig.name, updatedCred)
                                        if (cred.id == updatedCred.id) {
                                            targetCredential = updatedCred
                                        }
                                    }

                                    // Also save connection fields
                                    val portInt = port.toIntOrNull() ?: 389
                                    viewModel.saveConnection(
                                        selectedConnectionName = selectedConnectionName,
                                        name = name,
                                        host = host,
                                        port = portInt,
                                        baseDN = baseDN,
                                        useSsl = useSSL,
                                        useTls = useTLS
                                    )

                                    configs = viewModel.getConfig().connections
                                    viewModel.connect(targetCredential)
                                },
                                modifier = Modifier.height(320.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.lg))

                // Action Buttons (fixed at bottom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            IconButton(
                                onClick = { viewModel.disconnect() }
                            ) {
                                Icon(
                                    key = AllIconsKeys.General.Close,
                                    contentDescription = "Disconnect",
                                    modifier = Modifier.size(AppSizes.iconMedium)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.sm))
                                Text("Disconnect")
                            }
                        }

                        is ConnectionState.Connecting -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                                modifier = Modifier.padding(AppSpacing.md)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(AppSizes.iconMedium))
                                Text("Connecting...")
                            }
                        }

                        else -> {
                            // Save button
                            OutlinedButton(
                                onClick = {
                                    if (selectedNode is ConfigTreeNode.BindDnNode) {
                                        val node = selectedNode as ConfigTreeNode.BindDnNode
                                        val updatedCred = node.credential.copy(
                                            label = bindLabel,
                                            bindUser = bindUser,
                                            bindPass = bindPass,
                                            isDefault = isBindDefault
                                        )
                                        viewModel.updateBindCredential(node.parentConfig.name, updatedCred)

                                        // Also save connection fields (even if on BindDnNode, we should ensure parent is synced)
                                        val portInt = port.toIntOrNull() ?: 389
                                        val finalName = viewModel.saveConnection(
                                            selectedConnectionName = selectedConnectionName,
                                            name = name,
                                            host = host,
                                            port = portInt,
                                            baseDN = baseDN,
                                            useSsl = useSSL,
                                            useTls = useTLS
                                        )

                                        configs = viewModel.getConfig().connections
                                        selectedConnectionName = finalName
                                        // Update selected node to reflect changes
                                        selectedNode = ConfigTreeNode.BindDnNode(
                                            configs.find { it.name == finalName } ?: node.parentConfig,
                                            updatedCred
                                        )
                                    } else {
                                        if (isConnectionValid) {
                                            val portInt = port.toIntOrNull() ?: 389
                                            val finalName = viewModel.saveConnection(
                                                selectedConnectionName = selectedConnectionName,
                                                name = name,
                                                host = host,
                                                port = portInt,
                                                baseDN = baseDN,
                                                useSsl = useSSL,
                                                useTls = useTLS
                                            )
                                            configs = viewModel.getConfig().connections
                                            selectedConnectionName = finalName
                                            // Update selected node
                                            configs.find { it.name == finalName }?.let {
                                                selectedNode = ConfigTreeNode.ConnectionNode(it)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Save")
                            }

                            Spacer(modifier = Modifier.width(AppSpacing.sm))

                            // Connect button
                            DefaultButton(
                                onClick = {
                                    if (selectedNode is ConfigTreeNode.BindDnNode) {
                                        val node = selectedNode as ConfigTreeNode.BindDnNode
                                        val updatedCred = node.credential.copy(
                                            label = bindLabel,
                                            bindUser = bindUser,
                                            bindPass = bindPass,
                                            isDefault = isBindDefault
                                        )
                                        viewModel.updateBindCredential(node.parentConfig.name, updatedCred)

                                        // Also save connection fields
                                        val portInt = port.toIntOrNull() ?: 389
                                        val finalName = viewModel.saveConnection(
                                            selectedConnectionName = selectedConnectionName,
                                            name = name,
                                            host = host,
                                            port = portInt,
                                            baseDN = baseDN,
                                            useSsl = useSSL,
                                            useTls = useTLS
                                        )

                                        configs = viewModel.getConfig().connections
                                        selectedConnectionName = finalName
                                        // Update selected node
                                        val updatedNode = ConfigTreeNode.BindDnNode(
                                            configs.find { it.name == finalName } ?: node.parentConfig,
                                            updatedCred
                                        )
                                        selectedNode = updatedNode

                                        viewModel.connect(updatedCred)
                                    } else {
                                        if (isConnectionValid) {
                                            val portInt = port.toIntOrNull() ?: 389
                                            // Find default credential to connect with
                                            val conn = configs.find { it.name == selectedConnectionName }
                                            val targetCred = conn?.effectiveBindCredentials?.find { it.isDefault }
                                                ?: conn?.effectiveBindCredentials?.firstOrNull()

                                            val finalName = viewModel.saveAndConnect(
                                                selectedConnectionName = selectedConnectionName,
                                                name = name,
                                                host = host,
                                                port = portInt,
                                                baseDN = baseDN,
                                                useSsl = useSSL,
                                                useTls = useTLS,
                                                credential = targetCred
                                            )
                                            configs = viewModel.getConfig().connections
                                            selectedConnectionName = finalName
                                            // Update selected node
                                            configs.find { it.name == finalName }?.let {
                                                selectedNode = ConfigTreeNode.ConnectionNode(it)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        }
    }

    if (appState.showBindDnSelection && appState.connectionForSelection != null) {
        com.moribito.gui.ui.components.BindDnSelectionDialog(
            connection = appState.connectionForSelection!!,
            onSelect = { viewModel.connect(it) },
            onCancel = { viewModel.cancelBindDnSelection() }
        )
    }
}
