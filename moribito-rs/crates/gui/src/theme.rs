//! Theme and styling for the Moribito GUI
//!
//! This module defines the visual theme including colors, spacing,
//! typography, and other design tokens used throughout the application.

use gpui::Hsla;
use serde::{Deserialize, Serialize};

/// Font configuration for the theme
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FontConfig {
    pub family: String,
    pub mono_family: Option<String>,
    pub size: Option<f32>,
    pub mono_size: Option<f32>,
}

impl Default for FontConfig {
    fn default() -> Self {
        Self {
            family: "system-ui".to_string(),
            mono_family: Some("monospace".to_string()),
            size: None,
            mono_size: None,
        }
    }
}

impl FontConfig {
    pub fn from_json_value(value: &serde_json::Value) -> Option<Self> {
        match value {
            serde_json::Value::Object(map) => {
                let family = map
                    .get("family")
                    .and_then(|v| v.as_str())
                    .unwrap_or("system-ui")
                    .to_string();
                let mono_family = map
                    .get("mono_family")
                    .and_then(|v| v.as_str())
                    .map(|s| s.to_string());
                let size = map
                    .get("size")
                    .and_then(|v| v.as_f64())
                    .map(|s| s as f32);
                let mono_size = map
                    .get("mono_size")
                    .and_then(|v| v.as_f64())
                    .map(|s| s as f32);
                Some(Self {
                    family,
                    mono_family,
                    size,
                    mono_size,
                })
            }
            _ => None,
        }
    }
}

/// Application theme containing all design tokens
#[derive(Debug, Clone)]
pub struct Theme {
    pub colors: Colors,
    pub spacing: Spacing,
    pub typography: Typography,
    pub borders: Borders,
}

/// Color palette for the application
#[derive(Debug, Clone)]
pub struct Colors {
    // Background colors
    pub background: Hsla,
    pub surface: Hsla,
    pub surface_hover: Hsla,

    // Text colors
    pub text_primary: Hsla,
    pub text_secondary: Hsla,
    pub text_disabled: Hsla,

    // Brand/accent colors
    pub primary: Hsla,
    pub primary_hover: Hsla,

    // Semantic colors
    pub success: Hsla,
    pub warning: Hsla,
    pub error: Hsla,
    pub info: Hsla,

    // Border colors
    pub border: Hsla,
    pub border_focus: Hsla,
}

/// Spacing scale for consistent layout
#[derive(Debug, Clone, Copy)]
pub struct Spacing {
    pub xs: f32,  // 4px
    pub sm: f32,  // 8px
    pub md: f32,  // 16px
    pub lg: f32,  // 24px
    pub xl: f32,  // 32px
    pub xxl: f32, // 48px
}

/// Typography settings
#[derive(Debug, Clone)]
pub struct Typography {
    pub font_family: String,
    pub mono_font_family: String,
    pub font_size_xs: f32,
    pub font_size_sm: f32,
    pub font_size_md: f32,
    pub font_size_lg: f32,
    pub font_size_xl: f32,
    pub line_height: f32,
}

/// Border styling
#[derive(Debug, Clone, Copy)]
pub struct Borders {
    pub radius_sm: f32,
    pub radius_md: f32,
    pub radius_lg: f32,
    pub width_thin: f32,
    pub width_medium: f32,
}

impl Default for Theme {
    fn default() -> Self {
        Self::dark()
    }
}

impl Theme {
    /// Create a dark theme (default) - Gruvbox Dark
    pub fn dark() -> Self {
        Self {
            colors: Colors {
                // Backgrounds - Gruvbox Dark
                background: hsla(0.0, 0.0, 0.113, 1.0), // #1d2021
                surface: hsla(0.0, 0.0, 0.157, 1.0),    // #282828
                surface_hover: hsla(0.0, 0.0, 0.196, 1.0), // #32302f

                // Text - Gruvbox Dark
                text_primary: hsla(39.0 / 360.0, 0.36, 0.82, 1.0), // #ebdbb2 (fg)
                text_secondary: hsla(40.0 / 360.0, 0.28, 0.67, 1.0), // #a89984 (fg4)
                text_disabled: hsla(0.0, 0.07, 0.42, 1.0),         // #665c54 (fg1)

                // Brand - Gruvbox Yellow/Aqua
                primary: hsla(43.0 / 360.0, 0.74, 0.49, 1.0), // #d79921 (yellow)
                primary_hover: hsla(43.0 / 360.0, 0.74, 0.59, 1.0), // lighter yellow

                // Semantic - Gruvbox colors
                success: hsla(104.0 / 360.0, 0.41, 0.58, 1.0), // #98971a (green)
                warning: hsla(32.0 / 360.0, 0.88, 0.51, 1.0),  // #d65d0e (orange)
                error: hsla(0.0, 0.89, 0.59, 1.0),             // #fb4934 (red)
                info: hsla(205.0 / 360.0, 0.35, 0.56, 1.0),    // #83a598 (aqua)

                // Borders - Gruvbox
                border: hsla(20.0 / 360.0, 0.05, 0.24, 1.0), // #3e3936
                border_focus: hsla(43.0 / 360.0, 0.74, 0.49, 1.0), // #d79921 (yellow)
            },
            spacing: Spacing {
                xs: 4.0,
                sm: 8.0,
                md: 16.0,
                lg: 24.0,
                xl: 32.0,
                xxl: 48.0,
            },
            typography: Typography {
                font_family: "system-ui".to_string(),
                mono_font_family: "monospace".to_string(),
                font_size_xs: 11.0,
                font_size_sm: 13.0,
                font_size_md: 14.0,
                font_size_lg: 16.0,
                font_size_xl: 20.0,
                line_height: 1.5,
            },
            borders: Borders {
                radius_sm: 4.0,
                radius_md: 6.0,
                radius_lg: 8.0,
                width_thin: 1.0,
                width_medium: 2.0,
            },
        }
    }

    /// Create a light theme - Gruvbox Light
    pub fn light() -> Self {
        Self {
            colors: Colors {
                // Backgrounds - Gruvbox Light
                background: hsla(48.0 / 360.0, 0.87, 0.86, 1.0), // #fbf1c7
                surface: hsla(40.0 / 360.0, 0.54, 0.80, 1.0),    // #ebdbb2
                surface_hover: hsla(36.0 / 360.0, 0.48, 0.75, 1.0), // #d5c4a1

                // Text - Gruvbox Light
                text_primary: hsla(20.0 / 360.0, 0.11, 0.24, 1.0), // #3c3836 (fg)
                text_secondary: hsla(30.0 / 360.0, 0.11, 0.38, 1.0), // #504945 (fg4)
                text_disabled: hsla(35.0 / 360.0, 0.12, 0.55, 1.0), // #7c6f64 (fg1)

                // Brand - Gruvbox Yellow
                primary: hsla(43.0 / 360.0, 0.74, 0.49, 1.0), // #d79921 (yellow)
                primary_hover: hsla(43.0 / 360.0, 0.74, 0.39, 1.0), // darker yellow

                // Semantic - Gruvbox colors
                success: hsla(104.0 / 360.0, 0.41, 0.58, 1.0), // #98971a (green)
                warning: hsla(32.0 / 360.0, 0.88, 0.51, 1.0),  // #d65d0e (orange)
                error: hsla(0.0, 0.89, 0.59, 1.0),             // #fb4934 (red)
                info: hsla(205.0 / 360.0, 0.34, 0.42, 1.0),    // #458588 (blue)

                // Borders - Gruvbox Light
                border: hsla(36.0 / 360.0, 0.48, 0.75, 1.0), // #d5c4a1
                border_focus: hsla(43.0 / 360.0, 0.74, 0.49, 1.0), // #d79921 (yellow)
            },
            spacing: Spacing {
                xs: 4.0,
                sm: 8.0,
                md: 16.0,
                lg: 24.0,
                xl: 32.0,
                xxl: 48.0,
            },
            typography: Typography {
                font_family: "system-ui".to_string(),
                mono_font_family: "monospace".to_string(),
                font_size_xs: 11.0,
                font_size_sm: 13.0,
                font_size_md: 14.0,
                font_size_lg: 16.0,
                font_size_xl: 20.0,
                line_height: 1.5,
            },
            borders: Borders {
                radius_sm: 4.0,
                radius_md: 6.0,
                radius_lg: 8.0,
                width_thin: 1.0,
                width_medium: 2.0,
            },
        }
    }

    pub fn apply_font_config(&mut self, config: &FontConfig) {
        self.typography.font_family = config.family.clone();
        if let Some(mono_family) = &config.mono_family {
            self.typography.mono_font_family = mono_family.clone();
        }
    }
}

/// Helper function to create HSLA colors
///
/// # Arguments
/// * `h` - Hue (0.0 to 1.0)
/// * `s` - Saturation (0.0 to 1.0)
/// * `l` - Lightness (0.0 to 1.0)
/// * `a` - Alpha (0.0 to 1.0)
pub fn hsla(h: f32, s: f32, l: f32, a: f32) -> Hsla {
    Hsla { h, s, l, a }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_theme_is_dark() {
        let theme = Theme::default();
        // Dark theme should have low lightness for background
        assert!(theme.colors.background.l < 0.5);
    }

    #[test]
    fn test_light_theme() {
        let theme = Theme::light();
        // Light theme should have high lightness for background
        assert!(theme.colors.background.l > 0.5);
    }

    #[test]
    fn test_spacing_scale() {
        let theme = Theme::default();
        assert_eq!(theme.spacing.xs, 4.0);
        assert_eq!(theme.spacing.sm, 8.0);
        assert_eq!(theme.spacing.md, 16.0);
        assert_eq!(theme.spacing.lg, 24.0);
        assert_eq!(theme.spacing.xl, 32.0);
        assert_eq!(theme.spacing.xxl, 48.0);
    }

    #[test]
    fn test_hsla_helper() {
        let color = hsla(0.5, 0.6, 0.7, 0.8);
        assert_eq!(color.h, 0.5);
        assert_eq!(color.s, 0.6);
        assert_eq!(color.l, 0.7);
        assert_eq!(color.a, 0.8);
    }
}
