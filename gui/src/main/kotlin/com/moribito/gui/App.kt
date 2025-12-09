package com.moribito.gui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moribito.config.Config
import com.moribito.gui.ui.screens.ConfigurationScreen
import com.moribito.gui.viewmodel.MainViewModel
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.Mica
import io.github.composefluent.component.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun App() {
    // Load config and create ViewModel
    var viewModel by remember { mutableStateOf<MainViewModel?>(null) }
    var configLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val config = withContext(Dispatchers.IO) {
                try {
                    Config.load().first
                } catch (e: Exception) {
                    // Create default config if none exists
                    val defaultPath = Config.createDefault()
                    println("Created default config at: $defaultPath")
                    Config.load().first
                }
            }
            viewModel = MainViewModel(config)
        } catch (e: Exception) {
            configLoadError = "Failed to load configuration: ${e.message}"
        }
    }

    FluentTheme {
        when {
            configLoadError != null -> {
                // Show error screen
                Mica(modifier = Modifier.fillMaxSize()) {
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
                Mica(modifier = Modifier.fillMaxSize()) {
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

                ConfigurationScreen(
                    viewModel = vm,
                    ldapConfig = vm.getConfig().ldap,
                    connectionState = state.connectionState
                )
            }
        }
    }
}
