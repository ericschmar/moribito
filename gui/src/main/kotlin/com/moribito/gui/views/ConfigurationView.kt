package com.moribito.gui.views

import com.moribito.config.Config
import com.moribito.gui.theme.MoribitoTheme
import com.moribito.gui.viewmodel.MainViewModel
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.controls.text.TextField
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.layout.constraints.*

/**
 * Configuration view for setting up LDAP connection parameters.
 */
class ConfigurationView(
    private val theme: MoribitoTheme,
    private val viewModel: MainViewModel,
    private val initialConfig: Config
) : View() {

    private val titleLabel = Label("LDAP Connection Configuration").apply {
        font = theme.fonts.heading
        foregroundColor = theme.colors.textPrimary
    }

    private val hostField = TextField().apply {
        text = initialConfig.ldap.host
        font = theme.fonts.body
    }

    private val portField = TextField().apply {
        text = initialConfig.ldap.port.toString()
        font = theme.fonts.body
    }

    private val baseDNField = TextField().apply {
        text = initialConfig.ldap.baseDN
        font = theme.fonts.body
    }

    private val bindUserField = TextField().apply {
        text = initialConfig.ldap.bindUser
        font = theme.fonts.body
    }

    private val bindPassField = TextField().apply {
        text = initialConfig.ldap.bindPass
        font = theme.fonts.body
    }

    private val connectButton = PushButton("Connect to LDAP").apply {
        font = theme.fonts.bodyBold
        backgroundColor = theme.colors.primaryBlue
        foregroundColor = theme.colors.textLight

        fired += {
            handleConnect()
        }
    }

    init {
        children += titleLabel
        children += Label("Host:").apply { font = theme.fonts.body }
        children += hostField
        children += Label("Port:").apply { font = theme.fonts.body }
        children += portField
        children += Label("Base DN:").apply { font = theme.fonts.body }
        children += baseDNField
        children += Label("Bind User:").apply { font = theme.fonts.body }
        children += bindUserField
        children += Label("Password:").apply { font = theme.fonts.body }
        children += bindPassField
        children += connectButton

        // Simple top-to-bottom layout
        layout = constrain(
            titleLabel,
            children[1],  // Host label
            hostField,
            children[3],  // Port label
            portField,
            children[5],  // Base DN label
            baseDNField,
            children[7],  // Bind User label
            bindUserField,
            children[9],  // Password label
            bindPassField,
            connectButton
        ) { title, hostLbl, host, portLbl, port, dnLbl, dn, userLbl, user, passLbl, pass, btn ->
            val leftMargin = 50.0
            val topStart = 50.0
            val fieldWidth = 400.0
            val spacing = 40.0
            var y = topStart

            title.left eq leftMargin
            title.top eq y
            y += spacing

            hostLbl.left eq leftMargin
            hostLbl.top eq y
            host.left eq leftMargin + 120
            host.top eq y
            host.width eq fieldWidth
            y += spacing

            portLbl.left eq leftMargin
            portLbl.top eq y
            port.left eq leftMargin + 120
            port.top eq y
            port.width eq fieldWidth
            y += spacing

            dnLbl.left eq leftMargin
            dnLbl.top eq y
            dn.left eq leftMargin + 120
            dn.top eq y
            dn.width eq fieldWidth
            y += spacing

            userLbl.left eq leftMargin
            userLbl.top eq y
            user.left eq leftMargin + 120
            user.top eq y
            user.width eq fieldWidth
            y += spacing

            passLbl.left eq leftMargin
            passLbl.top eq y
            pass.left eq leftMargin + 120
            pass.top eq y
            pass.width eq fieldWidth
            y += spacing

            btn.left eq leftMargin + 120
            btn.top eq y
            btn.width eq 200
        }
    }

    private fun handleConnect() {
        val port = portField.text.toIntOrNull() ?: 389

        viewModel.updateConfig(
            host = hostField.text,
            port = port,
            baseDN = baseDNField.text,
            useSSL = false,  // TODO: Add checkboxes
            useTLS = false,
            bindUser = bindUserField.text,
            bindPass = bindPassField.text
        )

        viewModel.connect()
    }

    override fun render(canvas: Canvas) {
        canvas.rect(bounds.atOrigin, theme.colors.backgroundWhite.paint)
        super.render(canvas)
    }
}
