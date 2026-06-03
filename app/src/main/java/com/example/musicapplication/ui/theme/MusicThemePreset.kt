package com.example.musicapplication.ui.theme

import androidx.compose.ui.graphics.Color

enum class MusicThemePreset(
    val label: String,
    val primary: Color,
    val accent: Color,
    val primarySoft: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val cardSoft: Color,
    val playerSurface: Color
) {
    Green(
        label = "松石绿",
        primary = Color(0xFF14B8A6),
        accent = Color(0xFF22C55E),
        primarySoft = Color(0xFFD7F7F2),
        bgTop = Color(0xFFFAFFFD),
        bgBottom = Color(0xFFEFFFFA),
        cardSoft = Color(0xFFE6F8F4),
        playerSurface = Color(0xFFF7FFFC)
    ),

    Blue(
        label = "湖蓝",
        primary = Color(0xFF0EA5E9),
        accent = Color(0xFF38BDF8),
        primarySoft = Color(0xFFDFF5FF),
        bgTop = Color(0xFFFAFDFF),
        bgBottom = Color(0xFFEFF9FF),
        cardSoft = Color(0xFFE6F6FF),
        playerSurface = Color(0xFFF7FCFF)
    ),

    Red(
        label = "玫瑰",
        primary = Color(0xFFE11D48),
        accent = Color(0xFFF43F5E),
        primarySoft = Color(0xFFFFDEE7),
        bgTop = Color(0xFFFFFBFC),
        bgBottom = Color(0xFFFFF1F5),
        cardSoft = Color(0xFFFFE8EE),
        playerSurface = Color(0xFFFFF8FA)
    ),

    Yellow(
        label = "琥珀",
        primary = Color(0xFFD97706),
        accent = Color(0xFFF59E0B),
        primarySoft = Color(0xFFFFE8B5),
        bgTop = Color(0xFFFFFDF7),
        bgBottom = Color(0xFFFFF6E3),
        cardSoft = Color(0xFFFFF0D1),
        playerSurface = Color(0xFFFFFBF2)
    ),

    Purple(
        label = "紫罗兰",
        primary = Color(0xFF7C3AED),
        accent = Color(0xFFA78BFA),
        primarySoft = Color(0xFFEDE7FF),
        bgTop = Color(0xFFFCFAFF),
        bgBottom = Color(0xFFF5F0FF),
        cardSoft = Color(0xFFF0E9FF),
        playerSurface = Color(0xFFFBF8FF)
    ),

    Gray(
        label = "石墨",
        primary = Color(0xFF374151),
        accent = Color(0xFF64748B),
        primarySoft = Color(0xFFE2E8F0),
        bgTop = Color(0xFFFBFCFD),
        bgBottom = Color(0xFFF1F5F9),
        cardSoft = Color(0xFFECEFF3),
        playerSurface = Color(0xFFF8FAFC)
    )
}
