package com.example.myapplication

import androidx.compose.runtime.Composable
import com.example.myapplication.theme.MyApplicationTheme

@Composable
fun App() {
    MyApplicationTheme {
        // Show main UI immediately — splash was blocking iOS from reaching Home.
        AppRoot()
    }
}
