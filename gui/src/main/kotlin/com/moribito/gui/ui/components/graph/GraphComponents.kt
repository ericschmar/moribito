package com.moribito.gui.ui.components.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.theme.IntelliJColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Background with equally spaced dim dots.
 */
@Composable
fun DotBackground(
    modifier: Modifier = Modifier,
    offset: Offset = Offset.Zero,
    scale: Float = 1f,
    dotColor: Color = Color(0xFF4C5052), // Lighter, more visible dots
    dotSpacing: Dp = 10.dp
) {
    val backgroundColor = IntelliJColors.islandBackground
    
    Canvas(modifier = modifier.fillMaxSize().clipToBounds().background(backgroundColor)) {
        val spacingPx = dotSpacing.toPx()
        val dotRadius = 1.0.dp.toPx()
        
        // Use a fixed spacing for dots, but offset them based on panned position
        val offsetX = (offset.x * scale) % (spacingPx * scale)
        val offsetY = (offset.y * scale) % (spacingPx * scale)
        
        val scaledSpacing = spacingPx * scale
        
        val rows = (size.height / scaledSpacing).toInt() + 2
        val cols = (size.width / scaledSpacing).toInt() + 2
        
        for (r in -1..rows) {
            for (c in -1..cols) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius * scale.coerceAtLeast(0.5f),
                    center = Offset(offsetX + c * scaledSpacing, offsetY + r * scaledSpacing)
                )
            }
        }
    }
}

/**
 * A node in the graph.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun GraphNode(
    name: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    isLoaded: Boolean = true,
    canHaveChildren: Boolean = true,
    onHover: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    var hovered by remember { mutableStateOf(false) }
    val borderColor = if (hovered || isHighlighted) {
        Color(0xFF4A90E2) // IntelliJ blue highlight
    } else {
        JewelTheme.globalColors.borders.normal
    }
    
    val backgroundColor = if (hovered || isHighlighted) {
        IntelliJColors.islandBackground.copy(alpha = 0.8f)
    } else {
        IntelliJColors.islandBackground
    }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { 
                hovered = true
                onHover(true)
            }
            .onPointerEvent(PointerEventType.Exit) { 
                hovered = false
                onHover(false)
            }
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = name,
                maxLines = 1
            )
            
            if (!isLoaded && canHaveChildren) {
                Icon(
                    key = AllIconsKeys.General.ArrowRight,
                    contentDescription = "Load children",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * An edge in the graph.
 */
@Composable
fun GraphEdge(
    start: Offset,
    end: Offset,
    color: Color = Color(0xFF666666) // Brighter, more prominent edge color
) {
    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 3.dp.toPx()
        )
    }
}
