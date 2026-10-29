package com.example.musicapplication.ui.theme

import androidx.compose.ui.graphics.Color

enum class MusicThemePreset(
    val label: String,
    val primary: Color,
    val accent: Color,
    val primarySoft: Color,
    val cardSoft: Color,
    val playerSurface: Color
) {
    // 樱桃红：默认主题。沿用原来「玫瑰」的枚举名 Red，之前选过玫瑰的用户会自动变成樱桃红
    Red(
        label = "樱桃红",
        primary = Color(0xFFFF2D55),
        accent = Color(0xFFFF6A3D),
        primarySoft = Color(0xFFFFE3E9),
        cardSoft = Color(0xFFFFE8EE),
        playerSurface = Color(0xFFFFF8FA)
    ),

    Green(
        label = "松石绿",
        primary = Color(0xFF14B8A6),
        accent = Color(0xFF22C55E),
        primarySoft = Color(0xFFD7F7F2),
        cardSoft = Color(0xFFE6F8F4),
        playerSurface = Color(0xFFF7FFFC)
    ),

    Blue(
        label = "湖蓝",
        primary = Color(0xFF0EA5E9),
        accent = Color(0xFF38BDF8),
        primarySoft = Color(0xFFDFF5FF),
        cardSoft = Color(0xFFE6F6FF),
        playerSurface = Color(0xFFF7FCFF)
    ),

    Yellow(
        label = "琥珀",
        primary = Color(0xFFD97706),
        accent = Color(0xFFF59E0B),
        primarySoft = Color(0xFFFFE8B5),
        cardSoft = Color(0xFFFFF0D1),
        playerSurface = Color(0xFFFFFBF2)
    ),

    Purple(
        label = "紫罗兰",
        primary = Color(0xFF7C3AED),
        accent = Color(0xFFA78BFA),
        primarySoft = Color(0xFFEDE7FF),
        cardSoft = Color(0xFFF0E9FF),
        playerSurface = Color(0xFFFBF8FF)
    ),

    Gray(
        label = "石墨",
        primary = Color(0xFF374151),
        accent = Color(0xFF64748B),
        primarySoft = Color(0xFFE2E8F0),
        cardSoft = Color(0xFFECEFF3),
        playerSurface = Color(0xFFF8FAFC)
    )
}
