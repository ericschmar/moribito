//! Configuration View for LDAP connection settings
//!
//! This module provides the main configuration screen where users can
//! create, edit, and test LDAP connections with a resizable split layout.

use gpui::prelude::*;
use gpui::{StatefulInteractiveElement, *};
use gpui_component::{
    button::{Button, ButtonVariants},
    input::{Input, InputState},
    resizable::{h_resizable, resizable_panel},
    theme::ActiveTheme,
    v_flex, Disableable, IconName, Sizable,
};
use moribito_core::client::LdapClient;
use moribito_core::config::{Config, ConnectionSettings};
use std::collections::HashMap;

use crate::app_state::SharedAppState;
use crate::components::{CheckboxCard, ConnectionItem, ConnectionsList};

/// The main configuration view for managing LDAP connections
pub struct ConfigView {
    /// Input states for form fields
    name_input: Entity<InputState>,
    host_input: Entity<InputState>,
    port_input: Entity<InputState>,
    base_dn_input: Entity<InputState>,
    bind_user_input: Entity<InputState>,
    bind_password_input: Entity<InputState>,

    /// Checkbox states
    use_ssl: bool,
    use_tls: bool,

    /// UI state
    is_testing: bool,
    test_result: Option<Result<String, String>>,
    validation_errors: HashMap<String, String>,

    /// Application config
    config: Config,

    /// Saved connections list
    saved_connections: Vec<ConnectionItem>,

    /// Currently selected connection name
    selected_connection: Option<String>,

    /// Shared application state
    app_state: SharedAppState,

    /// Focus handle
    focus_handle: FocusHandle,
}

impl ConfigView {
    /// Create a new configuration view
    pub fn new(app_state: SharedAppState, window: &mut Window, cx: &mut App) -> Entity<Self> {
        cx.new(|cx| {
            // Load config from default OS-specific path, or use default if file doesn't exist
            let config = Config::load_from_default_path().unwrap_or_else(|e| {
                eprintln!("Failed to load config, using defaults: {}", e);
                Config::default()
            });

            // Load saved connections from config
            let saved_connections: Vec<ConnectionItem> = config
                .ldap
                .saved_connections
                .iter()
                .map(|conn| ConnectionItem {
                    name: conn.name.clone(),
                    host: conn.host.clone(),
                    port: conn.port,
                    is_selected: false,
                })
                .collect();

            // Create input states with placeholders (no default values)
            let name_input = cx.new(|cx| InputState::new(window, cx).placeholder("My Connection"));
            let host_input = cx.new(|cx| {
                InputState::new(window, cx)
                    .placeholder("ldap.forumsys.com")
                    .default_value("ldap.forumsys.com")
            });
            let port_input = cx.new(|cx| {
                InputState::new(window, cx)
                    .placeholder("389")
                    .default_value("389")
            });
            let base_dn_input = cx.new(|cx| {
                InputState::new(window, cx)
                    .placeholder("dc=example,dc=com")
                    .default_value("dc=example,dc=com")
            });
            let bind_user_input = cx.new(|cx| {
                InputState::new(window, cx)
                    .placeholder("cn=read-only-admin,dc=example,dc=com")
                    .default_value("cn=read-only-admin,dc=example,dc=com")
            });
            let bind_password_input = cx.new(|cx| {
                let mut state = InputState::new(window, cx)
                    .placeholder("••••••••")
                    .default_value("password");
                state.set_masked(true, window, cx);
                state
            });

            Self {
                name_input,
                host_input,
                port_input,
                base_dn_input,
                bind_user_input,
                bind_password_input,
                use_ssl: false,
                use_tls: false,
                is_testing: false,
                test_result: None,
                validation_errors: HashMap::new(),
                config,
                saved_connections,
                selected_connection: None,
                app_state,
                focus_handle: cx.focus_handle(),
            }
        })
    }

    /// Select a connection from the list
    fn select_connection(&mut self, name: &str, window: &mut Window, cx: &mut Context<Self>) {
        self.selected_connection = Some(name.to_string());

        // Update selection state in connections list
        for conn in &mut self.saved_connections {
            conn.is_selected = conn.name == name;
        }

        // Find the connection in config and load its settings
        if let Some(saved_conn) = self
            .config
            .ldap
            .saved_connections
            .iter()
            .find(|c| c.name == name)
        {
            // Helper to replace text completely
            let replace_input = |input: &Entity<InputState>,
                                 text: &str,
                                 window: &mut Window,
                                 cx: &mut Context<Self>| {
                input.update(cx, |state, cx| {
                    // Get the current text length and replace the entire range
                    let len = state.text().len();
                    state.replace_text_in_range(Some(0..len), text, window, cx);
                });
            };

            // Update input fields
            replace_input(&self.name_input, &saved_conn.name, window, cx);
            replace_input(&self.host_input, &saved_conn.host, window, cx);
            replace_input(&self.port_input, &saved_conn.port.to_string(), window, cx);
            replace_input(&self.base_dn_input, &saved_conn.base_dn, window, cx);
            replace_input(&self.bind_user_input, &saved_conn.bind_user, window, cx);
            replace_input(&self.bind_password_input, &saved_conn.bind_pass, window, cx);

            self.use_ssl = saved_conn.use_ssl;
            self.use_tls = saved_conn.use_tls;
        }

        cx.notify();
    }

    /// Create a new connection
    fn new_connection(&mut self, window: &mut Window, cx: &mut Context<Self>) {
        // Clear selection
        self.selected_connection = None;
        for conn in &mut self.saved_connections {
            conn.is_selected = false;
        }

        // Clear all form fields
        self.name_input
            .update(cx, |state, cx| state.replace("", window, cx));
        self.host_input
            .update(cx, |state, cx| state.replace("", window, cx));
        self.port_input
            .update(cx, |state, cx| state.replace("", window, cx));
        self.base_dn_input
            .update(cx, |state, cx| state.replace("", window, cx));
        self.bind_user_input
            .update(cx, |state, cx| state.replace("", window, cx));
        self.bind_password_input
            .update(cx, |state, cx| state.replace("", window, cx));

        self.use_ssl = false;
        self.use_tls = false;
        self.test_result = None;
        self.validation_errors.clear();

        cx.notify();
    }

    /// Delete a connection
    fn delete_connection(&mut self, name: &str, cx: &mut Context<Self>) {
        // Remove from config
        self.config
            .ldap
            .saved_connections
            .retain(|c| c.name != name);

        // Update the connections list UI
        self.saved_connections = self
            .config
            .ldap
            .saved_connections
            .iter()
            .map(|conn| ConnectionItem {
                name: conn.name.clone(),
                host: conn.host.clone(),
                port: conn.port,
                is_selected: false,
            })
            .collect();

        // Clear selection if this was the selected connection
        if self.selected_connection.as_ref() == Some(&name.to_string()) {
            self.selected_connection = None;
        }

        // Persist config to file
        if let Err(e) = self.config.save_to_default_path() {
            eprintln!("Failed to save config after deletion: {}", e);
        } else {
            println!(
                "💾 Connection '{}' deleted and saved to {}",
                name,
                Config::default_path().display()
            );
        }

        cx.notify();
    }

    /// Validate the current form inputs
    fn validate(&mut self, cx: &Context<Self>) -> bool {
        self.validation_errors.clear();

        let host = self.host_input.read(cx).text().to_string();
        let port_str = self.port_input.read(cx).text().to_string();

        if host.trim().is_empty() {
            self.validation_errors
                .insert("host".to_string(), "Host is required".to_string());
        }

        if port_str.trim().is_empty() {
            self.validation_errors
                .insert("port".to_string(), "Port is required".to_string());
        } else if port_str.parse::<u16>().is_err() {
            self.validation_errors.insert(
                "port".to_string(),
                "Port must be a valid number between 1 and 65535".to_string(),
            );
        }

        self.validation_errors.is_empty()
    }

    /// Get connection settings from form
    fn get_connection_settings(&self, cx: &Context<Self>) -> ConnectionSettings {
        let host = self.host_input.read(cx).text().to_string();
        let port = self
            .port_input
            .read(cx)
            .text()
            .to_string()
            .parse::<u16>()
            .unwrap_or(389);
        let base_dn = self.base_dn_input.read(cx).text().to_string();
        let bind_user = self.bind_user_input.read(cx).text().to_string();
        let bind_pass = self.bind_password_input.read(cx).text().to_string();

        ConnectionSettings {
            host,
            port,
            base_dn,
            bind_user,
            bind_pass,
            use_ssl: self.use_ssl,
            use_tls: self.use_tls,
        }
    }

    /// Handle test connection action
    fn test_connection(&mut self, cx: &mut Context<Self>) {
        if !self.validate(cx) {
            cx.notify();
            return;
        }

        self.is_testing = true;
        self.test_result = None;
        cx.notify();

        let settings = self.get_connection_settings(cx);

        // Test the connection
        match LdapClient::connect(&settings) {
            Ok(mut client) => match client.bind() {
                Ok(_) => {
                    self.test_result = Some(Ok("✓ Connection successful!".to_string()));
                }
                Err(e) => {
                    self.test_result = Some(Err(format!("✗ Authentication failed: {}", e)));
                }
            },
            Err(e) => {
                self.test_result = Some(Err(format!("✗ Connection failed: {}", e)));
            }
        }

        self.is_testing = false;
        cx.notify();
    }

    /// Connect to the LDAP server and close the config window
    fn connect_and_close(&mut self, window: &mut Window, cx: &mut Context<Self>) {
        log::info!("🔌 [ConfigView] Connect button clicked");

        // First validate the connection settings
        if !self.validate(cx) {
            log::warn!("❌ [ConfigView] Validation failed");
            cx.notify();
            return;
        }

        log::info!("✅ [ConfigView] Validation passed");

        // Save the connection first
        self.save_connection(window, cx);

        // Get connection settings
        let settings = self.get_connection_settings(cx);

        log::info!(
            "🌐 [ConfigView] Attempting to connect to {}:{} with base DN: {}",
            settings.host,
            settings.port,
            settings.base_dn
        );

        // Attempt to connect to LDAP server
        match LdapClient::connect(&settings) {
            Ok(mut client) => {
                log::info!("✅ [ConfigView] Connected to LDAP server, attempting to bind...");
                // Attempt to bind (authenticate)
                match client.bind() {
                    Ok(()) => {
                        log::info!("✅ [ConfigView] Authentication successful!");
                        // Connection successful - update app state
                        {
                            let mut state = self.app_state.write();
                            state.active_client = Some(client);
                            state.is_connected = true;
                            state.current_base_dn = Some(settings.base_dn.clone());
                            state.selected_connection =
                                Some(self.name_input.read(cx).text().to_string());
                            state.status_message =
                                Some(format!("Connected to {} successfully", settings.host));
                            log::info!(
                                "📝 [ConfigView] App state updated: is_connected={}, base_dn={:?}",
                                state.is_connected,
                                state.current_base_dn
                            );
                        }

                        log::info!("🪟 [ConfigView] Closing config window...");
                        // Close the config window
                        window.remove_window();
                    }
                    Err(e) => {
                        // Authentication failed
                        log::error!("❌ [ConfigView] Authentication failed: {}", e);
                        self.test_result = Some(Err(format!("Authentication failed: {}", e)));
                        cx.notify();
                    }
                }
            }
            Err(e) => {
                // Connection failed
                log::error!("❌ [ConfigView] Connection failed: {}", e);
                self.test_result = Some(Err(format!("Connection failed: {}", e)));
                cx.notify();
            }
        }
    }

    /// Handle save connection action
    fn save_connection(&mut self, window: &mut Window, cx: &mut Context<Self>) {
        if !self.validate(cx) {
            cx.notify();
            return;
        }

        let settings = self.get_connection_settings(cx);

        // Get connection name from input, or use host as fallback
        let name_text = self.name_input.read(cx).text().to_string();
        let connection_name = if let Some(selected) = &self.selected_connection {
            // Updating existing connection - use the new name if provided, otherwise keep existing
            if name_text.trim().is_empty() {
                selected.clone()
            } else {
                name_text.trim().to_string()
            }
        } else {
            // New connection - use name input if provided, otherwise use host
            if name_text.trim().is_empty() {
                settings.host.clone()
            } else {
                name_text.trim().to_string()
            }
        };

        // Create saved connection
        let saved_connection = moribito_core::config::SavedConnection {
            name: connection_name.clone(),
            host: settings.host.clone(),
            port: settings.port,
            base_dn: settings.base_dn.clone(),
            bind_user: settings.bind_user.clone(),
            bind_pass: settings.bind_pass.clone(),
            use_ssl: settings.use_ssl,
            use_tls: settings.use_tls,
        };

        // Update or add to config
        if let Some(pos) = self
            .config
            .ldap
            .saved_connections
            .iter()
            .position(|c| c.name == connection_name)
        {
            // Update existing
            self.config.ldap.saved_connections[pos] = saved_connection;
        } else {
            // Add new
            self.config.ldap.saved_connections.push(saved_connection);
        }

        // Update the connections list UI
        self.saved_connections = self
            .config
            .ldap
            .saved_connections
            .iter()
            .map(|conn| ConnectionItem {
                name: conn.name.clone(),
                host: conn.host.clone(),
                port: conn.port,
                is_selected: conn.name == connection_name,
            })
            .collect();

        // Set this as the selected connection
        self.selected_connection = Some(connection_name.clone());

        // Persist config to file
        if let Err(e) = self.config.save_to_default_path() {
            eprintln!("Failed to save config to file: {}", e);
            self.test_result = Some(Err(format!(
                "Saved in memory, but failed to write to disk: {}",
                e
            )));
        } else {
            println!(
                "💾 Connection '{}' saved to {}",
                connection_name,
                Config::default_path().display()
            );
            self.test_result = Some(Ok("✓ Connection settings saved!".to_string()));
        }

        cx.notify();
    }

    /// Render a labeled input field with optional placeholder and error message
    fn render_input_field(
        &self,
        label: String,
        input: &Entity<InputState>,
        error: Option<&String>,
        cx: &App,
    ) -> Div {
        div()
            .flex()
            .flex_col()
            .gap_1p5()
            .w_full()
            .child(
                div()
                    .text_xs()
                    .font_weight(FontWeight::MEDIUM)
                    .text_color(cx.theme().muted_foreground)
                    .child(label),
            )
            .child(
                div()
                    .w_full()
                    .bg(cx.theme().secondary)
                    .rounded(px(4.0))
                    .child(Input::new(input).appearance(false).w_full().text_xs()),
            )
            .when_some(error, |el, err| {
                el.child(
                    div()
                        .text_xs()
                        .text_color(gpui::red())
                        .pt_1()
                        .child(err.clone()),
                )
            })
    }

    /// Render a section with a title and content
    fn render_section(&self, title: String, content: Div, cx: &App) -> Div {
        div()
            .flex()
            .flex_col()
            .gap_3()
            .pt_3()
            .w_full()
            .child(
                div()
                    .text_sm()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(cx.theme().foreground)
                    .child(title),
            )
            .child(content)
    }

    /// Render the configuration form (right panel)
    fn render_config_form(&self, cx: &mut Context<Self>) -> impl IntoElement {
        let name_input = self.name_input.clone();
        let host_input = self.host_input.clone();
        let port_input = self.port_input.clone();
        let base_dn_input = self.base_dn_input.clone();
        let bind_user_input = self.bind_user_input.clone();
        let bind_password_input = self.bind_password_input.clone();

        let validation_errors = self.validation_errors.clone();
        let test_result = self.test_result.clone();
        let is_testing = self.is_testing;
        let use_ssl = self.use_ssl;
        let use_tls = self.use_tls;

        v_flex()
            .id("config-form-scroll")
            .w_full()
            .h_full()
            .bg(cx.theme().background)
            .overflow_y_scroll()
            .child(
                div()
                    .flex()
                    .flex_col()
                    .gap_2()
                    .pt_2()
                    .pb_8()
                    .px_8()
                    .w_full()
                    .child(
                        self.render_section(
                            "Connection".to_string(),
                            div()
                                .flex()
                                .flex_col()
                                .gap_3()
                                .child(self.render_input_field(
                                    "Name (optional)".to_string(),
                                    &name_input,
                                    validation_errors.get("name"),
                                    cx,
                                )),
                            cx,
                        ),
                    )
                    .child(
                        self.render_section(
                            "Server".to_string(),
                            div()
                                .flex()
                                .flex_col()
                                .gap_3()
                                .child(
                                    div()
                                        .flex()
                                        .gap_3()
                                        .child(div().flex_1().child(self.render_input_field(
                                            "Host".to_string(),
                                            &host_input,
                                            validation_errors.get("host"),
                                            cx,
                                        )))
                                        .child(div().w(px(120.0)).child(self.render_input_field(
                                            "Port".to_string(),
                                            &port_input,
                                            validation_errors.get("port"),
                                            cx,
                                        ))),
                                )
                                .child(self.render_input_field(
                                    "Base DN".to_string(),
                                    &base_dn_input,
                                    validation_errors.get("base_dn"),
                                    cx,
                                )),
                            cx,
                        ),
                    )
                    .child(
                        self.render_section(
                            "Authentication".to_string(),
                            div()
                                .flex()
                                .flex_col()
                                .gap_3()
                                .child(self.render_input_field(
                                    "Bind User".to_string(),
                                    &bind_user_input,
                                    validation_errors.get("bind_user"),
                                    cx,
                                ))
                                .child(self.render_input_field(
                                    "Password".to_string(),
                                    &bind_password_input,
                                    validation_errors.get("password"),
                                    cx,
                                )),
                            cx,
                        ),
                    )
                    .child(
                        self.render_section(
                            "Security".to_string(),
                            div()
                                .flex()
                                .flex_row()
                                .gap_3()
                                .child(
                                    CheckboxCard::new("use_ssl")
                                        .label("Use SSL/TLS (LDAPS)")
                                        .checked(use_ssl)
                                        .on_click(cx.listener(|view, checked, _window, cx| {
                                            view.use_ssl = *checked;
                                            cx.notify();
                                        })),
                                )
                                .child(
                                    CheckboxCard::new("use_tls")
                                        .label("Use StartTLS")
                                        .checked(use_tls)
                                        .on_click(cx.listener(|view, checked, _window, cx| {
                                            view.use_tls = *checked;
                                            cx.notify();
                                        })),
                                ),
                            cx,
                        ),
                    )
                    .child(
                        div()
                            .flex()
                            .justify_end()
                            .gap_2()
                            .pt_6()
                            .text_xs()
                            .child(
                                Button::new("test")
                                    .primary()
                                    .icon(IconName::Globe)
                                    .label("Test")
                                    .small()
                                    .disabled(is_testing)
                                    .on_click(cx.listener(|view, _event, _window, cx| {
                                        view.test_connection(cx);
                                    })),
                            )
                            .child(
                                Button::new("save")
                                    .info()
                                    .icon(IconName::Star)
                                    .label("Save")
                                    .small()
                                    .on_click(cx.listener(|view, _event, window, cx| {
                                        view.save_connection(window, cx);
                                    })),
                            )
                            .child(
                                Button::new("connect")
                                    .success()
                                    .icon(IconName::ArrowRight)
                                    .label("Connect")
                                    .small()
                                    .on_click(cx.listener(|view, _event, window, cx| {
                                        view.connect_and_close(window, cx);
                                    })),
                            ),
                    ),
            )
    }
}

impl Focusable for ConfigView {
    fn focus_handle(&self, _cx: &App) -> FocusHandle {
        self.focus_handle.clone()
    }
}

impl Render for ConfigView {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let connections = self.saved_connections.clone();

        // Create resizable split layout
        h_resizable("config-view-split")
            .child(
                // Left panel - Connections list
                resizable_panel()
                    .size(px(250.0))
                    .size_range(px(200.0)..px(400.0))
                    .child(
                        ConnectionsList::new(connections)
                            .on_select(cx.listener(|view, name, window, cx| {
                                view.select_connection(name, window, cx);
                            }))
                            .on_new(cx.listener(|view, _event, window, cx| {
                                view.new_connection(window, cx);
                            }))
                            .on_delete(cx.listener(|view, name, _window, cx| {
                                view.delete_connection(name, cx);
                            })),
                    ),
            )
            .child(
                // Right panel - Configuration form
                div()
                    .flex_1()
                    .child(self.render_config_form(cx))
                    .into_any_element(),
            )
    }
}
