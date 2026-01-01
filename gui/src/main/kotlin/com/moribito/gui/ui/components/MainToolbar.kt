package com.moribito.gui.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSpacing
import compose.icons.Octicons
import compose.icons.octicons.Search16
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainToolbar(
    onInspectSchema: () -> Unit,
    onOpenGraph: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Tooltip(tooltip = { Text("Inspect Schema") }) {
            IconButton(
                onClick = onInspectSchema,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Nodes.Annotationtype,
                    contentDescription = "Inspect Schema",
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Tooltip(tooltip = { Text("Directory Graph") }) {
            IconButton(
                onClick = onOpenGraph,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    key = AllIconsKeys.General.Tree,
                    contentDescription = "Directory Graph",
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
