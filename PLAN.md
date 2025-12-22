# Tree View Redesign - Navigate INTO OUs

## Summary

Redesign the LDAP tree view to support **navigating into OUs** (context change + new LDAP query) instead of in-place expansion. Add a **Zed-style action bar** with breadcrumb navigation and action buttons. Simplify virtual grouping to only **[Service Accounts]** for `cn=` entries.

---

## Requirements

1. **Navigate INTO OUs**: Clicking an OU changes context and loads its children via new LDAP query
2. **Action Bar**: Zed-style header with breadcrumb (left) and icon buttons (right)
3. **Virtual Grouping**: Keep only `[Service Accounts]` for `cn=` entries; remove `[Root Users]`
4. **Remove OU Dropdown**: Navigation happens by clicking OUs directly

---

## Implementation Phases

### Phase 1: State Management (`app_state.rs`)

**Add to TreeState:**
```rust
pub struct TreeState {
    pub expanded_nodes: HashSet<String>,
    pub node_children: HashMap<String, Vec<TreeNode>>,
    pub loading_nodes: HashSet<String>,
    
    // NEW
    pub current_context_dn: String,      // DN we're viewing children of
    pub navigation_stack: Vec<String>,   // Parent DNs for breadcrumb/up
}
// REMOVE: ou_filter, available_ous
```

**Add navigation methods:**
- `navigate_into(target_dn)` - Push current to stack, set new context
- `navigate_up()` - Pop stack, return to parent
- `navigate_to(target_dn)` - Jump to specific breadcrumb level
- `get_breadcrumb_path()` - Return `Vec<(label, dn)>` for display

---

### Phase 2: New Actions (`actions.rs`)

```rust
#[action] pub struct NavigateIntoOu { pub dn: String }
#[action] pub struct NavigateToBreadcrumb { pub dn: String }
actions!(moribito, [NavigateUp, NavigateToRoot]);
```

---

### Phase 3: Create Action Bar Component

**New file:** `components/tree_panel_header.rs`

```rust
pub struct TreePanelHeader {
    app_state: SharedAppState,
}

// Renders:
h_flex()
    .h(px(32.0))
    .bg(theme.muted)
    .border_b_1()
    // Left: Breadcrumb (clickable segments)
    .child(render_breadcrumb())  // dc=example > ou=users > ou=admins
    .child(div().flex_1())       // Spacer
    // Right: Action buttons
    .child(
        Button::new("up").icon(IconName::ArrowUp).ghost().xsmall()
        Button::new("refresh").icon(IconName::RefreshCw).ghost().xsmall()
        Button::new("add").icon(IconName::Plus).ghost().xsmall()
        Button::new("more").icon(IconName::MoreHorizontal).ghost().xsmall()
    )
```

---

### Phase 4: Update TreeView (`tree_view.rs`)

**Remove:**
- `extract_ous()` method
- `apply_ou_filter()` method
- `partition_by_uid()` and `[Root Users]` grouping
- `available_ous` handling

**Modify `organize_entries()`:**
```rust
fn organize_entries(&self, nodes: Vec<TreeNode>) -> Vec<TreeNode> {
    // Only group cn= entries into [Service Accounts]
    let (service_accounts, others) = nodes.into_iter().partition(|n| {
        n.name.to_lowercase().starts_with("cn=")
    });
    
    let mut organized = Vec::new();
    if !service_accounts.is_empty() {
        organized.push(Self::create_virtual_group("[Service Accounts]", service_accounts));
    }
    organized.extend(others);
    organized
}
```

**Modify click handler:**
```rust
.on_click(move |_, _, cx| {
    if is_virtual {
        // Toggle expansion for virtual nodes
        state.toggle_node_expansion(&dn);
    } else if name.to_lowercase().starts_with("ou=") {
        // Navigate INTO OUs
        cx.dispatch_action(&NavigateIntoOu { dn: dn.clone() });
    } else {
        // Select regular entries
        cx.emit(SelectTreeEntry { dn: dn.clone() });
    }
})
```

**Modify `reload_tree()`:**
- Use `current_context_dn` instead of base_dn parameter
- No OU filter application

---

### Phase 5: Update BrowserView (`browser_view.rs`)

**Remove:**
- `ou_filter: Entity<OuFilter>` field
- `handle_filter_by_ou()` handler
- OuFilter initialization and rendering

**Add:**
- `tree_panel_header: TreePanelHeader` field
- Navigation action handlers:
  - `handle_navigate_into_ou()` - Call `app_state.navigate_into()`, reload tree
  - `handle_navigate_up()` - Call `app_state.navigate_up()`, reload tree
  - `handle_navigate_to_breadcrumb()` - Call `app_state.navigate_to()`, reload tree

**Update render():**
```rust
v_flex()
    .child(self.tree_panel_header.render(window, cx))  // NEW action bar
    .child(/* tree view */)
```

---

### Phase 6: Cleanup

- **Delete:** `components/ou_filter.rs`
- **Remove:** `FilterByOu` action
- **Update:** `components/mod.rs` - add `tree_panel_header`, remove `ou_filter`

---

## Files to Modify

| File | Action | Changes |
|------|--------|---------|
| `app_state.rs` | MODIFY | Add navigation state + methods |
| `actions.rs` | MODIFY | Add 4 navigation actions |
| `tree_panel_header.rs` | CREATE | New action bar component |
| `tree_view.rs` | MODIFY | Remove OU filter, change click behavior |
| `browser_view.rs` | MODIFY | Replace OuFilter with TreePanelHeader |
| `ou_filter.rs` | DELETE | No longer needed |
| `components/mod.rs` | MODIFY | Update exports |

---

## Technical Notes

**OU Navigation Detection:** Assume all OUs are navigable (no extra LDAP query to check for children). If OU is empty, show empty state - user can navigate back up.

**Caching:** Reuse existing `node_children` cache. Check cache before LDAP query when navigating.

**Breadcrumb Overflow:** For deep hierarchies, consider truncating middle segments with "..." and showing full path on hover.

---

## Testing Checklist

- [ ] Navigate into OU with children
- [ ] Navigate up to parent
- [ ] Click breadcrumb to jump to level
- [ ] Service accounts grouped at each level
- [ ] Regular entries (uid=) shown flat
- [ ] Empty OU shows appropriate state
- [ ] Refresh reloads current context
- [ ] Entry selection shows details
