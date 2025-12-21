//! Start screen that welcomes the user and lets them open a saved connection.

use gpui::prelude::*;
use gpui::{
    div, px, size, App, Bounds, Context, FocusHandle, Focusable, FontWeight,
    TitlebarOptions, Window, WindowBounds, WindowKind, WindowOptions,
};
use gpui_component::{
    button::{Button, ButtonVariants},
    h_flex, v_flex, Root,
};
use moribito_core::client::LdapClient;
use moribito_core::config::{ConnectionSettings, SavedConnection};

use crate::app_state::SharedAppState;
use crate::theme::Theme;
use crate::views::ConfigView;

/// Start view shown before the browser is connected.
pub struct StartView {
    app_state: SharedAppState,
    status_message: Option<String>,
    status_is_error: bool,
    is_connecting: bool,
    focus_handle: FocusHandle,
}

impl StartView {
    pub fn new(app_state: SharedAppState, _window: &mut Window, cx: &mut App) -> Self {
        Self {
            app_state,
            status_message: None,
            status_is_error: false,
            is_connecting: false,
            focus_handle: cx.focus_handle(),
        }
    }

    fn open_config_window(&self, window: &mut Window, cx: &mut Context<Self>) {
        let app_state = self.app_state.clone();
        let bounds = Bounds::centered(None, size(px(800.0), px(750.0)), cx);
        cx.open_window(
            WindowOptions {
                window_bounds: Some(WindowBounds::Windowed(bounds)),
                focus: true,
                show: true,
                kind: WindowKind::Normal,
                is_movable: true,
                titlebar: Some(TitlebarOptions {
                    title: Some("Connection Settings".into()),
                    ..Default::default()
                }),
                ..Default::default()
            },
            |window, cx| {
                let view = ConfigView::new(app_state, window, cx);
                cx.new(|cx| Root::new(view, window, cx))
            },
        )
        .ok();
    }

    fn connect_to_connection(
        &mut self,
        connection: SavedConnection,
        window: &mut Window,
        cx: &mut Context<Self>,
    ) {
        if self.is_connecting {
            return;
        }

        self.is_connecting = true;
        self.status_message = Some(format!("Connecting to {}...", connection.name));
        self.status_is_error = false;
        cx.notify();

        let settings = ConnectionSettings::from_saved(&connection);
        let cmd: Result<LdapClient, Box<dyn std::error::Error>> = (|| {
            let mut client = LdapClient::connect(&settings)?;
            client.bind()?;
            Ok(client)
        })();

        match cmd {
            Ok(client) => {
                let base_dn = settings.base_dn.clone();
                self.app_state
                    .apply_connection(connection.name.clone(), client, base_dn);
                self.status_message = Some(format!("Connected to {}", connection.name));
                self.status_is_error = false;
            }
            Err(err) => {
                self.status_message = Some(format!("Connection failed: {}", err));
                self.status_is_error = true;
            }
        }

        self.is_connecting = false;
        cx.notify();
    }

    fn render_connections<'a>(
        &self,
        theme: &Theme,
        cx: &'a mut Context<Self>,
        saved_connections: Vec<SavedConnection>,
    ) -> impl IntoElement + 'a {
        let body = if saved_connections.is_empty() {
            div()
                .flex()
                .flex_col()
                .justify_center()
                .items_center()
                .gap(px(theme.spacing.sm))
                .child(
                    div()
                        .text_size(px(theme.typography.font_size_sm))
                        .text_color(theme.colors.text_secondary)
                        .text_center()
                        .child("No saved connections yet."),
                )
                .child(
                    Button::new("start-config")
                        .ghost()
                        .label("Add connection")
                        .on_click(cx.listener(|view, _event, window, cx| {
                            view.open_config_window(window, cx);
                        })),
                )
        } else {
            let cards = saved_connections
                .into_iter()
                .take(3)
                .enumerate()
                .map(|(idx, conn)| {
                    let connection = conn.clone();
                    v_flex().gap(px(theme.spacing.xs)).child(
                        Button::new(("connection-card", idx))
                            .ghost()
                            .child(
                                v_flex()
                                    .gap(px(theme.spacing.xs))
                                    .p(px(theme.spacing.md))
                                    .w_full()
                                    .items_start()
                                    .child(
                                        div()
                                            .text_color(theme.colors.text_primary)
                                            .text_size(px(theme.typography.font_size_md))
                                            .font_weight(FontWeight::BOLD)
                                            .child(conn.name.clone()),
                                    )
                                    .child(
                                        div()
                                            .text_color(theme.colors.text_secondary)
                                            .text_size(px(theme.typography.font_size_sm))
                                            .child(format!("{}:{}", conn.host, conn.port)),
                                    )
                                    .child(
                                        div()
                                            .text_color(theme.colors.text_secondary)
                                            .text_size(px(theme.typography.font_size_sm))
                                            .child(if conn.base_dn.is_empty() {
                                                "Base DN not set".to_string()
                                            } else {
                                                conn.base_dn.clone()
                                            }),
                                    ),
                            )
                            .on_click(cx.listener(move |view, _event, window, cx| {
                                view.connect_to_connection(connection.clone(), window, cx);
                            })),
                    )
                })
                .collect::<Vec<_>>();

            v_flex().gap(px(theme.spacing.sm)).children(cards)
        };

        v_flex()
            .flex()
            .gap(px(theme.spacing.md))
            .child(body)
            .child(
                Button::new("start-config")
                    .ghost()
                    .label("Add connection")
                    .on_click(cx.listener(|view, _event, window, cx| {
                        view.open_config_window(window, cx);
                    })),
            )
            .child(self.render_status(theme))
    }

    fn render_status(&self, theme: &Theme) -> impl IntoElement {
        if let Some(ref message) = self.status_message {
            let color = if self.status_is_error {
                theme.colors.error
            } else {
                theme.colors.info
            };
            div()
                .text_size(px(theme.typography.font_size_sm))
                .text_color(color)
                .child(message.clone())
        } else {
            div().child(" ")
        }
    }
}

impl Render for StartView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let theme = Theme::default();
        let saved_connections = self.app_state.read().saved_connections.clone();

        h_flex()
            .size_full()
            .gap(px(theme.spacing.lg))
            .p(px(theme.spacing.lg))
            .child(
                v_flex()
                    .size_full()
                    .flex()
                    .justify_center()
                    .items_center()
                    .gap(px(theme.spacing.md))
                    .bg(theme.colors.surface)
                    .rounded_md()
                    .p(px(theme.spacing.lg))
                    .child(
                        div()
                            .text_size(px(theme.typography.font_size_xl))
                            .font_weight(FontWeight::BOLD)
                            .text_color(theme.colors.primary)
                            .child("Moribito"),
                    )
                    .child(
                        div()
                            .text_size(px(theme.typography.font_size_md))
                            .text_color(theme.colors.text_secondary)
                            .child("A modern LDAP explorer with saved connections, tree navigation, and quick filtering."),
                    )
            )
            .child(
                v_flex()
                    .flex()
                    .size_full()
                    .justify_center()
                    .items_center()
                    .bg(theme.colors.surface)
                    .rounded_md()
                    .p(px(theme.spacing.lg))
                    .child(self.render_connections(&theme, cx, saved_connections)),
            )
    }
}

impl Focusable for StartView {
    fn focus_handle(&self, _cx: &gpui::App) -> FocusHandle {
        self.focus_handle.clone()
    }
}
