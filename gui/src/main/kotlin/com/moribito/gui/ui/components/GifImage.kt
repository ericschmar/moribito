package com.moribito.gui.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.moribito.gui.generated.resources.Res
import kotlinx.coroutines.delay
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

@Composable
fun GifImage(
    resourcePath: String,
    modifier: Modifier = Modifier
) {
    val bytes by produceState<ByteArray?>(null, resourcePath) {
        value = Res.readBytes(resourcePath)
    }

    val codec = remember(bytes) {
        val currentBytes = bytes ?: return@remember null
        val data = Data.makeFromBytes(currentBytes)
        try {
            Codec.makeFromData(data)
        } finally {
            data.close()
        }
    }

    if (codec == null) return

    DisposableEffect(codec) {
        onDispose {
            codec.close()
        }
    }

    var currentFrame by remember(codec) { mutableStateOf(0) }
    val frameCount = codec.frameCount

    LaunchedEffect(codec) {
        while (frameCount > 1) {
            val info = codec.getFrameInfo(currentFrame)
            val duration = if (info.duration <= 0) 100 else info.duration
            delay(duration.toLong())
            currentFrame = (currentFrame + 1) % frameCount
        }
    }

    val bitmap = remember(codec, currentFrame) {
        val skiaBitmap = org.jetbrains.skia.Bitmap()
        skiaBitmap.allocPixels(codec.imageInfo)
        codec.readPixels(skiaBitmap, currentFrame)
        skiaBitmap.asComposeImageBitmap()
    }

    Image(
        painter = BitmapPainter(bitmap),
        contentDescription = null,
        modifier = modifier
    )
}
