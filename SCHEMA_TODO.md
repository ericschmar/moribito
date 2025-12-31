Let's design a feature for inspecting the LDAP schema. This will be composed of a couple of different things.

1. In the core module, we need all the backend code to pull the schema from the ldap server. If the ldap server doesn't support schema inspection then we should pull a list of all the OUs. Then add a function that will query an OU just for their attributes.
2. In the UI we automatically inspect the schema in the background. We will have a progress bar in the bottom status bar that says the current status of the inspection. For the case that the ldap server doesn't support schema inspection, we can add a warning indicator, and then allow the user to right-click to bring up a context menu on each OU to pull the individual attributes. 
3. A new tool bar on the right hand side of our main window layout (the split pane layouts). It should be the same width as our TitleBar and in one column, list out action buttons. For now, only one action button will be "Inspect Schema".
4. Attribute viewer--In our main layout of the app, we should add another panel to the right hand side. It should have an Island in it. Within that island, we will list the attributes for the ldap server, or a given OU. This panel should be able to be closed as well. We will display the attributes similarly to our record table, where we have teh attribute name and then the attribute value type. A toggle for sorting the list of attributes by name ascending or descending. 


Appendix:
**Action Icon Button** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/IconActionButton.kt

**Context Menu** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/ContextMenu.kt

**Progress Bar** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/LinearProgressBar.kt
