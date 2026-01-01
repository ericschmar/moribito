package com.moribito.gui.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI
import org.jetbrains.jewel.ui.component.Dropdown
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.painter.hints.Size
import org.jetbrains.jewel.window.DecoratedWindowScope
import org.jetbrains.jewel.window.TitleBar
import org.jetbrains.jewel.window.newFullscreenControls

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moribito.gui.ui.components.Identicon
import com.moribito.gui.viewmodel.ConnectionState
import com.moribito.gui.viewmodel.MainViewModel
import compose.icons.Octicons
import compose.icons.octicons.ChevronDown16
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.intui.standalone.styling.fullWidth
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class, ExperimentalJewelApi::class)
@ExperimentalLayoutApi
@Composable
internal fun DecoratedWindowScope.TitleBarView() {
    val viewModel: MainViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    TitleBar(Modifier.newFullscreenControls()) {
        Row(Modifier.align(Alignment.Start)) {
            if (state.connectionState is ConnectionState.Connected) {
                val currentConn = viewModel.getCurrentConnection()
                Dropdown(
                    modifier = Modifier.height(30.dp),
                    menuContent = {
                        currentConn.effectiveBindCredentials.forEach { cred ->
                            selectableItem(
                                selected = state.currentCredential?.id == cred.id,
                                onClick = {
                                    if (state.currentCredential?.id != cred.id) {
                                        viewModel.connect(cred)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Identicon(cred.bindUser, size = 14.dp)
                                    Text("${currentConn.name} (${cred.label})", fontSize = 12.sp)
                                }
                            }
                        }

                        selectableItem(
                            selected = false,
                            onClick = { viewModel.openConfigurationWindow() }
                        ) {
                            Text("Add DN...", fontSize = 12.sp)
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        state.currentCredential?.let { cred ->
                            Identicon(cred.bindUser, size = 14.dp)
                            Text("${currentConn.name} (${cred.label})", fontSize = 12.sp)
                        } ?: run {
                            Text(currentConn.name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Row(Modifier.align(Alignment.End)) {
            // end
        }
    }
}