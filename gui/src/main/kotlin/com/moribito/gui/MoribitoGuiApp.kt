package com.moribito.gui

import com.moribito.config.Config
import com.moribito.gui.components.StatusBar
import com.moribito.gui.theme.LoadedFonts
import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.theme.Typography
import com.moribito.gui.viewmodel.AppView
import com.moribito.gui.viewmodel.MainViewModel
import com.moribito.gui.views.ConfigurationView
import com.moribito.gui.views.QueryView
import com.moribito.gui.views.RecordView
import com.moribito.gui.views.TreeView
import io.nacular.doodle.application.Application
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.View
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Main Doodle application for Moribito LDAP Explorer.
 *
 * Manages the application lifecycle, view navigation, and state updates.
 */
class MoribitoGuiApp(
    private val display: Display,
    private val config: Config,
    private val fonts: LoadedFonts
) : Application {

    private lateinit var viewModel: MainViewModel
    private lateinit var theme: MoribitoTheme
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var mainView: MutableContainerView

    private inner class MutableContainerView : View() {
        fun updateContent(state: com.moribito.gui.viewmodel.AppState) {
            children.batch {
                clear()

                // Tab bar
                val tabBar = createTabBar(state)
                add(tabBar)

                // Content area
                val contentView = when (state.currentView) {
                    AppView.Configuration -> ConfigurationView(theme, viewModel, config)
                    AppView.Tree -> TreeView(theme, viewModel, state.treeRoot)
                    AppView.Record -> RecordView(theme, state.selectedEntry)
                    AppView.Query -> QueryView(theme, viewModel, state.queryText, state.queryResults)
                }

                contentView.suggestBounds(0.0, 50.0, 1024.0, 686.0)
                add(contentView)

                // Status bar
                val statusBar = StatusBar(theme, state)
                statusBar.suggestBounds(0.0, 736.0, 1024.0, 32.0)
                add(statusBar)
            }

            rerender()
        }
    }

    init {
        theme = MoribitoTheme(fonts)
        viewModel = MainViewModel(config)

        // Create and add main view to display
        mainView = createMainView()
        display += mainView

        // Observe state changes
        observeStateChanges()
    }

    override fun shutdown() {
        viewModel.cleanup()
        scope.cancel()
    }

    private fun createMainView(): MutableContainerView {
        return MutableContainerView().apply {
            suggestSize(1024.0, 768.0)
            backgroundColor = theme.colors.backgroundLight

            // Initial content
            updateContent(viewModel.state.value)
        }
    }

    private fun observeStateChanges() {
        viewModel.state.onEach { state ->
            mainView.updateContent(state)
        }.launchIn(scope)
    }

    private fun createTabBar(state: com.moribito.gui.viewmodel.AppState): View {
        return object : View() {
            init {
                suggestBounds(0.0, 0.0, 1024.0, 50.0)
                backgroundColor = theme.colors.backgroundDark

                val tabs = listOf(
                    "Configuration" to AppView.Configuration,
                    "Tree" to AppView.Tree,
                    "Record" to AppView.Record,
                    "Query" to AppView.Query
                )

                tabs.forEachIndexed { index, (label, targetView) ->
                    val isActive = state.currentView == targetView
                    val tabButton = PushButton(label).apply {
                        font = theme.fonts.bodyBold
                        suggestBounds(index * 150.0, 0.0, 150.0, 50.0)
                        backgroundColor = if (isActive) theme.colors.primaryBlue else theme.colors.backgroundDark
                        foregroundColor = theme.colors.textLight

                        fired += {
                            viewModel.navigateTo(targetView)
                        }
                    }

                    children += tabButton
                }
            }
        }
    }
}
