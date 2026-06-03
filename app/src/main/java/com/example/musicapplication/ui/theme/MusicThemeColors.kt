package com.example.musicapplication.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


@Immutable  // 不可变对象
data class MusicThemeColors(
    val primary: Color,
    val accent: Color,
    val primarySoft: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val cardSoft: Color,
    val playerSurface: Color,

    val surface: Color = Color.White,
    val field: Color = Color.White,
    val textPrimary: Color = Color(0xFF111111),
    val textSecondary: Color = Color(0xFF66736D),
    val textHint: Color = Color(0xFFA0AAA5),
    val border: Color = Color(0xFFDCEBE3),
    val disabledContainer: Color = Color(0xFFCFE8D8),
    val disabledContent: Color = Color(0xFF6F8A79),
    val iconMuted: Color = Color(0xFF78857D),
    val divider: Color = Color(0xFFE1EDE6)
)

fun MusicThemePreset.toMusicThemeColors(): MusicThemeColors {
    return MusicThemeColors(
        primary = primary,
        accent = accent,
        primarySoft = primarySoft,
        bgTop = bgTop,
        bgBottom = bgBottom,
        cardSoft = cardSoft,
        playerSurface = playerSurface
    )
}

val LocalMusicThemeColors = staticCompositionLocalOf {
    MusicThemePreset.Green.toMusicThemeColors()
}