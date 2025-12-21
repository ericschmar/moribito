# Improved LDAP Tree View Design

## Overview
Redesign the tree view to provide better navigation for LDAP directories by adding an OU filter dropdown and organizing entries by type, while maintaining full visibility of the LDAP structure.

## User Requirements
1. **Keep full tree structure visible** - Don't hide entries, show the true LDAP hierarchy
2. **OU Filter Dropdown (Option A)** - Dropdown at top showing "All", plus individual OUs, defaults to "All"
3. **Separate CNs from other entries** - Group service accounts (cn=admin, etc.) separately
4. **Support typical workflows**: Browse from base DN, search for specific entries, search by attributes

## Current Issues
Based on the log output provided:
- Flat list of 19 entries at root level mixing OUs, UIDs, and CNs
- No visual separation between entry types
- Hard to navigate when many entries exist
- No filtering mechanism

## Design Approach

### 1. OU Filter Dropdown Component
**Location**: Top of tree view panel
**Behavior**:
- Dropdown options: "All" (default), then each OU found under base DN
- When "All" selected: Show full tree hierarchy
- When specific OU selected: Filter tree to show only that OU and its descendants
- Dynamically populated based on LDAP search results

### 2. Tree Organization
**Grouping Strategy**:
```
dc=example,dc=com (base)
├── [Service Accounts] (virtual grouping node)
│   ├── cn=admin
│   └── cn=read-only-admin
├── [Root Users] (virtual grouping node, if any UIDs at root)
│   ├── uid=newton
│   ├── uid=einstein
│   └── ...
├── ou=mathematicians
│   └── (children loaded on expand)
├── ou=scientists
│   └── (children loaded on expand)
└── ou=chemists
    └── (children loaded on expand)
```

**Virtual Grouping Nodes**:
- Not real LDAP entries, just UI organization
- Collapsible like regular nodes
- Visually distinct (italic text, different icon)
- Only shown when entries exist for that type

### 3. Entry Type Detection
Categorize entries based on their RDN (Relative Distinguished Name):
- **Service Accounts**: `cn=*` at root level
- **Users**: `uid=*` at root level  
- **Organizational Units**: `ou=*`
- **Groups**: `cn=*` inside OUs (context-dependent)
- **Other**: Everything else

## Implementation Status

### ✅ Phase 1: COMPLETED - Create OU Filter Dropdown Component
**New File**: `moribito-rs/crates/gui/src/components/ou_filter.rs`

**Completed:**
- ✅ Created `OuOption` struct in `app_state.rs` with `label` and `dn` fields
- ✅ Added `ou_filter` and `available_ous` fields to `TreeState`
- ✅ Implemented helper methods: `set_ou_filter()`, `update_available_ous()`, `extract_ous_from_cache()`
- ✅ Added `FilterByOu` action to `actions.rs`
- ✅ Created `OuFilter` component (simplified display version for Phase 1)
- ✅ Exported component in `components/mod.rs`
- ✅ Successfully builds without errors

**Note:** Phase 1 implements a basic display component. Interactive dropdown will be enhanced in later phases.

### ✅ Phase 2: COMPLETED - Add Tree Grouping Logic
**Modified File**: `moribito-rs/crates/gui/src/components/tree_view.rs`

**Completed:**
- ✅ Implemented `organize_entries()` method to group entries by type
- ✅ Created `partition_by_cn()` and `partition_by_uid()` helper functions
- ✅ Implemented `create_virtual_group()` to create virtual grouping nodes
- ✅ Added `apply_ou_filter()` method for OU filtering
- ✅ Added `is_at_root_level()` helper to detect root-level entries
- ✅ Updated `set_root_nodes()` to extract OUs, organize entries, and apply filters
- ✅ Modified tree rendering to handle virtual nodes with special icon (Layers)
- ✅ Prevented LDAP operations on virtual nodes (no load_children, no select events)
- ✅ Successfully builds without errors

**Tree Organization:**
- Service accounts (`cn=*` at root) → `[Service Accounts]` virtual group
- Root users (`uid=*` at root) → `[Root Users]` virtual group  
- OUs and other entries → shown directly without grouping

### ✅ Phase 3: COMPLETED - Integrate Filter into Browser View
**Modified File**: `moribito-rs/crates/gui/src/views/browser_view.rs`

**Completed:**
- ✅ Added `OuFilter` component to `BrowserView` struct
- ✅ Initialized `OuFilter` in `BrowserView::new()`
- ✅ Implemented `handle_filter_by_ou()` action handler
- ✅ Added `.on_action()` listener for `FilterByOu` action
- ✅ Updated tree panel UI to include OuFilter at the top
- ✅ Added border separator between filter and tree
- ✅ Filter reloads tree when OU selection changes
- ✅ Status bar shows current filter selection
- ✅ Made `OuFilter` cloneable for BrowserView
- ✅ Successfully builds without errors

**UI Layout:**
```
┌─────────────────────────────────┐
│ Filter by OU:  [All]            │  ← OuFilter component
├─────────────────────────────────┤
│ 📚 [Service Accounts]           │
│ 📚 [Root Users]                 │
│ 📂 ou=mathematicians            │  ← TreeView component
│ 📂 ou=scientists                │
│ ...                             │
└─────────────────────────────────┘
```

## Implementation Plan

### Phase 1: Create OU Filter Dropdown Component ✅ DONE
**New File**: `moribito-rs/crates/gui/src/components/ou_filter.rs`

**Component Structure**:
```rust
pub struct OuFilter {
    app_state: SharedAppState,
    selected_ou: String, // "All" or specific OU DN
    available_ous: Vec<OuOption>,
}

pub struct OuOption {
    pub label: String,    // Display name (e.g., "mathematicians")
    pub dn: String,       // Full DN (e.g., "ou=mathematicians,dc=example,dc=com")
}
```

**Features**:
- Renders as a dropdown/select component
- Populated by scanning cached tree nodes for OU entries
- Emits `FilterByOu` action when selection changes
- Persists selection in app state

**UI Libraries**:
- Use gpui-component's existing components as base
- May need custom dropdown if not available (check gpui-component docs)

### Phase 2: Add Tree Grouping Logic
**Modified File**: `moribito-rs/crates/gui/src/components/tree_view.rs`

**New Functions**:
```rust
impl TreeView {
    /// Group entries by type for better organization
    fn organize_entries(&self, nodes: Vec<TreeNode>) -> Vec<TreeNode> {
        // Separate entries by type
        let (service_accounts, remaining) = partition_by_cn(nodes);
        let (root_users, ous_and_others) = partition_by_uid(remaining);
        
        let mut organized = vec![];
        
        // Add service accounts group if any exist
        if !service_accounts.is_empty() {
            organized.push(create_virtual_group(
                "[Service Accounts]",
                service_accounts
            ));
        }
        
        // Add root users group if any exist
        if !root_users.is_empty() {
            organized.push(create_virtual_group(
                "[Root Users]",
                root_users
            ));
        }
        
        // Add OUs and other entries directly
        organized.extend(ous_and_others);
        
        organized
    }
    
    /// Create a virtual grouping node (not a real LDAP entry)
    fn create_virtual_group(label: &str, children: Vec<TreeNode>) -> TreeNode {
        TreeNode {
            dn: format!("__virtual_{}", label), // Special DN prefix
            name: label.to_string(),
            children: Some(children),
            is_loaded: true,
        }
    }
    
    /// Apply OU filter to tree nodes
    fn apply_ou_filter(&self, nodes: Vec<TreeNode>, filter: &str) -> Vec<TreeNode> {
        if filter == "All" {
            return nodes;
        }
        
        // Filter to show only the selected OU and its descendants
        nodes.into_iter()
            .filter(|node| node.dn.starts_with(filter) || node.dn == filter)
            .collect()
    }
}
```

**Helper Functions**:
```rust
fn partition_by_cn(nodes: Vec<TreeNode>) -> (Vec<TreeNode>, Vec<TreeNode>) {
    nodes.into_iter().partition(|n| {
        n.name.starts_with("cn=") && is_at_root_level(&n.dn)
    })
}

fn partition_by_uid(nodes: Vec<TreeNode>) -> (Vec<TreeNode>, Vec<TreeNode>) {
    nodes.into_iter().partition(|n| {
        n.name.starts_with("uid=") && is_at_root_level(&n.dn)
    })
}

fn is_at_root_level(dn: &str) -> bool {
    // Check if DN has only one RDN component before base DN
    // e.g., "uid=newton,dc=example,dc=com" is root level
    // but "uid=newton,ou=users,dc=example,dc=com" is not
    dn.matches(',').count() == 2 // Adjust based on base DN depth
}
```

### Phase 3: Update App State
**Modified File**: `moribito-rs/crates/gui/src/app_state.rs`

**Add to TreeState**:
```rust
pub struct TreeState {
    // ... existing fields ...
    
    /// Current OU filter ("All" or specific OU DN)
    pub ou_filter: String,
    
    /// List of available OUs for filtering
    pub available_ous: Vec<String>,
}

impl AppState {
    /// Set the OU filter
    pub fn set_ou_filter(&mut self, filter: String) {
        self.tree_state.ou_filter = filter;
    }
    
    /// Update available OUs list
    pub fn update_available_ous(&mut self, ous: Vec<String>) {
        self.tree_state.available_ous = ous;
    }
    
    /// Extract OUs from cached children
    pub fn extract_ous_from_cache(&self) -> Vec<String> {
        self.tree_state.node_children
            .values()
            .flatten()
            .filter(|node| node.name.starts_with("ou="))
            .map(|node| node.dn.clone())
            .collect()
    }
}
```

### Phase 4: Add Actions
**Modified File**: `moribito-rs/crates/gui/src/actions.rs`

```rust
/// Filter tree by OU
#[derive(Clone, PartialEq)]
pub struct FilterByOu {
    pub ou_dn: String, // "All" or specific OU DN
}

actions!(moribito, [
    // ... existing actions ...
    FilterByOu,
]);
```

### Phase 5: Update TreeView Rendering
**Modified File**: `moribito-rs/crates/gui/src/components/tree_view.rs`

**Update `set_root_nodes`**:
```rust
pub fn set_root_nodes(&mut self, nodes: Vec<TreeNode>, cx: &mut App) {
    // Extract OUs for filter dropdown
    let ous: Vec<String> = nodes.iter()
        .filter(|n| n.name.starts_with("ou="))
        .map(|n| n.dn.clone())
        .collect();
    
    self.app_state.write().update_available_ous(ous);
    
    // Organize entries by type
    let organized = self.organize_entries(nodes);
    
    // Apply current OU filter
    let filter = self.app_state.read().tree_state.ou_filter.clone();
    let filtered = self.apply_ou_filter(organized, &filter);
    
    // Convert to TreeItems
    let items: Vec<TreeItem> = filtered
        .iter()
        .map(|node| self.tree_node_to_tree_item(node, cx))
        .collect();

    self.tree_state.update(cx, |state, cx| {
        *state = TreeState::new(cx).items(items);
    });
}
```

**Update icon selection for virtual nodes**:
```rust
// In render_tree closure
let icon = if node.dn.starts_with("__virtual_") {
    IconName::Layers // or another icon for groups
} else if !is_folder {
    IconName::File
} else if is_expanded {
    IconName::FolderOpen
} else {
    IconName::Folder
};
```

### Phase 6: Integrate Filter into Browser View
**Modified File**: `moribito-rs/crates/gui/src/views/browser_view.rs`

**Add OuFilter component**:
```rust
pub struct BrowserView {
    // ... existing fields ...
    ou_filter: OuFilter,
}

impl BrowserView {
    pub fn new(...) -> Self {
        // ... existing initialization ...
        let ou_filter = OuFilter::new(app_state.clone());
        
        Self {
            // ... existing fields ...
            ou_filter,
        }
    }
    
    fn handle_filter_by_ou(&mut self, action: &FilterByOu, window: &mut Window, cx: &mut Context<Self>) {
        // Update app state
        self.app_state.write().set_ou_filter(action.ou_dn.clone());
        
        // Reload tree with new filter
        if let Some(base_dn) = self.app_state.read().current_base_dn.clone() {
            self.tree_view.update(cx, |tree, cx| {
                tree.reload_tree(&base_dn, window, cx);
            });
        }
    }
}

impl Render for BrowserView {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // ... existing code ...
        .on_action(cx.listener(Self::handle_filter_by_ou))
        .child(
            // ... search bar ...
        )
        .child(
            // Main content with tree
            div().flex_1().w_full().child(
                h_resizable("browser-main-split")
                    .child(
                        resizable_panel()
                            .size(px(300.0))
                            .size_range(px(200.0)..px(600.0))
                            .child(
                                v_flex()
                                    .h_full()
                                    .w_full()
                                    .border_r_1()
                                    .border_color(border)
                                    // ADD OU FILTER HERE
                                    .child(
                                        div()
                                            .p_2()
                                            .border_b_1()
                                            .border_color(border)
                                            .child(self.ou_filter.render(window, cx))
                                    )
                                    // Tree view below filter
                                    .child(
                                        div()
                                            .id("tree-panel-scroll")
                                            .flex_1()
                                            .overflow_y_scroll()
                                            .child(
                                                self.tree_view.update(cx, |tree, cx| tree.render(window, cx))
                                            )
                                    )
                            )
                    )
                    // ... rest of layout ...
            )
        )
    }
}
```

## Visual Design

### Filter Dropdown
```
┌─────────────────────────────────┐
│ Filter by OU:  [All ▼]          │
├─────────────────────────────────┤
│ 📁 [Service Accounts]           │
│   ├─ 👤 cn=admin                │
│   └─ 👤 cn=read-only-admin      │
│ 📁 [Root Users]                 │
│   ├─ 👤 uid=newton              │
│   ├─ 👤 uid=einstein            │
│   └─ ...                        │
│ 📂 ou=mathematicians            │
│ 📂 ou=scientists                │
│ 📂 ou=chemists                  │
└─────────────────────────────────┘
```

### With OU Selected
```
┌─────────────────────────────────┐
│ Filter by OU:  [mathematicians ▼]│
├─────────────────────────────────┤
│ 📂 ou=mathematicians            │
│   ├─ 👤 uid=gauss               │
│   ├─ 👤 uid=euler               │
│   └─ 👤 uid=riemann             │
└─────────────────────────────────┘
```

## Files to Modify

1. **NEW**: `moribito-rs/crates/gui/src/components/ou_filter.rs` - OU filter dropdown component
2. **MODIFY**: `moribito-rs/crates/gui/src/components/tree_view.rs` - Add grouping and filtering logic
3. **MODIFY**: `moribito-rs/crates/gui/src/components/mod.rs` - Export OuFilter component
4. **MODIFY**: `moribito-rs/crates/gui/src/app_state.rs` - Add OU filter state
5. **MODIFY**: `moribito-rs/crates/gui/src/actions.rs` - Add FilterByOu action
6. **MODIFY**: `moribito-rs/crates/gui/src/views/browser_view.rs` - Integrate filter component

## Testing Considerations

1. **Test with empty OUs** - Ensure virtual groups don't show when no entries exist
2. **Test filter persistence** - OU filter should persist when expanding/collapsing nodes
3. **Test with deep hierarchies** - Ensure filtering works with nested OUs
4. **Test virtual node interaction** - Virtual group nodes should expand/collapse but not emit selection events for LDAP operations

## Edge Cases to Handle

1. **No OUs in directory** - Filter dropdown shows only "All"
2. **Mixed entry types in OUs** - Children of OUs are not grouped (only root level grouping)
3. **Virtual node DNs** - Must not attempt LDAP operations on `__virtual_*` DNs
4. **Filter state on reconnect** - Reset to "All" when connecting to new server
5. **Dynamic OU addition** - Update filter dropdown when new OUs are discovered

## Future Enhancements (Out of Scope)

- Multi-select filtering (show multiple OUs)
- Custom grouping rules configurable by user
- Save/restore filter preferences per connection
- Type-based filtering in addition to OU filtering
- Quick search/filter within current view
