/// Views for the Moribito GUI application
///
/// Views are top-level UI components that represent different screens or modes of the application.
pub mod app_view;
pub use app_view::AppView;

pub mod browser_view;
pub use browser_view::BrowserView;

pub mod config_view;
pub use config_view::ConfigView;

pub mod start_view;
pub use start_view::StartView;
