package com.moribito.gui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.Background
import com.moribito.gui.view.TitleBarView
import com.moribito.gui.view.ConfigurationWindow
import com.moribito.gui.view.LogViewerWindow
import com.moribito.logging.Logger
import com.moribito.logging.LogLevel
import java.io.File
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
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.moribito.config.ConfigurationService
import com.moribito.gui.ui.icons.AppIcons
import com.moribito.gui.viewmodel.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

object NoIndication : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return NoIndicationNode()
    }

    // This is the actual "worker" that handles the drawing
    private class NoIndicationNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            // We only call drawContent() and NOTHING else.
            // This effectively skips drawing any ripple or highlight.
            drawContent()
        }
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@OptIn(ExperimentalLayoutApi::class)
fun main() {
    // Initialize logger before anything else
    val logDirectory = File(System.getProperty("user.home"), ".moribito/logs")
    Logger.initialize(logDirectory, LogLevel.DEBUG)

    System.setProperty("apple.awt.application.name", "Moribito")
    application {
        KoinApplication(application = {
            modules(
                module {
                    singleOf(::ConfigurationService)
                    singleOf(::AppStateHolder)
                    singleOf(::ConfigViewModel)
                    single {
                        val ldapViewModel: LdapViewModel = get()
                        QueryViewModel(
                            stateHolder = get(),
                            clientProvider = { ldapViewModel.getClient() }
                        )
                    }
                    singleOf(::LogViewModel)
                    singleOf(::LdapViewModel)
                    singleOf(::MainViewModel)
                }
            )
        }) {
            val textStyle = JewelTheme.createDefaultTextStyle()
            val editorStyle = JewelTheme.createEditorTextStyle()

            val themeDefinition = JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle, editorTextStyle = editorStyle)

            IntUiTheme(
                theme = themeDefinition,
                styling =
                    ComponentStyling.default()
                        .decoratedWindow(
                            titleBarStyle = TitleBarStyle.dark(
                                colors = TitleBarColors.dark(
                                    backgroundColor = IntelliJColors.baseBackground,
                                    borderColor = Color(0xFF2B2D30)
                                )
                            )
                        ),
            ) {
                CompositionLocalProvider(
                    LocalIndication provides NoIndication
                ) {
                    val viewModel: MainViewModel = koinInject()
                    val state by viewModel.state.collectAsState()

                    val windowState = rememberWindowState(width = 900.dp, height = 700.dp)
                    DecoratedWindow(
                        state = windowState,
                        onCloseRequest = { exitApplication() },
                        title = "Moribito",
                        icon = painterResource(AppIcons.moribitoIcon),
                        content = {
                            val decoratedWindowScope = this
                            TitleBarView()
                            Background(modifier = Modifier.fillMaxSize()) {
                                val windowScope = remember(decoratedWindowScope) {
                                    object : FrameWindowScope {
                                        override val window: ComposeWindow get() = decoratedWindowScope.window
                                    }
                                }
                                windowScope.App(windowState)
                            }
                        },
                    )

                    // Configuration window
                    if (state.isConfigurationWindowOpen) {
                        ConfigurationWindow(
                            viewModel = viewModel,
                            onCloseRequest = { viewModel.closeConfigurationWindow() }
                        )
                    }

                    // Log viewer window
                    if (state.isLogViewerOpen) {
                        LogViewerWindow(
                            viewModel = viewModel,
                            onCloseRequest = { viewModel.closeLogViewer() }
                        )
                    }
                }
            }
        }
    }

    // Shutdown logger on exit
    Logger.shutdown()
}
