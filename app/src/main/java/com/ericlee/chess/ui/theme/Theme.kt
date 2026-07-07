package com.ericlee.chess.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrownPrimary = Color(0xFF8B4513)
private val BrownDark = Color(0xFF5C2D0A)
private val GoldAccent = Color(0xFFDAA520)
private val CreamBg = Color(0xFFFFF8DC)

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    secondary = BrownPrimary,
    background = Color(0xFF1A0A00),
    surface = Color(0xFF2D1A0A),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFFFF0D4),
    onSurface = Color(0xFFFFF0D4)
)

private val LightColorScheme = lightColorScheme(
    primary = BrownPrimary,
    secondary = GoldAccent,
    background = CreamBg,
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1A0A00),
    onSurface = Color(0xFF1A0A00)
)

@Composable
fun ChineseChessTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
