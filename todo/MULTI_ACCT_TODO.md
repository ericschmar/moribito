We need to support multiple bind dn's for a given ldap connection. This will span our connection management, ldap client, and the menu bar.

1. Within the connection manager window: add support for configuring multiple bind dn's. This should look like a small table, zebra stiped (like our record table). At the bottom right hand side, two buttons (see the ConfigurationScreen ActionBar) with a + and a -. A + will add an editable row to the table. The table will have the bind dn and password. Double clicking on a row will allow editing. Pressing enter will disable editing. We will still ultimately press save or connect to persist the changes. 
2. Within the connection manager window: Our Connection list will be changed to a Lazy Tree. Each node will be the ldap connection. With the children being the bind dn's. Clicking on any bind dn (or server) will open the populate the configuration screen with the connection info. Double-clicking on a bind dn will connect to the server using that credential.
3. Within the connection manager window: if there are multiple bind dn's, and a user clicks "connect" will prompt them to select which bind dn they want to use.
4. Update our ldap client and config code in core to support multiple bind dn's.
5. In our TitleBar, create a dropdown menu with the connection names. Clicking on a connection name will disconnect the current session and attempt to connect using that bind dn credential. A final row in the drop down menu will be a "add dn" option, which, when clicked will open a new window to the configuration screen.
6. The dropdown menu, when not opened, should display an identicon, the current bind dn, and an icon pointing downwards.
7. An identicon generator will be needed. Follow the appendix examples to create one.

Appendix:
**Dropdown Menu** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/Dropdown.kt

**Identicons EXAMPLE (React)** https://github.com/doke-v/react-identicons/blob/master/src/index.js

**Identicons EXAMPLE (Kotlin)** https://github.com/WycliffeAssociates/jdenticon-kotlin/tree/master/src/commonMain/kotlin/jdenticon

**Lazy Tree** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/LazyTree.kt