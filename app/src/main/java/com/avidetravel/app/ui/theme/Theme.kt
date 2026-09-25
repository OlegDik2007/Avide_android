package com.avidetravel.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AvideBlue = Color(0xFF0B4F8A)
private val AvideSky = Color(0xFF1C83C6)
private val AvideGold = Color(0xFFE5A33D)
private val AvideInk = Color(0xFF12253A)
private val AvideBackground = Color(0xFFF6F8FB)

private val LightColors = lightColorScheme(
    primary = AvideBlue,
    secondary = AvideSky,
    tertiary = AvideGold,
    background = AvideBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = AvideInk,
    onSurface = AvideInk
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF79B9ED),
    secondary = Color(0xFF8DC6F1),
    tertiary = Color(0xFFFFC978)
)

@Composable
fun AvideTravelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
