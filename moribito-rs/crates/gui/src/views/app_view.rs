use gpui::prelude::*;
use gpui::{App, Context, Entity, FocusHandle, Focusable, Window};

use crate::app_state::SharedAppState;
use crate::views::{BrowserView, StartView};

/// Chooses between the start screen and the browser view depending on the connection state.
pub struct AppView {
    app_state: SharedAppState,
    start_view: Entity<StartView>,
    browser_view: Option<Entity<BrowserView>>,
    focus_handle: FocusHandle,
}

impl AppView {
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Self {
        let start_view = cx.new(|cx| StartView::new(app_state.clone(), window, cx));
        Self {
            app_state,
            start_view,
            browser_view: None,
            focus_handle: cx.focus_handle(),
        }
    }
}

impl Render for AppView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        if self.app_state.read().is_connected {
            if self.browser_view.is_none() {
                let browser = cx.new(|cx| BrowserView::new(self.app_state.clone(), window, cx));
                self.browser_view = Some(browser);
            }

            let browser = self.browser_view.as_ref().unwrap().clone();
            browser.into_any_element()
        } else {
            self.browser_view = None;
            self.start_view.clone().into_any_element()
        }
    }
}

impl Focusable for AppView {
    fn focus_handle(&self, _cx: &gpui::App) -> FocusHandle {
        self.focus_handle.clone()
    }
}
