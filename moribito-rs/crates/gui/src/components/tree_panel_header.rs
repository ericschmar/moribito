//! Tree Panel Header Component
//!
//! Zed-style action bar with breadcrumb navigation and action buttons.

use gpui::prelude::*;
use gpui::*;
use gpui_component::{
    button::{Button, ButtonVariants},
    h_flex,
    theme::ActiveTheme,
    Disableable, Icon, IconName, Sizable,
};

use crate::actions::*;
use crate::app_state::SharedAppState;

pub struct TreePanelHeader {
    app_state: SharedAppState,
}

impl TreePanelHeader {
    pub fn new(app_state: SharedAppState) -> Self {
        Self { app_state }
    }

    pub fn render(&self, _window: &mut Window, cx: &mut App) -> impl IntoElement {
        let theme = cx.theme();
        let state = self.app_state.read();
        let breadcrumb = state.get_breadcrumb_path();
        let can_go_up = !state.tree_state.navigation_stack.is_empty();
        drop(state);

        h_flex()
            .w_full()
            .h(px(32.0))
            .px_2()
            .items_center()
            .bg(theme.muted)
            .border_b_1()
            .border_color(theme.border)
            // Left: Breadcrumb
            .child(self.render_breadcrumb(breadcrumb, &theme))
            // Spacer
            .child(div().flex_1())
            // Right: Action buttons
            .child(self.render_actions(can_go_up, &theme))
    }

    fn render_breadcrumb(
        &self,
        path: Vec<(String, String)>,
        theme: &gpui_component::Theme,
    ) -> impl IntoElement {
        let path_len = path.len();
        h_flex()
            .gap_1()
            .items_center()
            .overflow_x_hidden()
            .children(path.into_iter().enumerate().map(move |(i, (label, dn))| {
                let is_last = i == path_len - 1;

                h_flex()
                    .gap_1()
                    .when(i > 0, |this| {
                        this.child(
                            Icon::new(IconName::ChevronRight)
                                .size_3()
                                .text_color(theme.muted_foreground),
                        )
                    })
                    .child(
                        div()
                            .text_size(px(12.0))
                            .text_color(if is_last {
                                theme.foreground
                            } else {
                                theme.muted_foreground
                            })
                            .when(!is_last, |this| {
                                let dn = dn.clone();
                                this.cursor_pointer()
                                    .hover(|style| style.text_color(theme.primary))
                                    .on_mouse_down(
                                        MouseButton::Left,
                                        move |_event, _window, cx: &mut App| {
                                            cx.dispatch_action(&NavigateToBreadcrumb {
                                                dn: dn.clone(),
                                            });
                                        },
                                    )
                            })
                            .child(label.clone()),
                    )
            }))
    }

    fn render_actions(&self, can_go_up: bool, theme: &gpui_component::Theme) -> impl IntoElement {
        h_flex()
            .gap_1()
            .items_center()
            // Up/Back button
            .child(
                Button::new("nav-up")
                    .icon(IconName::ArrowUp)
                    .ghost()
                    .xsmall()
                    .disabled(!can_go_up)
                    .on_click(|_, _window, cx: &mut App| {
                        cx.dispatch_action(&NavigateUp);
                    }),
            )
    }
}

impl Clone for TreePanelHeader {
    fn clone(&self) -> Self {
        Self {
            app_state: self.app_state.clone(),
        }
    }
}
