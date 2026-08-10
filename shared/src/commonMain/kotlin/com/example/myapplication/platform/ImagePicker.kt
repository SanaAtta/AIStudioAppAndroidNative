package com.example.myapplication.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(onPicked: (ByteArray?) -> Unit): () -> Unit
