package com.moribito.gui.ui.components.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import com.moribito.ldap.Entry
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer

@Composable
fun RecordTable(
    entry: Entry?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Table content
        if (entry != null) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSizes.inputHeightStandard)
                    .background(Color(0xFF1F2124)) // Slightly lighter than island background
                    .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attribute",
                    style = AppTypography.labelMedium,
                    color = AppColors.neutral140,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = "Value",
                    style = AppTypography.labelMedium,
                    color = AppColors.neutral140,
                    modifier = Modifier.weight(0.7f)
                )
            }
            VerticallyScrollableContainer(
                modifier = Modifier.fillMaxSize()
            ) {
                Column {
                    entry.attributes.entries
                        .sortedBy { it.key.lowercase() }
                        .forEachIndexed { index, (key, values) ->
                            AttributeRow(
                                key = key,
                                values = values,
                                isEven = index % 2 == 0
                            )
                        }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select a node to view details",
                    style = AppTypography.bodyMedium,
                    color = AppColors.neutral60
                )
            }
        }
    }
}
