//! Moribito GUI - LDAP Explorer
//!
//! Main entry point for the GPUI-based GUI application.

#![recursion_limit = "256"]

mod actions;
mod app_state;
mod components;
mod menus;
mod theme;
mod views;

use gpui::*;
use gpui_component::{Root, Theme, ThemeRegistry};
use gpui_component_assets::Assets;
use std::path::PathBuf;

use app_state::SharedAppState;
use menus::build_menus;
use views::BrowserView;

fn main() {
    env_logger::init();

    Application::new().with_assets(Assets).run(|cx: &mut App| {
        // Initialize gpui-component
        gpui_component::init(cx);

        // Set up native menus
        cx.set_menus(build_menus());

        // Load and apply Gruvbox theme
        let theme_name = SharedString::from("Gruvbox Dark");
        let themes_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("themes");

        if let Err(err) = ThemeRegistry::watch_dir(themes_dir, cx, move |cx| {
            if let Some(theme) = ThemeRegistry::global(cx).themes().get(&theme_name).cloned() {
                Theme::global_mut(cx).apply_config(&theme);
            }
        }) {
            eprintln!("Failed to load themes: {}", err);
        }

        // Create shared application state
        let app_state = SharedAppState::default();

        // Set up the main window
        let bounds = Bounds::centered(None, size(px(1200.0), px(800.0)), cx);

        cx.open_window(
            WindowOptions {
                window_bounds: Some(WindowBounds::Windowed(bounds)),
                focus: true,
                show: true,
                kind: WindowKind::Normal,
                is_movable: true,
                titlebar: Some(TitlebarOptions {
                    title: Some("Moribito LDAP Browser".into()),
                    ..Default::default()
                }),
                ..Default::default()
            },
            |window, cx| {
                // Set up window close handler to quit the app when window closes
                window.on_window_should_close(cx, |_window, cx| {
                    cx.quit();
                    true // Allow the window to close
                });

                // Create the browser view and wrap it in Root component
                cx.new(|cx| {
                    let browser_view =
                        cx.new(|inner_cx| BrowserView::new(app_state, window, inner_cx));
                    // Focus the browser view so menu actions work
                    window.focus(&browser_view.focus_handle(cx));
                    Root::new(browser_view, window, cx)
                })
            },
        )
        .unwrap();

        cx.activate(true);
    });
}
