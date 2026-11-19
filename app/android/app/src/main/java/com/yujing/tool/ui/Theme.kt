package com.yujing.tool.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YujingColors = lightColorScheme(
    primary = Color(0xFF1F5C63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8E3E7),
    onPrimaryContainer = Color(0xFF0A2F33),
    secondary = Color(0xFF4C6358),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F4EF),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFEEEDE7),
    onSurfaceVariant = Color(0xFF5C5F5B),
    error = Color(0xFFB3261E),
)

@Composable
fun YujingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YujingColors,
        content = content,
    )
}
