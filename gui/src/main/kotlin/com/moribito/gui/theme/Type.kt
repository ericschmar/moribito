package com.moribito.gui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Application typography system.
 *
 * Font families:
 * - UI text: System sans-serif (clean, readable)
 * - Code/monospace: System monospace (or JetBrains Mono if bundled)
 *
 * Characteristics:
 * - Compact line heights (1.2-1.4 vs Material's 1.5)
 * - Slightly smaller sizes than Material defaults
 * - Clear hierarchy with appropriate weights
 */

/**
 * Main font families used throughout the app.
 */
object AppFonts {
    /**
     * Sans-serif font for UI elements.
     * Uses system default sans-serif which adapts to platform:
     * - macOS: SF Pro
     * - Windows: Segoe UI
     * - Linux: System default
     */
    val sansSerif = FontFamily.SansSerif

    /**
     * Monospace font for code and technical content.
     * Uses system monospace which adapts to platform.
     *
     * TODO: Consider bundling JetBrains Mono for consistent appearance:
     * 1. Download JetBrains Mono from https://www.jetbrains.com/lp/mono/
     * 2. Add TTF files to gui/src/main/resources/fonts/
     * 3. Load with: FontFamily(Font(resource = "fonts/JetBrainsMono-Regular.ttf"))
     */
    val monospace = FontFamily.Monospace
}

/**
 * Material 3 Typography configured with compact, information-dense style.
 *
 * Text style hierarchy:
 * - Display: Large page titles (rarely used)
 * - Headline: Section headers
 * - Title: Prominent titles, card headers
 * - Body: Main content text
 * - Label: Button text, form labels, captions
 */
val AppTypography = Typography(
    // Display styles - Large page titles (rarely used)
    displayLarge = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles - Section headers
    headlineLarge = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),

    // Title styles - Prominent titles, card headers
    titleLarge = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),

    // Body styles - Main content text
    bodyLarge = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),

    // Label styles - Button text, form labels, captions
    labelLarge = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFonts.sansSerif,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Monospace text styles for code, technical content, and LDAP DNs.
 * Use these when displaying code, DNS, file paths, or other technical strings.
 */
object AppMonospace {
    /** Large monospace text - 14sp */
    val large = TextStyle(
        fontFamily = AppFonts.monospace,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )

    /** Medium monospace text - 13sp - Standard for code blocks */
    val medium = TextStyle(
        fontFamily = AppFonts.monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )

    /** Small monospace text - 12sp - For compact code displays */
    val small = TextStyle(
        fontFamily = AppFonts.monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )

    /** Small monospace text - 12sp - For compact code displays */
    val xsmall = TextStyle(
        fontFamily = AppFonts.monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )

    /** Bold monospace - For emphasis in code */
    val bold = TextStyle(
        fontFamily = AppFonts.monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    )
}

/**
 * Usage guide for common scenarios:
 *
 * Screen titles:
 * - Text("LDAP Configuration", style = AppTypography.titleMedium)
 *
 * Section headers:
 * - Text("Connection Settings", style = AppTypography.headlineSmall)
 *
 * Body text:
 * - Text("Enter your LDAP server details", style = AppTypography.bodyMedium)
 *
 * Form labels:
 * - Text("Host:", style = AppTypography.labelMedium)
 *
 * Button text:
 * - Button(...) { Text("Connect", style = AppTypography.labelLarge) }
 *
 * Technical values (DNs, paths):
 * - Text("cn=admin,dc=example,dc=com", style = AppMonospace.medium)
 *
 * Code snippets:
 * - Text("objectClass: person", style = AppMonospace.medium)
 */
