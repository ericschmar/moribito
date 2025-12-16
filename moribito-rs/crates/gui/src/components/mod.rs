/// UI components for the Moribito GUI application
///
/// This module re-exports components from gpui-component with some helper utilities
/// for consistent styling throughout the application.
// Custom components
pub mod checkbox_card;
pub use checkbox_card::CheckboxCard;

pub mod connections_list;
pub use connections_list::{ConnectionItem, ConnectionsList};

pub mod entry_details_table;
pub use entry_details_table::EntryDetailsTable;

pub mod search_bar;
pub use search_bar::SearchBar;

pub mod status_bar;
pub use status_bar::StatusBar;

pub mod tree_view;
pub use tree_view::TreeView;

// Re-export commonly used gpui-component types
pub use gpui_component::{
    button::{Button, ButtonVariants},
    checkbox::Checkbox,
    input::{Input, InputState},
    label::Label,
    Disableable, Sizable,
};

// Re-export gpui prelude for convenient access
pub use gpui::prelude::*;
