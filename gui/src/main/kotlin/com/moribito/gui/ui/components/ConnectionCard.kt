package com.moribito.gui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.*

/**
 * A card component that displays a connection's name and host.
 * Features hover effects and selection state for a polished IntelliJ-style appearance.
 *
 * @param name The connection name to display
 * @param host The host address to display
 * @param isSelected Whether this connection is currently selected
 * @param onClick Callback when the card is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun ConnectionCard(
    name: String,
    host: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Determine background color based on state
    val backgroundColor = when {
        isHovered -> AppColors.surface.hover()
        else -> AppColors.surface
    }

    // Determine border color
    val borderColor = if (isSelected) AppColors.primary else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Connection name
            Text(
                text = name,
                style = AppTypography.titleMedium,
                color = AppColors.textPrimary
            )

            // Host address
            Text(
                text = host,
                style = AppMonospace.small,
                color = AppColors.textSecondary
            )
        }

        // Selection indicator (subtle border overlay)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(1.dp)
                    .background(Color.Transparent)
                    .let {
                        // Add border using padding and background trick
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    }
            )
        }
    }
}
