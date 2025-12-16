//! Native menu bar definitions for Moribito
//!
//! Defines the macOS/Windows/Linux menu bar structure with actions and keyboard shortcuts.

use gpui::*;

use crate::actions::*;

/// Build the complete menu bar structure
pub fn build_menus() -> Vec<Menu> {
    vec![
        build_app_menu(),
        build_file_menu(),
        build_edit_menu(),
        build_view_menu(),
        build_connection_menu(),
        build_help_menu(),
    ]
}

/// App menu (Moribito menu on macOS)
fn build_app_menu() -> Menu {
    Menu {
        name: "Moribito".into(),
        items: vec![
            MenuItem::action("About Moribito", ShowAbout),
            MenuItem::separator(),
            // Settings placeholder - grayed out for now (will be implemented later)
            // MenuItem::action("Settings...", OpenSettings).disabled(),
        ],
    }
}

/// File menu
fn build_file_menu() -> Menu {
    Menu {
        name: "File".into(),
        items: vec![
            MenuItem::separator(),
            MenuItem::action("Close Window", CloseWindow),
        ],
    }
}

/// Edit menu
fn build_edit_menu() -> Menu {
    Menu {
        name: "Edit".into(),
        items: vec![
            MenuItem::action("Cut", Cut),
            MenuItem::action("Copy", Copy),
            MenuItem::action("Paste", Paste),
            MenuItem::action("Delete", DeleteEntry),
            MenuItem::separator(),
            MenuItem::action("Select All", SelectAll),
        ],
    }
}

/// View menu
fn build_view_menu() -> Menu {
    Menu {
        name: "View".into(),
        items: vec![
            MenuItem::action("Refresh", Refresh),
            MenuItem::separator(),
            MenuItem::action("Expand All", ExpandAll),
            MenuItem::action("Collapse All", CollapseAll),
        ],
    }
}

/// Connection menu
fn build_connection_menu() -> Menu {
    Menu {
        name: "Connection".into(),
        items: vec![
            MenuItem::action("New Connection...", NewConnection),
            MenuItem::action("Manage Connections...", OpenConfigWindow),
            MenuItem::separator(),
            MenuItem::action("Connect", Connect),
            MenuItem::action("Disconnect", Disconnect),
            MenuItem::separator(),
            MenuItem::action("Test Connection", TestConnection),
        ],
    }
}

/// Help menu
fn build_help_menu() -> Menu {
    Menu {
        name: "Help".into(),
        items: vec![MenuItem::action("Documentation", ShowDocumentation)],
    }
}
