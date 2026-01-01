package com.moribito.gui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.WindowState
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.ui.components.Background
import com.moribito.gui.ui.screens.ConfigurationScreen
import com.moribito.gui.ui.screens.StartScreen
import com.moribito.gui.ui.screens.WorkspaceScreen
import com.moribito.gui.viewmodel.AppView
import com.moribito.gui.viewmodel.MainViewModel
import io.github.composefluent.component.*
import com.moribito.gui.viewmodel.ConnectionState
import org.koin.compose.koinInject

@Composable
fun FrameWindowScope.App(windowState: WindowState) {
    val viewModel: MainViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.currentView) {
        when (state.currentView) {
            is AppView.Start, is AppView.Configuration -> {
                windowState.size = DpSize(900.dp, 700.dp)
            }
            is AppView.Workspace -> {
                windowState.size = DpSize(1280.dp, 960.dp)
            }
        }
    }

    MenuBar {
        Menu("Connections") {
            Item("Manage Connections", onClick = {
                viewModel.navigateTo(AppView.Configuration)
            })
            Separator()
            Item("Disconnect", enabled = state.connectionState == ConnectionState.Connected, onClick = {
                viewModel.disconnect()
            })
            Item("Reconnect", enabled = state.connectionState == ConnectionState.Connected, onClick = {
                viewModel.reconnect()
            })
        }
        Menu("Settings") {
            Item("Settings", enabled = false, onClick = {})
        }
    }

    when (state.currentView) {
        is AppView.Start -> {
            StartScreen(
                viewModel = viewModel
            )
        }

        is AppView.Configuration -> {
            ConfigurationScreen(
                viewModel = viewModel,
                ldapConfig = viewModel.getConfig(),
                connectionState = state.connectionState
            )
        }

        is AppView.Workspace -> {
            WorkspaceScreen(
                viewModel = viewModel,
                state = state
            )
        }
    }
}
