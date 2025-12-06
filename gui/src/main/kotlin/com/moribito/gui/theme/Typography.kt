package com.moribito.gui.theme

import io.nacular.doodle.drawing.Font
import io.nacular.doodle.drawing.FontLoader

/**
 * Typography system for Moribito GUI application.
 *
 * Defines standard font styles used throughout the application.
 */
object Typography {
    // Font families
    const val FAMILY_SANS = "Inter, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif"
    const val FAMILY_MONO = "JetBrains Mono, Consolas, Monaco, Courier New, monospace"

    // Font sizes
    const val SIZE_TITLE = 24
    const val SIZE_HEADING = 18
    const val SIZE_SUBHEADING = 16
    const val SIZE_BODY = 14
    const val SIZE_CAPTION = 12
    const val SIZE_SMALL = 11

    /**
     * Loads all fonts asynchronously.
     * Must be called from a coroutine context.
     */
    suspend fun loadFonts(fontLoader: FontLoader): LoadedFonts {
        return LoadedFonts(
            title = fontLoader {
                size = SIZE_TITLE
                weight = 700
                families = listOf(FAMILY_SANS)
            },
            heading = fontLoader {
                size = SIZE_HEADING
                weight = 700
                families = listOf(FAMILY_SANS)
            },
            subheading = fontLoader {
                size = SIZE_SUBHEADING
                weight = 600
                families = listOf(FAMILY_SANS)
            },
            body = fontLoader {
                size = SIZE_BODY
                weight = 400
                families = listOf(FAMILY_SANS)
            },
            bodyBold = fontLoader {
                size = SIZE_BODY
                weight = 700
                families = listOf(FAMILY_SANS)
            },
            caption = fontLoader {
                size = SIZE_CAPTION
                weight = 400
                families = listOf(FAMILY_SANS)
            },
            small = fontLoader {
                size = SIZE_SMALL
                weight = 400
                families = listOf(FAMILY_SANS)
            },
            code = fontLoader {
                size = 13
                weight = 400
                families = listOf(FAMILY_MONO)
            }
        )
    }
}

/**
 * Container for loaded fonts.
 */
data class LoadedFonts(
    val title: Font?,
    val heading: Font?,
    val subheading: Font?,
    val body: Font?,
    val bodyBold: Font?,
    val caption: Font?,
    val small: Font?,
    val code: Font?
)
