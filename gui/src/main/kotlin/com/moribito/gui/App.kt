package com.moribito.gui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moribito.gui.ui.components.Background
import com.moribito.gui.ui.screens.ConfigurationScreen
import com.moribito.gui.ui.screens.StartScreen
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
import org.koin.compose.koinInject

@Composable
fun App() {
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

        when {
            configLoadError != null -> {
                // Show error screen
                Background(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(text = "Error: $configLoadError")
                    }
                }
            }
            viewModel == null -> {
                // Show loading screen
                Background(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ProgressRing()
                    }
                }
            }
            else -> {
                // Show main app
                val vm = viewModel!!
                val state by vm.state.collectAsState()

                Background(modifier = Modifier.fillMaxSize()) {
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
                        is AppView.Tree -> {
                            // TODO: Implement TreeScreen
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tree View - Not Implemented")
                            }
                        }
                        is AppView.Record -> {
                            // TODO: Implement RecordScreen
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Record View - Not Implemented")
                            }
                        }
                        is AppView.Query -> {
                            // TODO: Implement QueryScreen
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Query View - Not Implemented")
                            }
                        }
                    }
                }
            }
        }
    }
}
