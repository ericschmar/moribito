package com.moribito.gui.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * A visual identifier component that generates a unique 5x5 mirrored pattern
 * based on a string (e.g., a bind DN).
 */
@Composable
fun Identicon(
    identifier: String,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val hash = remember(identifier) { identifier.md5() }
    
    Canvas(modifier = modifier.size(size)) {
        val cellSize = size.toPx() / 5
        
        // Use first 3 bytes for color (HSL or RGB)
        // We'll use them for a nice base color
        val r = (hash[0].toInt() and 0xFF)
        val g = (hash[1].toInt() and 0xFF)
        val b = (hash[2].toInt() and 0xFF)
        
        // Ensure color is not too dark or too light for visibility
        val color = Color(
            red = (r % 200 + 55) / 255f,
            green = (g % 200 + 55) / 255f,
            blue = (b % 200 + 55) / 255f
        )
        
        // 5x5 grid, mirrored horizontally (cols 0,1,2 mirrored to 4,3)
        // We use bits starting from byte 3 of the hash
        for (x in 0 until 3) {
            for (y in 0 until 5) {
                val bitIndex = x * 5 + y
                val bytePos = 3 + bitIndex / 8
                val bitPos = bitIndex % 8
                
                val isFilled = if (bytePos < hash.size) {
                    (hash[bytePos].toInt() shr bitPos) and 1 == 1
                } else {
                    false
                }
                
                if (isFilled) {
                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                    
                    // Mirror to other side
                    if (x < 2) {
                        drawRect(
                            color = color,
                            topLeft = Offset((4 - x) * cellSize, y * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

private fun String.md5(): ByteArray {
    return MessageDigest.getInstance("MD5").digest(this.toByteArray("UTF-8"))
}

private fun String.toByteArray(charset: String): ByteArray {
    return this.toByteArray(java.nio.charset.Charset.forName(charset))
}
