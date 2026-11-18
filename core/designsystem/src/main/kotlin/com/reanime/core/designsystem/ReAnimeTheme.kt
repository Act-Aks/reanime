package com.reanime.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ReAnimeColors = darkColorScheme(
    primary = Color(0xFFB7F36B),
    onPrimary = Color(0xFF142000),
    background = Color(0xFF101114),
    surface = Color(0xFF191B20),
    onBackground = Color(0xFFE5E2E9),
    onSurface = Color(0xFFE5E2E9)
)

@Composable
fun ReAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReAnimeColors, content = content)
}
