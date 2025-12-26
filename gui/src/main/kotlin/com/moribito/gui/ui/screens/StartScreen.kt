package com.moribito.gui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.*
import com.moribito.gui.ui.components.ConnectionCard
import com.moribito.gui.viewmodel.AppView
import com.moribito.gui.viewmodel.MainViewModel
import compose.icons.Octicons
import compose.icons.octicons.Plug16
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import org.jetbrains.compose.splitpane.rememberSplitPaneState
import org.jetbrains.jewel.ui.component.OutlinedButton

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
        splitter {
            visiblePart {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(AppColors.border)
                )
            }
            handle {
                Box(
                    Modifier
                        .width(0.dp)
                        .fillMaxHeight()
                )
            }
        }

        // Left Panel: Branding
        first(minSize = 400.dp) {
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
                        text = "An LDAP viewer",
                        style = AppTypography.labelMedium,
                        color = AppColors.textPrimary
                    )
                }
            }
        }

        // Right Panel: Recent Connections
        second(minSize = 400.dp) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.lg)
            ) {
                // Top-right: Manage Connections button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.navigateTo(AppView.Configuration)
                        }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Octicons.Plug16,
                                contentDescription = "Plug",
                                modifier = Modifier.size(12.dp)
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
                            text = "No connections configured",
                            style = AppTypography.titleMedium,
                            color = AppColors.textSecondary
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.navigateTo(AppView.Configuration)
                            }
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
                            ConnectionCard(
                                name = connection.name.ifBlank { connection.host },
                                host = connection.host,
                                isSelected = (index == viewModel.getCurrentConnectionIndex()),
                                onClick = {
                                    viewModel.setCurrentConnection(index)
                                },
                                modifier = Modifier.width(400.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
