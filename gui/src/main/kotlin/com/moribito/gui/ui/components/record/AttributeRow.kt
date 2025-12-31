package com.moribito.gui.ui.components.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.AppMonospace
import com.moribito.gui.theme.AppSpacing
import com.moribito.gui.theme.AppTypography
import com.moribito.gui.theme.IntelliJColors
import org.jetbrains.jewel.ui.component.Text

/**
 * Single row in the attribute table with zebra striping.
 * Displays attribute key and values in a two-column layout.
 * Values are selectable but not editable (copy functionality).
 */
@Composable
fun AttributeRow(
    key: String,
    values: List<String>,
    isEven: Boolean,
    modifier: Modifier = Modifier
) {
    // Darker stripe uses island background, lighter stripe is slightly lighter
    val backgroundColor = if (isEven) {
        IntelliJColors.islandBackground
    } else {
        Color(0xFF1F2124) // Slightly lighter than island background
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = AppSpacing.xs, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        // Key column (30%)
        Text(
            text = key,
            style = AppTypography.bodyMedium,
            color = AppColors.neutral140,
            modifier = Modifier.weight(0.3f)
        )

        // Value column (70%)
        SelectionContainer(
            modifier = Modifier.weight(0.7f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.Start
            ) {
                values.forEach { value ->
                    Text(
                        text = value,
                        style = AppMonospace.small,
                        color = AppColors.neutral140
                    )
                }
            }
        }
    }
}
