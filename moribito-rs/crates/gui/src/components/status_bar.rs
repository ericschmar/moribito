//! Status Bar Component
//!
//! Displays connection status and current messages at the bottom of the browser view.

use gpui::prelude::*;
use gpui::*;
use gpui_component::{h_flex, theme::ActiveTheme};

use crate::app_state::SharedAppState;

/// Status bar component showing connection status and messages
pub struct StatusBar {
    app_state: SharedAppState,
}

impl StatusBar {
    /// Create a new StatusBar
    pub fn new(app_state: SharedAppState) -> Self {
        Self { app_state }
    }

    /// Render the status bar
    pub fn render(&self, _window: &mut Window, cx: &mut App) -> impl IntoElement {
        let theme = cx.theme();

        // Extract data from state before borrowing ends
        let (status_text, is_success, message_text) = {
            let state = self.app_state.read();
            let status = if state.is_connected {
                if let Some(ref base_dn) = state.current_base_dn {
                    format!("Connected: {}", base_dn)
                } else {
                    "Connected".to_string()
                }
            } else {
                "Not connected".to_string()
            };
            let message = state.status_message.clone().unwrap_or_default();
            (status, state.is_connected, message)
        };

        let status_color = if is_success {
            theme.success
        } else {
            theme.muted_foreground
        };

        h_flex()
            .w_full()
            .h(px(24.0))
            .gap_4()
            .px_3()
            .items_center()
            .bg(theme.muted)
            .border_t_1()
            .border_color(theme.border)
            .child(
                // Connection status
                div()
                    .text_size(px(12.0))
                    .text_color(status_color)
                    .child(status_text),
            )
            .when(!message_text.is_empty(), |this| {
                this.child(
                    div()
                        .text_size(px(12.0))
                        .text_color(theme.muted_foreground)
                        .child("|"),
                )
                .child(
                    div()
                        .text_size(px(12.0))
                        .text_color(theme.foreground)
                        .child(message_text),
                )
            })
    }
}

impl Clone for StatusBar {
    fn clone(&self) -> Self {
        Self {
            app_state: self.app_state.clone(),
        }
    }
}
