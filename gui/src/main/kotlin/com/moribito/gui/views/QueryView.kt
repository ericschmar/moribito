package com.moribito.gui.views

import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.viewmodel.MainViewModel
import com.moribito.ldap.Entry
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.controls.text.TextField
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.layout.constraints.*

/**
 * Query view for executing custom LDAP searches.
 */
class QueryView(
    private val theme: MoribitoTheme,
    private val viewModel: MainViewModel,
    private val queryText: String,
    private val results: List<Entry>
) : View() {

    private val queryField = TextField().apply {
        text = queryText
        font = theme.fonts.body
    }

    private val executeButton = PushButton("Execute Query").apply {
        font = theme.fonts.bodyBold
        backgroundColor = theme.colors.primaryBlue
        foregroundColor = theme.colors.textLight

        fired += {
            viewModel.executeQuery(queryField.text)
        }
    }

    private val resultsLabel = Label("Results: ${results.size} entries found").apply {
        font = theme.fonts.bodyBold
        foregroundColor = theme.colors.textPrimary
    }

    init {
        children += Label("LDAP Query").apply {
            font = theme.fonts.heading
            foregroundColor = theme.colors.textPrimary
        }
        children += Label("Filter:").apply {
            font = theme.fonts.body
        }
        children += queryField
        children += executeButton
        children += resultsLabel

        layout = constrain(
            children[0],  // Title
            children[1],  // Filter label
            queryField,
            executeButton,
            resultsLabel
        ) { title, filterLbl, query, btn, results ->
            title.left eq 50
            title.top eq 50

            filterLbl.left eq 50
            filterLbl.top eq title.bottom + 30

            query.left eq 120
            query.top eq title.bottom + 30
            query.width eq 400

            btn.left eq query.right + 20
            btn.top eq title.bottom + 30

            results.left eq 50
            results.top eq query.bottom + 30
        }
    }

    override fun render(canvas: Canvas) {
        canvas.rect(bounds.atOrigin, theme.colors.backgroundWhite.paint)
        super.render(canvas)
    }
}
