package com.larkplaye.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.larkplaye.player.core.AppPreferences

data class LarkPalette(
    val accent: Color,
    val accentSoft: Color,
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textSoft: Color,
    val isDark: Boolean,
    val heroStart: Color,
    val heroEnd: Color,
)

val LocalLarkPalette = staticCompositionLocalOf {
    LarkPalette(
        accent = Color(0xFFFFD54A),
        accentSoft = Color(0x33FFD54A),
        background = Color(0xFF0E0F12),
        backgroundAlt = Color(0xFF15171D),
        surface = Color(0xFF181B22),
        surfaceHigh = Color(0xFF23262F),
        text = Color(0xFFF5F6F8),
        textSoft = Color(0xFFA7ACB8),
        isDark = true,
        heroStart = Color(0xFF2A2410),
        heroEnd = Color(0xFF0E0F12),
    )
}

private data class ThemeAccent(val accent: Color, val start: Color, val end: Color)

private fun accentFor(prefs: AppPreferences): ThemeAccent = when (prefs.themeId) {
    "sunset" -> ThemeAccent(Color(0xFFFF9A62), Color(0xFF3A1E14), Color(0xFF160F0D))
    "ocean" -> ThemeAccent(Color(0xFF5DC2FF), Color(0xFF102A38), Color(0xFF0C1118))
    "forest" -> ThemeAccent(Color(0xFF55DDA1), Color(0xFF102C22), Color(0xFF0C1512))
    "candy" -> ThemeAccent(Color(0xFFFF86BE), Color(0xFF381525), Color(0xFF170D12))
    "mono" -> ThemeAccent(Color(0xFFE4E4E4), Color(0xFF2A2A2A), Color(0xFF101010))
    else -> ThemeAccent(Color(prefs.accentColor), Color(0xFF322A10), Color(0xFF0E0F12))
}

@Composable
fun LarkTheme(prefs: AppPreferences, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.darkMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val themeAccent = accentFor(prefs)
    val accent = Color(prefs.accentColor)
    val background = if (dark) Color(0xFF0E0F12) else Color(0xFFF7F5F0)
    val backgroundAlt = if (dark) Color(0xFF15171D) else Color(0xFFEDEAE3)
    val surface = if (dark) Color(0xFF181B22) else Color(0xFFFFFFFF)
    val surfaceHigh = if (dark) Color(0xFF23262F) else Color(0xFFE8E4DC)
    val text = if (dark) Color(0xFFF5F6F8) else Color(0xFF14151A)
    val textSoft = if (dark) Color(0xFFA7ACB8) else Color(0xFF64676F)

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF17130A),
            primaryContainer = accent.copy(alpha = 0.18f),
            onPrimaryContainer = accent,
            secondary = accent,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSoft,
            outline = Color(0xFF3A3E48),
            error = Color(0xFFFF6B6B),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color(0xFF17130A),
            primaryContainer = accent.copy(alpha = 0.24f),
            onPrimaryContainer = Color(0xFF3D3008),
            secondary = accent,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSoft,
            outline = Color(0xFFC9C4BA),
            error = Color(0xFFC62F2F),
        )
    }

    val palette = LarkPalette(
        accent = accent,
        accentSoft = accent.copy(alpha = 0.16f),
        background = background,
        backgroundAlt = backgroundAlt,
        surface = surface,
        surfaceHigh = surfaceHigh,
        text = text,
        textSoft = textSoft,
        isDark = dark,
        heroStart = if (dark) themeAccent.start else accent.copy(alpha = 0.28f).compositeOver(surface),
        heroEnd = if (dark) themeAccent.end else background,
    )

    CompositionLocalProvider(LocalLarkPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

fun Color.compositeOver(background: Color): Color = Color(
    red = red * alpha + background.red * (1 - alpha),
    green = green * alpha + background.green * (1 - alpha),
    blue = blue * alpha + background.blue * (1 - alpha),
    alpha = 1f,
)
