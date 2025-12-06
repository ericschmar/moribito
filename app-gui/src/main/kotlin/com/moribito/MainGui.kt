package com.moribito

import com.moribito.config.Config
import com.moribito.gui.MoribitoGuiApp
import com.moribito.gui.theme.Typography
import io.nacular.doodle.application.application
import io.nacular.doodle.drawing.FontLoader
import kotlinx.coroutines.runBlocking

/**
 * Entry point for Moribito GUI application.
 */
fun main(args: Array<String>) {
    println("Moribito GUI - LDAP Explorer v2.0.0")
    println("Loading configuration...")

    // Load configuration
    val config = runBlocking {
        try {
            val (cfg, path) = Config.load()
            println("Loaded configuration from: $path")
            cfg
        } catch (e: Exception) {
            println("Warning: Could not load config file: ${e.message}")
            println("Using default configuration")
            createDefaultConfig()
        }
    }

    println("Starting GUI application...")

    // Launch Doodle application
    application {
        val fontLoader: FontLoader = instance()
        val display = instance()

        // Load fonts before creating app
        println("Loading fonts...")
        val fonts = runBlocking {
            Typography.loadFonts(fontLoader)
        }

        println("Fonts loaded, initializing application...")
        MoribitoGuiApp(display, config, fonts)
    }
}

/**
 * Creates a default configuration.
 */
private fun createDefaultConfig(): Config {
    return Config(
        ldap = com.moribito.config.LdapConfig(
            host = "localhost",
            port = 389,
            baseDN = "dc=example,dc=com",
            useSSL = false,
            useTLS = false,
            bindUser = "",
            bindPass = ""
        )
    )
}
