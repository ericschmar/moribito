//! Connections List Component
//!
//! Displays a list of saved LDAP connections with selection and management actions.

use std::rc::Rc;

use gpui::prelude::*;
use gpui::{
    div, px, App, ClickEvent, Div, FontWeight, InteractiveElement, IntoElement, ParentElement,
    RenderOnce, StatefulInteractiveElement, Styled, Window,
};
use gpui_component::{
    button::{Button, ButtonVariants},
    theme::ActiveTheme,
    Disableable, IconName, Sizable,
};

use crate::theme::Theme;

/// A single connection item in the list
#[derive(Clone)]
pub struct ConnectionItem {
    pub name: String,
    pub host: String,
    pub port: u16,
    pub is_selected: bool,
}

/// Connections list component
#[derive(IntoElement)]
pub struct ConnectionsList {
    connections: Vec<ConnectionItem>,
    on_select: Option<Rc<dyn Fn(&str, &mut Window, &mut App) + 'static>>,
    on_new: Option<Rc<dyn Fn(&ClickEvent, &mut Window, &mut App) + 'static>>,
    on_delete: Option<Rc<dyn Fn(&str, &mut Window, &mut App) + 'static>>,
}

impl ConnectionsList {
    /// Create a new connections list
    pub fn new(connections: Vec<ConnectionItem>) -> Self {
        Self {
            connections,
            on_select: None,
            on_new: None,
            on_delete: None,
        }
    }

    /// Set the selection handler
    pub fn on_select<F>(mut self, handler: F) -> Self
    where
        F: Fn(&str, &mut Window, &mut App) + 'static,
    {
        self.on_select = Some(Rc::new(handler));
        self
    }

    /// Set the new connection handler
    pub fn on_new<F>(mut self, handler: F) -> Self
    where
        F: Fn(&ClickEvent, &mut Window, &mut App) + 'static,
    {
        self.on_new = Some(Rc::new(handler));
        self
    }

    /// Set the delete handler
    pub fn on_delete<F>(mut self, handler: F) -> Self
    where
        F: Fn(&str, &mut Window, &mut App) + 'static,
    {
        self.on_delete = Some(Rc::new(handler));
        self
    }

    fn render_connection_item(
        &self,
        item: &ConnectionItem,
        idx: usize,
        cx: &App,
    ) -> impl IntoElement {
        let theme = Theme::default();
        let name = item.name.clone();
        let on_select = self.on_select.as_ref().cloned();

        div()
            .id(("connection", idx))
            .flex()
            .items_center()
            .gap(px(theme.spacing.sm))
            .py(px(theme.spacing.xs))
            .bg(if item.is_selected {
                theme.colors.surface_hover
            } else {
                theme.colors.surface
            })
            .when(item.is_selected, |style| {
                style
                    .pl(px(theme.spacing.sm - 3.0))
                    .border_l_4()
                    .border_color(cx.theme().primary)
            })
            .when(!item.is_selected, |style| style.pl(px(theme.spacing.sm)))
            .border_color(theme.colors.primary)
            .hover(|style| style.bg(theme.colors.surface_hover))
            .cursor_pointer()
            .on_click(move |_event, window, cx| {
                if let Some(ref handler) = on_select {
                    handler(&name, window, cx);
                }
            })
            .child(
                // Name - max 50% width with ellipsis
                div()
                    .flex_1()
                    .max_w(px(125.0)) // Half of typical 250px panel width
                    .overflow_hidden()
                    .text_size(px(theme.typography.font_size_sm))
                    .font_weight(FontWeight::MEDIUM)
                    .text_color(theme.colors.text_primary)
                    .whitespace_nowrap()
                    .child(item.name.clone()),
            )
            .child(
                // Host - max 50% width with ellipsis
                div()
                    .flex_1()
                    .max_w(px(125.0))
                    .overflow_hidden()
                    .text_size(px(theme.typography.font_size_xs))
                    .text_color(theme.colors.text_secondary)
                    .whitespace_nowrap()
                    .child(format!("{}:{}", item.host, item.port)),
            )
    }
}

impl RenderOnce for ConnectionsList {
    fn render(self, _window: &mut Window, cx: &mut App) -> impl IntoElement {
        let theme = Theme::default();
        let on_new = self.on_new.clone();
        let on_delete = self.on_delete.clone();
        let connections = self.connections.clone();
        let selected_connection = connections
            .iter()
            .find(|c| c.is_selected)
            .map(|c| c.name.clone());

        div()
            .flex()
            .flex_col()
            .h_full()
            .w_full()
            .bg(cx.theme().background)
            .child(
                // Connections list - scrollable
                div()
                    .id("connections-list-scroll")
                    .flex()
                    .flex_col()
                    .overflow_y_scroll()
                    .flex_1()
                    .when(connections.is_empty(), |el| {
                        el.child(
                            div()
                                .flex()
                                .flex_col()
                                .items_center()
                                .justify_center()
                                .gap_2()
                                .h_full()
                                .p(px(theme.spacing.lg))
                                .child(
                                    div()
                                        .text_size(px(theme.typography.font_size_sm))
                                        .text_color(theme.colors.text_secondary)
                                        .child("No connections yet"),
                                )
                                .child(
                                    div()
                                        .text_size(px(theme.typography.font_size_xs))
                                        .text_color(theme.colors.text_disabled)
                                        .text_center()
                                        .child("Click + to add a new connection"),
                                ),
                        )
                    })
                    .children(
                        connections
                            .iter()
                            .enumerate()
                            .map(|(idx, conn)| self.render_connection_item(conn, idx, cx)),
                    ),
            )
            .child(
                // Bottom bar with +/- buttons
                div()
                    .flex()
                    .items_center()
                    .justify_end()
                    .gap(px(4.0))
                    .p(px(theme.spacing.xs))
                    .border_t_1()
                    .border_color(cx.theme().border)
                    .bg(cx.theme().background)
                    .child(
                        // Add button
                        Button::new("add-connection")
                            .ghost()
                            .icon(IconName::Plus)
                            .xsmall()
                            .compact()
                            .when_some(on_new.clone(), |btn, handler| {
                                btn.on_click(move |event, window, cx| {
                                    handler(event, window, cx);
                                })
                            }),
                    )
                    .child(
                        // Remove button
                        Button::new("remove-connection")
                            .ghost()
                            .icon(IconName::Minus)
                            .xsmall()
                            .compact()
                            .disabled(selected_connection.is_none())
                            .when_some(on_delete, |btn, handler| {
                                btn.on_click(move |_event, window, cx| {
                                    if let Some(ref name) = selected_connection {
                                        handler(name, window, cx);
                                    }
                                })
                            }),
                    ),
            )
    }
}
