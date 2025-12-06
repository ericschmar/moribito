package com.moribito.gui.views

import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.viewmodel.MainViewModel
import com.moribito.ldap.TreeNode
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint

/**
 * Tree view for browsing the LDAP directory structure.
 *
 * TODO: Implement proper tree rendering with Doodle's tree control
 */
class TreeView(
    private val theme: MoribitoTheme,
    private val viewModel: MainViewModel,
    private val rootNode: TreeNode?
) : View() {

    init {
        val label = if (rootNode != null) {
            Label("Tree view for: ${rootNode.dn}").apply {
                font = theme.fonts.body
                foregroundColor = theme.colors.textPrimary
            }
        } else {
            Label("No tree data available").apply {
                font = theme.fonts.body
                foregroundColor = theme.colors.textSecondary
            }
        }

        children += label
    }

    override fun render(canvas: Canvas) {
        canvas.rect(bounds.atOrigin, theme.colors.backgroundWhite.paint)
        super.render(canvas)
    }
}
