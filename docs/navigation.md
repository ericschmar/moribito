# Navigation

## General Controls

- **Tab** - Switch between Tree, Query, and Record views
- **1/2/3** - Jump directly to Start/Tree/Query/Record view
- **Ctrl+C** or **q** - Exit application
- **?** - Toggle help modal (context-sensitive)
- **Ctrl+R** - Refresh/reconnect to server

## Start/Configuration View

- **↑/↓** or **j/k** - Navigate through configuration fields
- **Enter** - Edit field value or execute action
- **←/→** or **h/l** - Navigate between saved connections (when in connection list)
- **Escape** - Cancel editing or dialog

### Connection Management
- **Add New Connection** - Save current settings as a new named connection
- **Delete Connection** - Remove the selected saved connection
- **Save Current as New** - Create a new connection from current configuration
- Navigate between saved connections and press **Enter** to switch to that connection

## Tree View

- **↑/↓** or **j/k** - Navigate up/down in tree
- **Page Up/Down** - Navigate by page
- **Enter** or **→** - Expand folder or view record
- **←** - Collapse folder or go up one level
- **/** - Focus search/filter input
- **Escape** - Clear search, return to tree navigation
- **Home/End** - Jump to beginning/end of current level

## Record View

- **↑/↓** or **j/k** - Navigate through attributes
- **Page Up/Down** - Navigate by page
- **Enter** - Copy selected attribute value to clipboard
- **Escape** or **←** - Return to previous view
- **/** - Focus search/filter for attributes

## Query View

- **/** or **Escape** - Focus query input
- **Ctrl+Enter** or **Ctrl+J** - Execute query
- **Ctrl+F** - Format query with proper indentation
- **Escape** - Clear query
- **Ctrl+V** - Paste from clipboard
- **↑/↓** - Navigate results (when not in input mode)
- **Page Up/Down** - Navigate by page (automatically loads more results)
- **Enter** - View selected record

> **Note**: The Query View uses automatic pagination to efficiently handle large result sets. When you scroll near the end of loaded results, the next page is automatically fetched from the LDAP server.

## Query Formatting

The **Ctrl+F** key combination formats complex LDAP queries with proper indentation for better readability:

```
# Before formatting:
(&(objectClass=person)(|(cn=john*)(sn=smith*))(department=engineering))

# After formatting (Ctrl+F):
(&
  (objectClass=person)
  (|
    (cn=john*)
    (sn=smith*)
  )
  (department=engineering)
)
```