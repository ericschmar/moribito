package com.moribito.gui.components

import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.viewmodel.AppState
import com.moribito.gui.viewmodel.LoadingState
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.layout.constraints.*

/**
 * Status bar component displayed at the bottom of the application.
 *
 * Shows connection status and current operation/loading state.
 */
class StatusBar(
    private val theme: MoribitoTheme,
    private val state: AppState
) : View() {

    private val connectionIndicator = ConnectionIndicator(theme, state.connectionState)

    private val loadingLabel = Label().apply {
        font = theme.fonts.caption
        foregroundColor = when (state.loadingState) {
            is LoadingState.Success -> theme.colors.success
            is LoadingState.Failed -> theme.colors.error
            is LoadingState.Loading -> theme.colors.info
            is LoadingState.Idle -> theme.colors.textSecondary
        }

        text = when (val loading = state.loadingState) {
            is LoadingState.Loading -> loading.operation
            is LoadingState.Success -> loading.message ?: "Ready"
            is LoadingState.Failed -> loading.error
            is LoadingState.Idle -> "Ready"
        }
    }

    init {
        size = Size(800.0, 32.0)
        backgroundColor = theme.colors.backgroundLight

        children += connectionIndicator
        children += loadingLabel

        layout = constrain(connectionIndicator, loadingLabel) { indicator, loading ->
            indicator.left eq parent.left + theme.spacing.MEDIUM
            indicator.centerY eq parent.centerY

            loading.left eq indicator.right + theme.spacing.XLARGE
            loading.centerY eq parent.centerY
        }
    }

    override fun render(canvas: Canvas) {
        // Draw top border
        canvas.rect(
            Rectangle(0.0, 0.0, width, theme.spacing.BORDER_THIN),
            theme.colors.border.paint
        )

        super.render(canvas)
    }
}
