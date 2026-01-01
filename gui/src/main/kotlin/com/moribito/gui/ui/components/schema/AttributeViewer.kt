package com.moribito.gui.ui.components.schema

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.icons.AppIcons
import com.moribito.ldap.LdapSchema
import compose.icons.Octicons
import compose.icons.octicons.ArrowDown16
import compose.icons.octicons.ArrowUp16
import compose.icons.octicons.X16
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Image
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttributeViewer(
    schema: LdapSchema?,
    sortAscending: Boolean,
    onToggleSort: () -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {

    var xOffset by remember { mutableStateOf(0.dp) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (true) {
                delay(250)
                // Walk from -60dp to 60dp in steps
                xOffset += 4.dp
                if (xOffset > 60.dp) {
                    xOffset = (-60).dp
                }
            }
        } else {
            xOffset = 0.dp
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Attributes",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = AppSpacing.xs)
                )
                if (schema?.isFromSchemaInspection == false) {
                    Text(
                        text = "(Discovery)",
                        fontSize = 11.sp,
                        color = JewelTheme.globalColors.text.disabled
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Tooltip(tooltip = { Text("Sort") }) {
                    IconButton(onClick = onToggleSort) {
                        Icon(
                            imageVector = if (sortAscending) Octicons.ArrowUp16 else Octicons.ArrowDown16,
                            contentDescription = "Sort",
                            tint = JewelTheme.globalColors.text.normal
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Tooltip(tooltip = { Text("Close Window") }) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Octicons.X16,
                            contentDescription = "Close",
                            tint = JewelTheme.globalColors.text.normal
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(JewelTheme.globalColors.borders.normal)
        )

        // Attribute list
        if (isLoading) {
            Image(
                iconKey = AppIcons.walkingIndicator,
                contentDescription = "Inspecting schema indicator",
                modifier = Modifier
                    .offset(x = xOffset)
                    .size(AppSizes.iconExtraLarge)
            )
        } else if (schema == null || schema.attributes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No attributes found", color = JewelTheme.globalColors.text.disabled)
            }
        } else {
            val sortedAttributes = if (sortAscending) {
                schema.attributes.sortedBy { it.name }
            } else {
                schema.attributes.sortedByDescending { it.name }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(sortedAttributes) { index, attr ->
                    AttributeRow(attr, isEven = index % 2 == 0)
                }
            }
        }
    }
}

@Composable
private fun AttributeRow(attribute: com.moribito.ldap.LdapAttribute, isEven: Boolean) {
    val backgroundColor = if (isEven) {
        IntelliJColors.islandBackground
    } else {
        Color(0xFF1F2124) // Slightly lighter than island background
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
    ) {
        SelectionContainer {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = attribute.name,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = attribute.type,
                        fontSize = 11.sp,
                        color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.disabled
                    )
                }
                attribute.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 10.sp,
                        color = org.jetbrains.jewel.foundation.theme.JewelTheme.globalColors.text.disabled,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
