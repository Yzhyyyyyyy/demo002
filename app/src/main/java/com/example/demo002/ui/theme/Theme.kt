package com.example.demo002.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF1C1C1E),
    onPrimary        = Color.White,
    background       = Color(0xFFF8FAFC),
    onBackground     = Color(0xFF1C1C1E),
    surface          = Color.White,
    onSurface        = Color(0xFF1C1C1E),
    surfaceVariant   = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline          = Color(0xFFE2E8F0),
    secondary        = PurpleGrey40,
    tertiary         = Pink40
)

@Composable
fun Demo002Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}