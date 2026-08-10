package com.example.myapplication.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onPicked: (ByteArray?) -> Unit): () -> Unit {
    return {
        println("ImagePicker: not implemented on iOS yet")
        onPicked(null)
    }
}
