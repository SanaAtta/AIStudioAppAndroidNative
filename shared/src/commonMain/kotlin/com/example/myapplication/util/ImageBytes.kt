package com.example.myapplication.util

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.toImageBitmapOrNull(): ImageBitmap?
