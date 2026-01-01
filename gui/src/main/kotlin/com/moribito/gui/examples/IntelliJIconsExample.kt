package com.moribito.gui.examples

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Example component demonstrating how to use IntelliJ Platform icons with Jewel.
 *
 * This component showcases various IntelliJ icon categories and how to integrate
 * them into your Compose UI.
 */
@Composable
fun IntelliJIconsExample() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("IntelliJ Platform Icons Example")

        // Actions icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Actions:")
            Icon(
                key = AllIconsKeys.Actions.Compile,
                contentDescription = "Compile"
            )
            Icon(
                key = AllIconsKeys.Actions.Execute,
                contentDescription = "Execute"
            )
            Icon(
                key = AllIconsKeys.Actions.Suspend,
                contentDescription = "Suspend"
            )
            Icon(
                key = AllIconsKeys.Actions.Refresh,
                contentDescription = "Refresh"
            )
        }

        // General icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("General:")
            Icon(
                key = AllIconsKeys.General.Settings,
                contentDescription = "Settings"
            )
            Icon(
                key = AllIconsKeys.General.Add,
                contentDescription = "Add"
            )
            Icon(
                key = AllIconsKeys.General.Remove,
                contentDescription = "Remove"
            )
            Icon(
                key = AllIconsKeys.General.Filter,
                contentDescription = "Filter"
            )
        }

        // Node icons (for tree views)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Nodes:")
            Icon(
                key = AllIconsKeys.Nodes.Folder,
                contentDescription = "Folder"
            )
            Icon(
                key = AllIconsKeys.Nodes.Module,
                contentDescription = "Module"
            )
            Icon(
                key = AllIconsKeys.Nodes.Class,
                contentDescription = "Class"
            )
            Icon(
                key = AllIconsKeys.Nodes.Method,
                contentDescription = "Method"
            )
        }

        // File type icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("File Types:")
            Icon(
                key = AllIconsKeys.FileTypes.Java,
                contentDescription = "Java"
            )
            Icon(
                key = AllIconsKeys.FileTypes.Xml,
                contentDescription = "XML"
            )
            Icon(
                key = AllIconsKeys.FileTypes.Json,
                contentDescription = "JSON"
            )
            Icon(
                key = AllIconsKeys.FileTypes.Text,
                contentDescription = "Text"
            )
        }

        // VCS icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("VCS:")
            Icon(
                key = AllIconsKeys.Vcs.Branch,
                contentDescription = "Branch"
            )
            Icon(
                key = AllIconsKeys.Vcs.Push,
                contentDescription = "Push"
            )
        }
    }
}
