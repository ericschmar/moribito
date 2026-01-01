package com.moribito.gui.view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Background
import com.moribito.gui.ui.screens.ConfigurationScreen
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.createEditorTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarStyle

/**
 * Configuration window that can be opened separately from the main window.
 * Shows the configuration screen for managing LDAP connections and credentials.
 */
@Composable
fun ConfigurationWindow(
    viewModel: MainViewModel,
    onCloseRequest: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val textStyle = JewelTheme.createDefaultTextStyle()
    val editorStyle = JewelTheme.createEditorTextStyle()

    val themeDefinition = JewelTheme.darkThemeDefinition(
        defaultTextStyle = textStyle,
        editorTextStyle = editorStyle
    )

    IntUiTheme(
        theme = themeDefinition,
        styling = ComponentStyling.default()
            .decoratedWindow(
                titleBarStyle = TitleBarStyle.dark(
                    colors = TitleBarColors.dark(
                        backgroundColor = IntelliJColors.islandBackground,
                        borderColor = Color(0xFF2B2D30)
                    )
                )
            ),
    ) {
        val windowState = rememberWindowState(width = 850.dp, height = 600.dp)
        DecoratedWindow(
            state = windowState,
            onCloseRequest = onCloseRequest,
            title = "Configuration - Moribito",
            content = {
                Background(modifier = Modifier.fillMaxSize()) {
                    ConfigurationScreen(
                        viewModel = viewModel,
                        ldapConfig = viewModel.getConfig(),
                        connectionState = state.connectionState
                    )
                }
            },
        )
    }
}
