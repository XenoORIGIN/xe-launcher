package com.xenoorigin.xelauncher.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.xenoorigin.xelauncher.di.AppGraph

/** Placeholder; replaced by the full settings navigation in Task 12. */
@Composable
fun SettingsHost(graph: AppGraph, start: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Text(start, color = Color.White) }
}
