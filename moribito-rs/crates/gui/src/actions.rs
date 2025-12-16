//! Actions for the Moribito GUI application
//!
//! Actions represent user intents and are used to implement keyboard-driven UI.
//! They can be bound to keys in the keymap and listeners in the element tree.

use gpui::{actions, Action};
use schemars::JsonSchema;
use serde::Deserialize;

// Configuration screen actions
actions!(
    moribito,
    [
        // File menu actions
        NewConnection,
        CloseWindow,
        // Edit menu actions
        Cut,
        Copy,
        Paste,
        DeleteEntry,
        SelectAll,
        // View menu actions
        Refresh,
        ExpandAll,
        CollapseAll,
        // Connection actions
        Connect,
        Disconnect,
        TestConnection,
        SaveConnection,
        DeleteConnection,
        LoadConnection,
        // Browser actions
        ExpandNode,
        CollapseNode,
        RefreshTree,
        Search,
        ClearSearch,
        SelectEntry,
        // Form actions
        SubmitForm,
        CancelForm,
        ResetForm,
        // Navigation actions
        FocusNextField,
        FocusPreviousField,
        // Application actions
        OpenConfigWindow,
        ShowAbout,
        ShowDocumentation,
        Quit,
    ]
);

/// Action to select a saved connection by name
#[derive(Clone, Action, PartialEq, Eq, Deserialize, JsonSchema)]
#[action(namespace = moribito)]
pub struct SelectConnection {
    pub name: String,
}

/// Action to update a field value
#[derive(Clone, Action, PartialEq, Eq, Deserialize, JsonSchema)]
#[action(namespace = moribito)]
pub struct UpdateField {
    pub field: String,
    pub value: String,
}

/// Action to toggle a boolean setting
#[derive(Clone, Action, PartialEq, Eq, Deserialize, JsonSchema)]
#[action(namespace = moribito)]
pub struct ToggleSetting {
    pub setting: String,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_action_names() {
        // Verify action names are correctly formatted
        assert_eq!(TestConnection::name_for_type(), "moribito::TestConnection");
        assert_eq!(SaveConnection::name_for_type(), "moribito::SaveConnection");
    }

    #[test]
    fn test_select_connection_action() {
        let action = SelectConnection {
            name: "test-server".to_string(),
        };
        assert_eq!(action.name, "test-server");
    }

    #[test]
    fn test_update_field_action() {
        let action = UpdateField {
            field: "host".to_string(),
            value: "ldap.example.com".to_string(),
        };
        assert_eq!(action.field, "host");
        assert_eq!(action.value, "ldap.example.com");
    }

    #[test]
    fn test_toggle_setting_action() {
        let action = ToggleSetting {
            setting: "use_ssl".to_string(),
        };
        assert_eq!(action.setting, "use_ssl");
    }
}
