package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.theme.AppColors
import com.moribito.gui.theme.IntelliJColors
import com.moribito.gui.ui.components.editor.drawing.AnimatedGirl
import org.jetbrains.jewel.ui.component.Text

/**
 * A card component that displays a connection's name and host.
 * Features hover effects and selection state for a polished IntelliJ-style appearance.
 *
 * @param name The connection name to display
 * @param host The host address to display
 * @param isSelected Whether this connection is currently selected
 * @param onClick Callback when the card is clicked
 * @param enabled Whether the card is enabled for input
 * @param modifier Optional modifier for the card
 */
@Composable
fun ConnectionCard(
    name: String,
    host: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Determine background color based on state using IntelliJ colors
    val backgroundColor = when {
        isSelected && isHovered -> IntelliJColors.selectedBackground
        isSelected -> IntelliJColors.selectedBackground
        isHovered -> IntelliJColors.hoverBackground
        else -> IntelliJColors.baseBackground
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .border(
                width = 1.dp,
                color = AppColors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) backgroundColor else IntelliJColors.baseBackgroundDisabled)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Identicon based on connection name
                Identicon(
                    identifier = name,
                    size = 32.dp
                )

                // Connection details
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Connection name - larger, bold, primary color
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )

                    // Host address - smaller, muted secondary color, monospace
                    Text(
                        text = host,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.textSecondary
                    )
                }
            }

            if (loading) {
                AnimatedGirl(animate = loading, size = 32.dp)
            }
        }
    }
}
