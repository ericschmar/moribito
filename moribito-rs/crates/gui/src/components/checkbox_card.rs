//! CheckboxCard component - A card-style selectable checkbox component
//!
//! This component combines a checkbox with a card container to create a more
//! prominent selectable option with label and description text.

use std::rc::Rc;

use gpui::{
    div, px, App, Div, ElementId, FontWeight, InteractiveElement, IntoElement, ParentElement,
    RenderOnce, SharedString, StatefulInteractiveElement, Styled, Window,
};
use gpui::prelude::*;
use gpui_component::checkbox::Checkbox;
use gpui_component::Disableable;

use crate::theme::Theme;

/// A card-style checkbox component with label and description
///
/// # Example
/// ```ignore
/// CheckboxCard::new("enable-ssl")
///     .label("Enable SSL/TLS")
///     .description("Use encrypted connection to the LDAP server")
///     .checked(true)
///     .on_click(cx.listener(|view, checked, _window, cx| {
///         view.ssl_enabled = *checked;
///         cx.notify();
///     }))
/// ```
#[derive(IntoElement)]
pub struct CheckboxCard {
    id: ElementId,
    base: Div,
    label: SharedString,
    description: Option<SharedString>,
    checked: bool,
    disabled: bool,
    on_click: Option<Rc<dyn Fn(&bool, &mut Window, &mut App) + 'static>>,
}

impl CheckboxCard {
    /// Create a new CheckboxCard with the given ID
    pub fn new(id: impl Into<ElementId>) -> Self {
        Self {
            id: id.into(),
            base: div(),
            label: SharedString::from(""),
            description: None,
            checked: false,
            disabled: false,
            on_click: None,
        }
    }

    /// Set the label text (appears to the right of the checkbox)
    pub fn label(mut self, label: impl Into<SharedString>) -> Self {
        self.label = label.into();
        self
    }

    /// Set the description text (appears below the label)
    pub fn description(mut self, description: impl Into<SharedString>) -> Self {
        self.description = Some(description.into());
        self
    }

    /// Set the initial checked state
    pub fn checked(mut self, checked: bool) -> Self {
        self.checked = checked;
        self
    }

    /// Set whether the checkbox is disabled
    pub fn disabled(mut self, disabled: bool) -> Self {
        self.disabled = disabled;
        self
    }

    /// Set the click handler - receives the new checked state
    pub fn on_click(mut self, handler: impl Fn(&bool, &mut Window, &mut App) + 'static) -> Self {
        self.on_click = Some(Rc::new(handler));
        self
    }

    fn handle_click(
        on_click: &Option<Rc<dyn Fn(&bool, &mut Window, &mut App) + 'static>>,
        checked: bool,
        window: &mut Window,
        cx: &mut App,
    ) {
        let new_checked = !checked;
        if let Some(f) = on_click {
            (f)(&new_checked, window, cx);
        }
    }
}

impl InteractiveElement for CheckboxCard {
    fn interactivity(&mut self) -> &mut gpui::Interactivity {
        self.base.interactivity()
    }
}

impl StatefulInteractiveElement for CheckboxCard {}

impl Styled for CheckboxCard {
    fn style(&mut self) -> &mut gpui::StyleRefinement {
        self.base.style()
    }
}

impl RenderOnce for CheckboxCard {
    fn render(self, _window: &mut Window, _cx: &mut App) -> impl IntoElement {
        let theme = Theme::default();
        let checked = self.checked;
        let disabled = self.disabled;
        let on_click = self.on_click.clone();

        div()
            .id(self.id.clone())
            .flex()
            .flex_row()
            .gap(px(theme.spacing.md))
            .p(px(theme.spacing.md))
            .text_align(gpui::TextAlign::Center)
            .border_1()
            .border_color(if checked {
                theme.colors.border_focus
            } else {
                theme.colors.border
            })
            .rounded(px(theme.borders.radius_md))
            .bg(if checked {
                theme.colors.surface_hover
            } else {
                theme.colors.surface
            })
            .when(!disabled, |this| {
                this.hover(|style| style.bg(theme.colors.surface_hover))
                    .cursor_pointer()
                    .on_click(move |_event, window, cx| {
                        Self::handle_click(&on_click, checked, window, cx);
                    })
            })
            .when(disabled, |this| this.opacity(0.5))
            .child(
                // Checkbox on the left
                div().flex().items_start().pt(px(2.0)).child(
                    Checkbox::new(self.id.clone())
                        .checked(self.checked)
                        .disabled(self.disabled),
                ),
            )
            .child(
                // Label and description on the right
                div()
                    .flex()
                    .flex_col()
                    .gap(px(theme.spacing.xs))
                    .flex_1()
                    .child(
                        div()
                            .text_color(theme.colors.text_primary)
                            .text_size(px(theme.typography.font_size_md))
                            .font_weight(FontWeight::NORMAL)
                            .child(self.label),
                    )
                    .when_some(self.description, |this, desc| {
                        this.child(
                            div()
                                .text_color(theme.colors.text_secondary)
                                .text_size(px(theme.typography.font_size_sm))
                                .child(desc),
                        )
                    }),
            )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_checkbox_card_builder() {
        let card = CheckboxCard::new("test-id")
            .label("Test Label")
            .description("Test Description")
            .checked(true)
            .disabled(false);

        assert_eq!(card.label, SharedString::from("Test Label"));
        assert_eq!(
            card.description,
            Some(SharedString::from("Test Description"))
        );
        assert_eq!(card.checked, true);
        assert_eq!(card.disabled, false);
    }

    #[test]
    fn test_checkbox_card_without_description() {
        let card = CheckboxCard::new("test-id").label("Test Label");

        assert_eq!(card.description, None);
    }
}
