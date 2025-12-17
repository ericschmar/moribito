//! Search Bar Component
//!
//! Provides search interface for LDAP queries with filter and base DN inputs.

use crate::views::browser_view::BrowserView;
use gpui::*;
use gpui_component::{
    button::{Button, ButtonVariants},
    h_flex,
    input::{Input, InputState},
    theme::ActiveTheme,
    Disableable, Sizable,
};

/// Search bar component for LDAP searches
pub struct SearchBar {
    filter_input: Entity<InputState>,
    base_dn_input: Entity<InputState>,
}

impl SearchBar {
    /// Create a new SearchBar with initial values
    pub fn new(
        filter: impl Into<SharedString>,
        base_dn: impl Into<SharedString>,
        window: &mut Window,
        cx: &mut App,
    ) -> Self {
        let filter_str = filter.into().to_string();
        let base_dn_str = base_dn.into().to_string();

        let filter_input = cx.new(|cx| {
            InputState::new(window, cx)
                .placeholder("(objectClass=*)")
                .default_value(&filter_str)
        });

        let base_dn_input = cx.new(|cx| {
            InputState::new(window, cx)
                .placeholder("dc=example,dc=com")
                .default_value(&base_dn_str)
        });

        Self {
            filter_input,
            base_dn_input,
        }
    }

    /// Render the search bar
    pub fn render<F, C>(
        &self,
        on_search: F,
        on_clear: C,
        _window: &mut Window,
        cx: &mut Context<BrowserView>,
    ) -> impl IntoElement
    where
        F: Fn(String, String) + 'static,
        C: Fn() + 'static,
    {
        let theme = cx.theme();
        let filter_input = self.filter_input.clone();
        let base_dn_input = self.base_dn_input.clone();

        h_flex()
            .w_full()
            .gap_2()
            .p_2()
            .bg(theme.background)
            .border_b_1()
            .border_color(theme.border)
            .child(
                // Base DN input
                h_flex()
                    .flex_1()
                    .items_center()
                    .gap_2()
                    .child(
                        div()
                            .text_size(px(12.0))
                            .text_color(theme.foreground)
                            .child("Base DN:"),
                    )
                    .child(Input::new(&base_dn_input).flex_1().text_xs()),
            )
            .child(
                // Filter input
                h_flex()
                    .flex_1()
                    .items_center()
                    .gap_2()
                    .child(
                        div()
                            .text_size(px(12.0))
                            .text_color(theme.foreground)
                            .child("Filter:"),
                    )
                    .child(Input::new(&filter_input).flex_1().text_xs()),
            )
            .child(
                // Search button
                Button::new("search")
                    .primary()
                    .small()
                    .label("Search")
                    .on_click({
                        let filter_input = filter_input.clone();
                        let base_dn_input = base_dn_input.clone();
                        move |_event, _window, cx| {
                            let filter = filter_input.read(cx).text().to_string();
                            let base_dn = base_dn_input.read(cx).text().to_string();
                            on_search(filter, base_dn);
                        }
                    }),
            )
            .child(
                // Clear button
                Button::new("clear")
                    .ghost()
                    .small()
                    .label("Clear")
                    .on_click(move |_event, _window, _cx| {
                        on_clear();
                    }),
            )
    }
}

impl Clone for SearchBar {
    fn clone(&self) -> Self {
        Self {
            filter_input: self.filter_input.clone(),
            base_dn_input: self.base_dn_input.clone(),
        }
    }
}
