# Custom Fonts for Moribito GUI

This directory contains custom TrueType font files for the Moribito LDAP Browser.

## Current Configuration

The application is configured to use the following fonts:
- **Inter** - Primary UI font for regular text
- **JetBrains Mono** - Monospace font for code and data display

## Font Usage

Fonts can be provided in two ways:

### 1. System Fonts (Development)

During development, the application will use system-installed fonts if available:
- Install "Inter" font from https://rsms.me/inter/
- Install "JetBrains Mono" font from https://www.jetbrains.com/lp/mono/

### 2. Bundled Fonts (Production)

For production distribution, place TTF font files in this directory:
- `inter-regular.ttf`
- `inter-bold.ttf`
- `jetbrainsmono-regular.ttf`
- `jetbrainsmono-bold.ttf`

Fonts are configured in `themes/gruvbox.json` under the `font` section for each theme variant.

## Adding Custom Fonts

To use different fonts:

1. Place the TTF files in this directory
2. Update `themes/gruvbox.json` to reference the new font families
3. Rebuild the application

Font family names in the theme JSON should match the PostScript name or full font name as registered with the system or font library.
