package com.moribito.gui.ui.components.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import compose.icons.AllIcons
import compose.icons.Octicons
import compose.icons.octicons.Check16
import compose.icons.octicons.FileDirectory16
import compose.icons.octicons.ThreeBars16
import compose.icons.octicons.X16
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Action bar for the tree view with a settings dropdown menu.
 * Provides options like toggling virtual member children display.
 */
@Composable
fun TreeViewActionBar(
    showVirtualMembers: Boolean,
    onToggleVirtualMembers: () -> Unit,
    isShowingQueryResults: Boolean = false,
    onShowDirectory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(
                color = IntelliJColors.islandBackground,
                shape = RoundedCornerShape(
                    topStart = AppSizes.borderRadiusExtraLarge,
                    topEnd = AppSizes.borderRadiusExtraLarge,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .padding(horizontal = AppSpacing.xs, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show Directory button (only visible when showing query results)
        if (isShowingQueryResults) {
            IconButton(
                onClick = onShowDirectory,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    key = AllIconsKeys.Diff.Remove,
                    contentDescription = "Cancel Search",
                    modifier = Modifier
                        .size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box {
            IconButton(
                onClick = onShowDirectory,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    key = AllIconsKeys.General.ChevronDown,
                    contentDescription = "Tree View Settings",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showMenu = !showMenu }
                        .padding(2.dp)
                )
            }

            if (showMenu) {
                Popup(
                    alignment = Alignment.TopEnd,
                    offset = IntOffset(0, 24),
                    onDismissRequest = { showMenu = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .background(
                                color = IntelliJColors.baseBackground,
                                shape = RoundedCornerShape(AppSizes.borderRadiusExtraLarge)
                            )
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleVirtualMembers() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showVirtualMembers) {
                                    Icon(
                                        imageVector = Octicons.Check16,
                                        contentDescription = "Checked",
                                        tint = JewelTheme.globalColors.text.normal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Coalesce Members",
                                color = JewelTheme.globalColors.text.normal
                            )
                        }
                    }
                }
            }
        }
    }
}
