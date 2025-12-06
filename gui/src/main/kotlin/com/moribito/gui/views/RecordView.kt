package com.moribito.gui.views

import com.moribito.gui.theme.MoribitoTheme
import com.moribito.ldap.Entry
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.layout.constraints.*

/**
 * Record view for displaying LDAP entry details.
 */
class RecordView(
    private val theme: MoribitoTheme,
    private val entry: Entry?
) : View() {

    init {
        if (entry != null) {
            val dnLabel = Label("DN: ${entry.dn}").apply {
                font = theme.fonts.subheading
                foregroundColor = theme.colors.primaryBlue
            }

            val attrLabel = Label("Attributes: ${entry.attributes.size}").apply {
                font = theme.fonts.body
                foregroundColor = theme.colors.textPrimary
            }

            children += dnLabel
            children += attrLabel

            layout = constrain(dnLabel, attrLabel) { dn, attr ->
                dn.left eq 50
                dn.top eq 50

                attr.left eq 50
                attr.top eq dn.bottom + 20
            }
        } else {
            children += Label("No entry selected").apply {
                font = theme.fonts.body
                foregroundColor = theme.colors.textSecondary
            }
        }
    }

    override fun render(canvas: Canvas) {
        canvas.rect(bounds.atOrigin, theme.colors.backgroundWhite.paint)
        super.render(canvas)
    }
}
