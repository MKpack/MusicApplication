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
    val divider: Color = Color(0xFFE1EDE6),

    // iOS 风格的中性色，跟随深色模式变化。改版后的新页面（欢迎页、登录注册）用这一组，旧页面继续用上面的字段
    val neutral: NeutralColors = NeutralColors.Light
) {
    // 主题色的浅底：符号圆、浅色按钮的背景
    val primaryTint: Color
        get() = primary.copy(alpha = if (neutral.isDark) 0.2f else 0.12f)
}

@Immutable
data class NeutralColors(
    val isDark: Boolean,
    val groupedBackground: Color,   // 分组页面的底色
    val card: Color,                // 分组卡片
    val label: Color,               // 主要文字
    val labelSecondary: Color,      // 次要文字、说明
    val labelTertiary: Color,       // 占位、箭头、未激活的横线
    val separator: Color,           // 分隔线
    val fill: Color,                // 进度条底、未选中的步骤
    val success: Color,             // 校验通过的 ✓
    val warning: Color,             // 密码强度「中」
    val danger: Color,              // 错误提示、密码强度「弱」
    val glass: Color,               // 液态玻璃的底色
    val glassStrong: Color          // 大面积玻璃面板（欢迎页）的底色，更不透明，保证文字清楚
) {
    companion object {
        val Light = NeutralColors(
            isDark = false,
            groupedBackground = Color(0xFFF2F2F7),
            card = Color.White,
            label = Color.Black,
            labelSecondary = Color(0x9E3C3C43),
            labelTertiary = Color(0x4D3C3C43),
            separator = Color(0x2E3C3C43),
            fill = Color(0x29787880),
            success = Color(0xFF34C759),
            warning = Color(0xFFFF9F0A),
            danger = Color(0xFFFF3B30),
            glass = Color(0x9EFFFFFF),
            glassStrong = Color(0xBDFFFFFF)
        )

        val Dark = NeutralColors(
            isDark = true,
            groupedBackground = Color.Black,
            card = Color(0xFF1C1C1E),
            label = Color.White,
            labelSecondary = Color(0x9EEBEBF5),
            labelTertiary = Color(0x4DEBEBF5),
            separator = Color(0x99545458),
            fill = Color(0x47767680),
            success = Color(0xFF30D158),
            warning = Color(0xFFFF9F0A),
            danger = Color(0xFFFF453A),
            glass = Color(0x8034343A),
            glassStrong = Color(0xA61C1C1E)
        )
    }
}

fun MusicThemePreset.toMusicThemeColors(darkTheme: Boolean = false): MusicThemeColors {
    return MusicThemeColors(
        primary = primary,
        accent = accent,
        primarySoft = primarySoft,
        bgTop = bgTop,
        bgBottom = bgBottom,
        cardSoft = cardSoft,
        playerSurface = playerSurface,
        // 不可用按钮的颜色跟随主题色，原来固定的浅绿色只适合松石绿主题
        disabledContainer = primarySoft,
        disabledContent = primary.copy(alpha = 0.55f),
        neutral = if (darkTheme) NeutralColors.Dark else NeutralColors.Light
    )
}

val LocalMusicThemeColors = staticCompositionLocalOf {
    MusicThemePreset.Red.toMusicThemeColors()
}
