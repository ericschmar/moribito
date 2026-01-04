package com.moribito.gui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI
import com.moribito.gui.license.AccessStatus
import com.moribito.gui.license.LicenseResult
import com.moribito.gui.theme.*
import com.moribito.gui.ui.components.ConnectionCard
import com.moribito.gui.ui.components.Island
import com.moribito.gui.ui.components.TextField as AppTextField
import com.moribito.gui.viewmodel.AppView
import com.moribito.gui.viewmodel.LoadingState
import com.moribito.gui.viewmodel.LoadingState.Loading
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Start screen displayed on app launch.
 * Features a clean two-panel layout with branding and recent connections.
 *
 * Left panel: "Moribito" title centered with orange accent color
 * Right panel: Recent connections list and "Manage Connections" button
 *
 * @param viewModel The main view model managing app state
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun StartScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val splitPaneState = rememberSplitPaneState(0.5f) // 50/50 split
    val recentConnections = viewModel.getRecentConnections(3)
    val appState by viewModel.state.collectAsState()

    var licenseKey by remember { mutableStateOf(viewModel.getSavedLicenseKey() ?: "") }
    val accessStatus = appState.accessStatus
    val hasAccess = accessStatus?.hasAccess() == true

    // Custom gradient style for "Moribito" title
    val moribitoGradient = Brush.linearGradient(
        colors = listOf(AppColors.red100, AppColors.orange),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val moribitoStyle = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        brush = moribitoGradient
    )

    HorizontalSplitPane(
        splitPaneState = splitPaneState,
        modifier = modifier.fillMaxSize()
    ) {
        // Left Panel: Branding
        first(minSize = 400.dp) {
            Island(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = AppSpacing.md, end = AppSpacing.xs, bottom = AppSpacing.md)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = AppSpacing.lg)
                    ) {
                        Text(
                            text = "Moribito",
                            style = moribitoStyle
                        )
                        Text(
                            text = "An LDAP viewer"
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xxxl))

                        Column(
                            modifier = Modifier.width(300.dp),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            AppTextField(
                                value = licenseKey,
                                onValueChange = {
                                    licenseKey = it
                                },
                                label = "License Key",
                                placeholder = "Paste your license key here"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            Desktop.getDesktop().browse(URI("https://buy.stripe.com/test_eVq8wOcxabMB2nk5VW3ZK00"))
                                        } catch (_: Exception) {
                                            // Silently fail if desktop browse is not supported
                                        }
                                    }
                                ) {
                                    Text("Buy License")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.verifyLicense(licenseKey)
                                    }
                                ) {
                                    Text("Verify License")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Panel: Recent Connections
        second(minSize = 400.dp) {
            Island(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = AppSpacing.xs, end = AppSpacing.md, bottom = AppSpacing.md)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Top-right: Manage Connections button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo(AppView.Configuration)
                            },
                            enabled = hasAccess
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    key = AllIconsKeys.General.Settings,
                                    contentDescription = "Plug",
                                    modifier = Modifier.size(14.dp),
                                )
                                Text("Manage Connections")
                            }
                        }
                    }

                    // Centered connection list
                    Spacer(modifier = Modifier.weight(1f))

                    if (recentConnections.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                            Text(
                                text = "No connections configured"
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.navigateTo(AppView.Configuration)
                                },
                                enabled = hasAccess
                            ) {
                                Text("Add Connection")
                            }
                        }
                    } else {
                        // Connection cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            recentConnections.forEachIndexed { index, connection ->
                                key(connection.host + connection.name) {
                                    ConnectionCard(
                                        name = connection.name.ifBlank { connection.host },
                                        host = connection.host,
                                        onClick = {
                                            viewModel.setCurrentConnection(index)
                                            viewModel.connect()
                                        },
                                        loading = appState.loadingState is LoadingState.Loading && appState.currentConnectionIndex == index,
                                        enabled = hasAccess,
                                        modifier = Modifier.width(400.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
