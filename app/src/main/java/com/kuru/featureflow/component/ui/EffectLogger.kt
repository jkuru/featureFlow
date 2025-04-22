package com.kuru.featureflow.component.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

@Composable
fun LogEffect(tag: String, message: String = "Effect triggered") {
    LaunchedEffect(Unit) {
        Log.d(tag, message)
    }
}

@Composable
fun LogDisposableEffect(
    tag: String,
    message: String = "Effect triggered",
    onDispose: () -> Unit = {}
) {
    DisposableEffect(Unit) {
        Log.d(tag, message)
        onDispose {
            Log.d(tag, "Disposing $message")
            onDispose()
        }
    }
}

@Composable
fun LogEffect(
    tag: String,
    key: Any?,
    message: String = "Effect triggered"
) {
    LaunchedEffect(key) {
        Log.d(tag, message)
    }
} 