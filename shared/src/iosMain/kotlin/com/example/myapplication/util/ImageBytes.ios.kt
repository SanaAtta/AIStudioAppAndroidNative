package com.example.myapplication.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.toImageBitmapOrNull(): ImageBitmap? = runCatching {
    Image.makeFromEncoded(this).toComposeImageBitmap()
}.getOrNull()
