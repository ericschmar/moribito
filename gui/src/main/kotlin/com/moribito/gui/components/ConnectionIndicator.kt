package com.moribito.gui.components

import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.viewmodel.ConnectionState
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.core.view
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.layout.constraints.*

/**
 * Component that displays the current LDAP connection status.
 *
 * Shows a colored indicator dot and status text.
 */
class ConnectionIndicator(
    private val theme: MoribitoTheme,
    private val state: ConnectionState
) : View() {

    private val statusDot = object : View() {
        init {
            suggestSize(12.0, 12.0)
        }

        override fun render(canvas: Canvas) {
            val color = when (state) {
                is ConnectionState.Connected -> theme.colors.success
                is ConnectionState.Connecting -> theme.colors.warning
                is ConnectionState.Error -> theme.colors.error
                is ConnectionState.Disconnected -> theme.colors.disabledGray
            }

            canvas.circle(
                circle = Circle(io.nacular.doodle.geometry.Point(6.0, 6.0), radius = 5.0),
                fill = color.paint
            )
        }
    }

    private val statusLabel = Label().apply {
        font = theme.fonts.caption
        foregroundColor = when (state) {
            is ConnectionState.Connected -> theme.colors.success
            is ConnectionState.Connecting -> theme.colors.warning
            is ConnectionState.Error -> theme.colors.error
            is ConnectionState.Disconnected -> theme.colors.textSecondary
        }

        text = when (state) {
            is ConnectionState.Connected -> "Connected"
            is ConnectionState.Connecting -> "Connecting..."
            is ConnectionState.Error -> "Error: ${state.message}"
            is ConnectionState.Disconnected -> "Disconnected"
        }
    }

    init {
        size = Size(200.0, 24.0)

        children += statusDot
        children += statusLabel

        layout = constrain(statusDot, statusLabel) { dot, label ->
            dot.left eq parent.left + theme.spacing.SMALL
            dot.centerY eq parent.centerY

            label.left eq dot.right + theme.spacing.SMALL
            label.centerY eq parent.centerY
        }
    }
}
