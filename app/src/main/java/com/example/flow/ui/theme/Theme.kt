package com.example.flow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlowDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8EAED),
    onPrimary = Color(0xFF101114),
    secondary = Color(0xFFB8BEC8),
    onSecondary = Color(0xFF111317),
    tertiary = Color(0xFF9FA7B3),
    background = Color.Black,
    onBackground = Color(0xFFF2F3F5),
    surface = Color(0xFF0B0C0E),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF17191D),
    onSurfaceVariant = Color(0xFFB5BAC3),
    outline = Color(0xFF3A3E46)
)

@Composable
fun FlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlowDarkColorScheme,
        typography = Typography,
        content = content
    )
}
