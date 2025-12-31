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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.moribito.config.ConfigurationService
import LdapConfig
import com.moribito.gui.viewmodel.ConnectionState
import org.koin.compose.koinInject

@Composable
fun FrameWindowScope.App(windowState: WindowState) {
    KoinApplication(application = {
        modules(
            module {
                singleOf(::ConfigurationService)
            }
        )
    }) {
        // Load config and create ViewModel
        var viewModel by remember { mutableStateOf<MainViewModel?>(null) }
        var configLoadError by remember { mutableStateOf<String?>(null) }

        val configService = koinInject<ConfigurationService>()
        LaunchedEffect(Unit) {
            try {
                val config = configService.load()
                viewModel = MainViewModel(config)
            } catch (e: Exception) {
                configLoadError = "Failed to load configuration: ${e.message}"
            }
        }

        if (viewModel != null) {
            val vm = viewModel!!
            val state by vm.state.collectAsState()

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
                        vm.navigateTo(AppView.Configuration)
                    })
                    Separator()
                    Item("Disconnect", enabled = state.connectionState == ConnectionState.Connected, onClick = {
                        vm.disconnect()
                    })
                    Item("Reconnect", enabled = state.connectionState == ConnectionState.Connected, onClick = {
                        vm.reconnect()
                    })
                }
                Menu("Settings") {
                    Item("Settings", enabled = false, onClick = {})
                }
            }
        }

        when {
            configLoadError != null -> {
                // Show error screen
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "Error: $configLoadError")
                }
            }

            viewModel == null -> {
                // Show loading screen
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ProgressRing()
                }
            }

            else -> {
                // Show main app
                val vm = viewModel!!
                val state by vm.state.collectAsState()

                when (state.currentView) {
                    is AppView.Start -> {
                        StartScreen(
                            viewModel = vm
                        )
                    }

                    is AppView.Configuration -> {
                        ConfigurationScreen(
                            viewModel = vm,
                            ldapConfig = vm.getConfig(),
                            connectionState = state.connectionState
                        )
                    }

                    is AppView.Workspace -> {
                        WorkspaceScreen(
                            viewModel = vm,
                            state = state
                        )
                    }
                }
            }
        }
    }
}
