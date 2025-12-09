package com.moribito.gui

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Moribito - LDAP Explorer"
    ) {
        App()
    }
}
