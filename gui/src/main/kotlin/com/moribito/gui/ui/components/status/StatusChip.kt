package com.moribito.gui.ui.components.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moribito.gui.viewmodel.ConnectionState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Status chip component displaying connection state with colored indicator.
 * Compact design with subtle rounded corners.
 */
@Composable
fun StatusChip(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    // Using semantic IntelliJ colors
    val successColor = Color(0xFF6AAB73) // IntelliJ green
    val warningColor = Color(0xFFCDA869) // IntelliJ yellow/orange
    val errorColor = Color(0xFFCC666E) // IntelliJ red
    val neutralColor = JewelTheme.globalColors.borders.normal

    val (dotColor, text) = when (state) {
        is ConnectionState.Connected -> Pair(successColor, "Connected")
        is ConnectionState.Connecting -> Pair(warningColor, "Connecting")
        is ConnectionState.Disconnected -> Pair(neutralColor, "Disconnected")
        is ConnectionState.Error -> Pair(errorColor, "Error")
    }

    Row(
        modifier = modifier
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Status indicator dot (5dp diameter)
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(dotColor, CircleShape)
        )

        // Status text
        Text(
            text = text,
            fontSize = 11.sp
        )
    }
}
