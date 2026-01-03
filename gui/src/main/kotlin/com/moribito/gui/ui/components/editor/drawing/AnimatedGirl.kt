package com.moribito.gui.ui.components.editor.drawing

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moribito.gui.theme.AppSizes
import com.moribito.gui.ui.components.GifImage
import com.moribito.gui.ui.icons.AppIcons
import kotlinx.coroutines.delay

@Composable
fun AnimatedGirl(
    animate: Boolean,
    size: Dp = AppSizes.iconExtraLarge
) {
    var xOffset by remember { mutableStateOf(0.dp) }

    LaunchedEffect(animate) {
        if (animate) {
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

    GifImage(
        resourcePath = AppIcons.walkingIndicatorPath,
        modifier = Modifier.size(size).offset(x = xOffset)
    )
}