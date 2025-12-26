package com.moribito.gui

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import com.moribito.gui.ui.components.Background
import com.moribito.gui.view.TitleBarView
import com.moribito.gui.viewmodel.MainViewModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToSvgPainter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.util.JewelLogger
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.createEditorTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.intui.window.styling.lightWithLightHeader
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarMetrics
import org.jetbrains.jewel.window.styling.TitleBarStyle

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
fun main() = application {
    val textStyle = JewelTheme.createDefaultTextStyle()
    val editorStyle = JewelTheme.createEditorTextStyle()

    val themeDefinition = JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle, editorTextStyle = editorStyle)

    CompositionLocalProvider(
        LocalContentColor provides AppColors.textPrimary,
        LocalTextStyle provides AppTypography.bodyMedium,
        LocalIndication provides NoIndication
    ) {
        IntUiTheme(
            theme = themeDefinition,
            styling =
                ComponentStyling.default()
                    .decoratedWindow(
                        titleBarStyle = TitleBarStyle.dark(
                            colors = TitleBarColors(
                                background = AppColors.colorScheme.surface,
                                inactiveBackground = AppColors.colorScheme.surface,
                                content = AppColors.colorScheme.inverseSurface,
                                border = AppColors.colorScheme.surface,
                                fullscreenControlButtonsBackground = AppColors.colorScheme.surface,
                                titlePaneButtonHoveredBackground = AppColors.colorScheme.surface,
                                titlePaneButtonPressedBackground = AppColors.colorScheme.surface,
                                titlePaneCloseButtonHoveredBackground = AppColors.colorScheme.surface,
                                titlePaneCloseButtonPressedBackground = AppColors.colorScheme.surface,
                                iconButtonHoveredBackground = AppColors.colorScheme.surface,
                                iconButtonPressedBackground = AppColors.colorScheme.surface,
                                dropdownPressedBackground = AppColors.colorScheme.surface,
                                dropdownHoveredBackground = AppColors.colorScheme.surface
                            ),
                            metrics = TitleBarMetrics(
                                28.dp,
                                0.dp,
                                0.dp,
                                DpSize(18.dp, 18.dp)
                            )
                        )
                    ),
        ) {
            DecoratedWindow(
                onCloseRequest = { exitApplication() },
                title = "Moribito",
                content = {
                    Background(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TitleBarView()
                            App()
                        }
                    }
                },
            )
        }
    }
}