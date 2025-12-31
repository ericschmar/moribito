LDAP Code Editor Component Implementation Plan

     Overview

     Create a minimal, standalone code editor component 
     with LDAP filter syntax highlighting for the
     Moribito LDAP client. This will replace the current
     basic TextArea in QueryPanel with a proper code
     editor featuring line numbers, synchronized
     scrolling, and colorized LDAP syntax.

     User Requirements

     - Minimal implementation: Core editing with line 
     numbers and scrolling (no code completion,
     diagnostics, or go-to-declaration)
     - LDAP syntax highlighting: Colorize LDAP filter 
     operators, attributes, values, and wildcards
     - Standalone: No external dependencies beyond 
     existing Jetpack Compose

     Files to Create

     1. gui/src/main/kotlin/com/moribito/gui/ui/component
     s/editor/LdapToken.kt

     Purpose: Define token types for LDAP syntax parsing

     Contents:
     - LdapTokenType sealed class with types: 
     LogicalOperator, Parenthesis, Attribute,
     ComparisonOperator, Value, Wildcard, Whitespace,
     Invalid
     - LdapToken data class (type, text, startIndex, 
     endIndex)
     - getColor() function mapping tokens to Gruvbox 
     theme colors

     Color Mappings:
     - LogicalOperator (&, |, !) → AppColors.orange 
     (#FE8019)
     - Parenthesis ((, )) → AppColors.neutral110 
     (#D5C4A1)
     - Attribute (e.g., cn, objectClass) → 
     AppColors.blue100 (#83A598)
     - ComparisonOperator (=, >=, <=, ~=) → 
     AppColors.purple100 (#D3869B)
     - Value (e.g., admin, person) → AppColors.green100 
     (#B8BB26)
     - Wildcard (*) → AppColors.yellow120 (#FABD2F)
     - Invalid syntax → AppColors.error (#FB4934)

     2. gui/src/main/kotlin/com/moribito/gui/ui/component
     s/editor/LdapSyntaxHighlighter.kt

     Purpose: Parse LDAP filters and apply syntax 
     highlighting

     Key Functions:
     - parseLdapFilter(text: String): List<LdapToken> - 
     Character-by-character state machine lexer
     - buildHighlightedText(text: String): 
     AnnotatedString - Convert tokens to colored 
     AnnotatedString

     Parsing States:
     enum class ParseState {
         NEUTRAL,       // Looking for next token
         IN_ATTRIBUTE,  // Reading attribute name (before
      =)
         IN_COMPARISON, // Reading comparison operator 
     (=, >=, etc.)
         IN_VALUE,      // Reading value (after =)
         AFTER_ESCAPE   // Just saw backslash
     }

     Parsing Logic:
     1. Start in NEUTRAL state
     2. On ( or ) → emit Parenthesis token
     3. On &, |, ! after ( → emit LogicalOperator token
     4. On letter/digit → enter IN_ATTRIBUTE state, 
     collect until operator
     5. On =, >=, etc. → emit Attribute, enter 
     IN_COMPARISON state
     6. After operator → enter IN_VALUE state, collect 
     until )
     7. In values, detect * as Wildcard
     8. Handle \ escape sequences

     3. gui/src/main/kotlin/com/moribito/gui/ui/component
     s/editor/CodeEditor.kt

     Purpose: Main editor component with line numbers and
      syntax highlighting

     Component Structure:
     @Composable
     fun CodeEditor(
         text: String,
         onTextChange: (String) -> Unit,
         modifier: Modifier = Modifier,
         enabled: Boolean = true,
         placeholder: String = ""
     )

     Layout:
     Row
     ├── Column (line numbers)
     │   └── Text("1"), Text("2"), ... (verticalScroll 
     disabled)
     └── Box (editor)
         └── BasicTextField (verticalScroll enabled)
             └── AnnotatedString with syntax colors

     State Management:
     - val scrollState = rememberScrollState() - Shared 
     by both columns
     - var textFieldValue by remember { 
     mutableStateOf(TextFieldValue(text)) } - Internal 
     editor state
     - LaunchedEffect(text) - Sync external text changes 
     to internal state
     - val highlightedText by 
     remember(textFieldValue.text) { derivedStateOf { ...
      } } - Compute highlighting

     Styling:
     - Editor text: 16.sp line height monospace (matching
      current QueryPanel)
     - Line numbers: AppMonospace.xsmall (10sp) with 
     16.sp line height to align
     - Line number color: 
     JewelTheme.globalColors.text.info.copy(alpha = 0.6f)
     - Cursor: SolidColor(AppColors.blue100)
     - Background: 
     JewelTheme.globalColors.panelBackground

     Scroll Synchronization:
     - Line numbers: Modifier.verticalScroll(scrollState,
      enabled = false) - follows scroll but doesn't 
     handle input
     - Editor: Modifier.verticalScroll(scrollState, 
     enabled = true) - user controls scrolling
     - Both stay perfectly synchronized through shared 
     state

     File to Modify

     4. gui/src/main/kotlin/com/moribito/gui/ui/component
     s/query/QueryPanel.kt

     Changes:
     - Remove Jewel TextFieldState and sync logic (lines 
     38-56)
     - Remove manual line number rendering (lines 77-101)
     - Replace TextArea component (lines 105-130) with 
     CodeEditor
     - Keep Island wrapper and button row unchanged

     Before:
     val textState = rememberTextFieldState(queryText)
     // Complex LaunchedEffect sync logic...

     Row {
         Column { /* Manual line numbers */ }
         Box { TextArea(state = textState, ...) }
     }

     After:
     CodeEditor(
         text = queryText,
         onTextChange = onQueryChange,
         enabled = isConnected,
         placeholder = "Enter LDAP filter (e.g., 
     (objectClass=*))",
         modifier = Modifier.fillMaxWidth().weight(1f)
     )

     Backward Compatibility: QueryPanel function 
     signature remains identical - this is a pure
     internal implementation change.

     Implementation Steps

     Phase 1: Token System

     1. Create editor/ directory under 
     gui/src/main/kotlin/com/moribito/gui/ui/components/
     2. Create LdapToken.kt with token types and color 
     mapping
     3. Verify compilation and color references

     Phase 2: Syntax Highlighting

     4. Create LdapSyntaxHighlighter.kt
     5. Implement parseLdapFilter() lexer with state 
     machine
     6. Test lexer with sample filters: (cn=admin), 
     (&(objectClass=person)(cn=*)), 
     (|(uid>=1000)(gid=100))
     7. Implement buildHighlightedText() to create 
     AnnotatedString
     8. Verify syntax highlighting in isolation

     Phase 3: Editor Component

     9. Create CodeEditor.kt with basic structure
     10. Implement line number column (synchronized 
     scroll)
     11. Implement BasicTextField with AnnotatedString 
     support
     12. Add text state synchronization (external ↔ 
     internal)
     13. Integrate syntax highlighting via derivedStateOf
     14. Test editor in isolation with sample LDAP 
     filters

     Phase 4: Integration

     15. Modify QueryPanel.kt to use CodeEditor
     16. Remove old TextFieldState and sync logic
     17. Test full integration in WorkspaceScreen
     18. Verify Format and Run buttons still work
     19. Verify state persistence across view changes

     Phase 5: Polish

     20. Fine-tune line number alignment (ensure exact 
     16.sp line height match)
     21. Test with edge cases (empty text, very long 
     filters, malformed syntax)
     22. Performance check: ensure highlighting is fast 
     (add 10k char limit if needed)

     Technical Details

     Scroll Synchronization Pattern

     val scrollState = rememberScrollState()

     Row {
         // Line numbers - scroll follows
         Column(modifier = 
     Modifier.verticalScroll(scrollState, enabled =
     false)) {
             repeat(lineCount) { Text("${it + 1}") }
         }

         // Editor - user controls scroll
         BasicTextField(modifier = 
     Modifier.verticalScroll(scrollState, enabled =
     true))
     }

     Text State Synchronization Pattern

     var textFieldValue by remember { 
     mutableStateOf(TextFieldValue(text)) }

     // External → Internal
     LaunchedEffect(text) {
         if (textFieldValue.text != text) {
             textFieldValue = TextFieldValue(text, 
     selection = textFieldValue.selection)
         }
     }

     // Internal → External
     BasicTextField(
         value = textFieldValue,
         onValueChange = { newValue ->
             textFieldValue = newValue
             if (newValue.text != text) 
     onTextChange(newValue.text)
         }
     )

     Syntax Highlighting Pattern

     val highlightedText by remember(textFieldValue.text)
      {
         derivedStateOf {
             val tokens = 
     parseLdapFilter(textFieldValue.text)
             buildAnnotatedString {
                 append(textFieldValue.text)
                 tokens.forEach { token ->
                     addStyle(
                         SpanStyle(color = 
     token.type.getColor()),
                         start = token.startIndex,
                         end = token.endIndex
                     )
                 }
             }
         }
     }

     BasicTextField(
         value = textFieldValue.copy(annotatedString = 
     highlightedText),
         // ...
     )

     Testing Plan

     Unit Tests

     - Token parsing: (cn=admin) → [Parenthesis, 
     Attribute, ComparisonOp, Value, Parenthesis]
     - Logical operators: (&(cn=a)(uid=b)) → includes 
     LogicalOperator token
     - Wildcards: (cn=admin*) → includes Wildcard token
     - Comparison operators: (uid>=1000) → ComparisonOp 
     token is >=

     Integration Tests

     - Text input/output through CodeEditor
     - Line number count accuracy
     - Scroll synchronization
     - External text updates (Format button)
     - Cursor preservation

     Manual Testing

     - Type and edit LDAP filters
     - Line numbers display and align correctly
     - Scrolling is smooth and synchronized
     - Syntax highlighting applies correct colors
     - Format button updates text with highlighting 
     intact
     - Run button executes query
     - Placeholder text shows/hides
     - Copy/paste works
     - State persists across view changes

     Risk Mitigation

     Risk: Line number misalignment
     - Mitigation: Use exact same line height (16.sp) for
      both columns
     - Test: Visual inspection at various scroll 
     positions

     Risk: Performance on large filters
     - Mitigation: Add text length check, skip 
     highlighting beyond 10k characters
     - Implementation: if (text.length > 10000) 
     AnnotatedString(text) else
     buildHighlightedText(text)

     Risk: Text sync issues with ViewModel
     - Mitigation: Use proven LaunchedEffect pattern from
      existing TextField
     - Test: Verify Format button (external update) works
      correctly

     Risk: Cursor jumping during external updates
     - Mitigation: Preserve selection when updating 
     TextFieldValue from external source
     - Implementation: TextFieldValue(text, selection = 
     textFieldValue.selection)

     Success Criteria

     1. ✅ Code editor displays with line numbers
     2. ✅ LDAP syntax is highlighted with Gruvbox colors
     3. ✅ Scrolling is smooth and synchronized
     4. ✅ Text editing works (cursor, selection, 
     copy/paste)
     5. ✅ Format and Run buttons function correctly
     6. ✅ No breaking changes to QueryPanel API
     7. ✅ Performance is acceptable (no lag when typing)

     Out of Scope (Future Enhancements)

     - Code completion / auto-suggest
     - Syntax error indicators (red squiggles)
     - Bracket matching
     - Search/replace
     - Code folding
     - Multiple cursors
     - Minimap
     - Diagnostics/tooltips
