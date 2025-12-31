package com.moribito.gui.ui.screens

import RootConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moribito.config.ConfigurationService
import com.moribito.gui.theme.*
import com.moribito.gui.ui.components.ActionBar
import com.moribito.gui.ui.components.TextField as AppTextField
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.MainViewModel
import compose.icons.Octicons
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import compose.icons.octicons.CheckCircle16
import compose.icons.octicons.Dash16
import compose.icons.octicons.Eye16
import compose.icons.octicons.EyeClosed16
import compose.icons.octicons.Gear16
import compose.icons.octicons.Lock16
import compose.icons.octicons.Mention16
import compose.icons.octicons.Person16
import compose.icons.octicons.Plus16
import compose.icons.octicons.Server16
import compose.icons.octicons.Workflow16
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.component.styling.CheckboxColors
import org.koin.compose.koinInject

@Composable
private fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val iconColor = tint ?: JewelTheme.contentColor

    Box(
        modifier = modifier
            .size(24.dp)
            .hoverable(interactionSource)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHovered) iconColor.copy(alpha = 0.7f) else iconColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSplitPaneApi::class)
@Composable
fun ConfigurationScreen(
    viewModel: MainViewModel,
    ldapConfig: RootConfig,
    connectionState: ConnectionState,
    configService: ConfigurationService = koinInject()
) {
    // Get the current connection
    val currentConn = viewModel.getCurrentConnection()

    var name by remember { mutableStateOf(currentConn.name) }
    var host by remember { mutableStateOf(currentConn.host) }
    var port by remember { mutableStateOf(currentConn.port.toString()) }
    var baseDN by remember { mutableStateOf(currentConn.baseDN) }
    var useSSL by remember { mutableStateOf(currentConn.useSsl) }
    var useTLS by remember { mutableStateOf(currentConn.useTls) }
    var bindUser by remember { mutableStateOf(currentConn.bindUser) }
    var bindPass by remember { mutableStateOf(currentConn.bindPass) }
    var showPassword by remember { mutableStateOf(false) }

    val state = rememberSplitPaneState(0.3f)
    var configs by remember { mutableStateOf(configService.load().connections) }
    var selectedConnectionName by remember { mutableStateOf(currentConn.name) }

    // Update form fields when selection changes
    LaunchedEffect(selectedConnectionName) {
        configs.find { it.name == selectedConnectionName }?.let { conn ->
            name = conn.name
            host = conn.host
            port = conn.port.toString()
            baseDN = conn.baseDN
            useSSL = conn.useSsl
            useTLS = conn.useTls
            bindUser = conn.bindUser
            bindPass = conn.bindPass
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

    // Form is valid if all required fields are valid
    val isFormValid = isHostValid && isPortValid && isBaseDNValid && isBindUserValid

    // Log validation state changes
    LaunchedEffect(isFormValid, isHostValid, isPortValid, isBaseDNValid, isBindUserValid) {
        println("[ConfigurationScreen] Validation state changed:")
        println("  isFormValid = $isFormValid")
        println("  isHostValid = $isHostValid (host='$host')")
        println("  isPortValid = $isPortValid (port='$port')")
        println("  isBaseDNValid = $isBaseDNValid (baseDN='$baseDN')")
        println("  isBindUserValid = $isBindUserValid (bindUser='$bindUser')")
    }

    HorizontalSplitPane(
        splitPaneState = state,
        modifier = Modifier.fillMaxSize().background(IntelliJColors.islandBackground)
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
                Modifier.fillMaxSize().background(IntelliJColors.islandBackground)
                    .absolutePadding(top = AppSpacing.xs, bottom = AppSpacing.xs),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VerticallyScrollableContainer(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        configs.forEach {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .height(26.dp)
                                    .clickable(
                                        enabled = true,
                                        onClick = {
                                            selectedConnectionName = it.name
                                        },
                                    ),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize()
                                        .absolutePadding(right = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedConnectionName == it.name) {
                                        Box(
                                            Modifier.width(4.dp).fillMaxHeight().background(IntelliJColors.warning),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .absolutePadding(left = if (selectedConnectionName != it.name) 8.dp else 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = it.name, fontSize = AppTypography.labelLarge.fontSize)
                                        Text(
                                            text = "${it.host}:${it.port}",
                                            fontSize = AppMonospace.small.fontSize,
                                            fontFamily = AppMonospace.small.fontFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ActionBar(
                    rightContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            IconButton(
                                icon = Octicons.Dash16,
                                contentDescription = "Delete",
                                onClick = {
                                    if (configs.size > 1) {
                                        viewModel.deleteConnection(selectedConnectionName)
                                        configs = viewModel.getConfig().connections
                                        // Select the first connection after deletion
                                        selectedConnectionName = configs.firstOrNull()?.name ?: ""
                                        configService.save(viewModel.getConfig())
                                    }
                                }
                            )

                            IconButton(
                                icon = Octicons.Plus16,
                                contentDescription = "Add",
                                onClick = {
                                    val newConn = viewModel.addConnection()
                                    configs = viewModel.getConfig().connections
                                    selectedConnectionName = newConn.name
                                    configService.save(viewModel.getConfig())
                                }
                            )
                        }
                    }
                )
            }
        }
        second(minSize = 400.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.lg)
                    .background(IntelliJColors.islandBackground),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                // Section Title
                Text(
                    text = "LDAP Configuration",
                    style = AppTypography.titleMedium
                )

                // Configuration Form Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                ) {
                    // Name Field (Optional)
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Connection Name (Optional)",
                        placeholder = "My LDAP Server",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Mention16,
                                contentDescription = "Name",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Host Field
                    AppTextField(
                        value = host,
                        onValueChange = {
                            host = it
                        },
                        label = "Server Host",
                        placeholder = "ldap.example.com",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Server16,
                                contentDescription = "Host",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        trailingIcon = {
                            if (isHostValid && host.isNotEmpty()) {
                                Icon(
                                    imageVector = Octicons.CheckCircle16,
                                    contentDescription = "Valid",
                                    tint = IntelliJColors.success,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        isError = showHostError,
                        errorMessage = "Server host is required",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Port Field
                    AppTextField(
                        value = port,
                        onValueChange = {
                            port = it
                        },
                        label = "Port",
                        placeholder = "389",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Gear16,
                                contentDescription = "Port",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        trailingIcon = {
                            if (isPortValid && port.isNotEmpty()) {
                                Icon(
                                    imageVector = Octicons.CheckCircle16,
                                    contentDescription = "Valid",
                                    tint = IntelliJColors.success,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        isError = showPortError,
                        errorMessage = "Port must be between 1-65535",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Base DN Field
                    AppTextField(
                        value = baseDN,
                        onValueChange = {
                            baseDN = it
                        },
                        label = "Base DN",
                        placeholder = "dc=example,dc=com",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Workflow16,
                                contentDescription = "Base DN",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        trailingIcon = {
                            if (isBaseDNValid && baseDN.isNotEmpty()) {
                                Icon(
                                    imageVector = Octicons.CheckCircle16,
                                    contentDescription = "Valid",
                                    tint = IntelliJColors.success,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        isError = showBaseDNError,
                        errorMessage = "Base DN is required",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Bind User Field
                    AppTextField(
                        value = bindUser,
                        onValueChange = {
                            bindUser = it
                        },
                        label = "Bind User",
                        placeholder = "cn=admin,dc=example,dc=com",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Person16,
                                contentDescription = "User",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        trailingIcon = {
                            if (isBindUserValid && bindUser.isNotEmpty()) {
                                Icon(
                                    imageVector = Octicons.CheckCircle16,
                                    contentDescription = "Valid",
                                    tint = IntelliJColors.success,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        isError = showBindUserError,
                        errorMessage = "Bind user is required",
                        isRequired = true,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Bind Password Field
                    AppTextField(
                        value = bindPass,
                        onValueChange = { bindPass = it },
                        label = "Bind Password",
                        leadingIcon = {
                            Icon(
                                imageVector = Octicons.Lock16,
                                contentDescription = "Password",
                                modifier = Modifier.size(12.dp),
                                tint = JewelTheme.contentColor
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                icon = if (showPassword) Octicons.EyeClosed16 else Octicons.Eye16,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                onClick = { showPassword = !showPassword }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                    )
                }

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

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (connectionState) {
                        is ConnectionState.Connected -> {
                            OutlinedButton(
                                onClick = { viewModel.disconnect() }
                            ) {
                                Icon(
                                    imageVector = Octicons.Lock16,
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
                            // Save button (outlined style for visual distinction)
                            OutlinedButton(
                                onClick = {
                                    println("=== [ConfigurationScreen] Save button CLICKED ===")
                                    println("[ConfigurationScreen] isFormValid = $isFormValid")

                                    if (isFormValid) {
                                        println("[ConfigurationScreen] Form is valid, proceeding with save")
                                        val portInt = port.toIntOrNull() ?: 389

                                        // Use ViewModel to save
                                        val finalName = viewModel.saveConnection(
                                            configService = configService,
                                            selectedConnectionName = selectedConnectionName,
                                            name = name,
                                            host = host,
                                            port = portInt,
                                            baseDN = baseDN,
                                            useSsl = useSSL,
                                            useTls = useTLS,
                                            bindUser = bindUser,
                                            bindPass = bindPass
                                        )

                                        // Refresh the configs list and update selected connection name
                                        configs = viewModel.getConfig().connections
                                        selectedConnectionName = finalName

                                        println("[ConfigurationScreen] Save completed. New selected name: $finalName")
                                    } else {
                                        println("[ConfigurationScreen] Form is INVALID - cannot save")
                                        println("[ConfigurationScreen]   hostValid=$isHostValid, portValid=$isPortValid, baseDNValid=$isBaseDNValid, bindUserValid=$isBindUserValid")
                                    }
                                }
                            ) {
                                Text("Save")
                            }

                            Spacer(modifier = Modifier.width(AppSpacing.sm))

                            // Connect button (primary action)
                            DefaultButton(
                                onClick = {
                                    println("=== [ConfigurationScreen] Connect button CLICKED ===")
                                    println("[ConfigurationScreen] isFormValid = $isFormValid")

                                    if (isFormValid) {
                                        println("[ConfigurationScreen] Form is valid, proceeding with connect")
                                        val portInt = port.toIntOrNull() ?: 389

                                        // Use ViewModel to save and connect
                                        val finalName = viewModel.saveAndConnect(
                                            configService = configService,
                                            selectedConnectionName = selectedConnectionName,
                                            name = name,
                                            host = host,
                                            port = portInt,
                                            baseDN = baseDN,
                                            useSsl = useSSL,
                                            useTls = useTLS,
                                            bindUser = bindUser,
                                            bindPass = bindPass
                                        )

                                        // Refresh the configs list and update selected connection name
                                        configs = viewModel.getConfig().connections
                                        selectedConnectionName = finalName

                                        println("[ConfigurationScreen] Connect initiated. New selected name: $finalName")
                                    } else {
                                        println("[ConfigurationScreen] Form is INVALID - cannot connect")
                                        println("[ConfigurationScreen]   hostValid=$isHostValid, portValid=$isPortValid, baseDNValid=$isBaseDNValid, bindUserValid=$isBindUserValid")
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
}
