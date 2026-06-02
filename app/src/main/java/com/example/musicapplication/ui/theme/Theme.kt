package com.example.musicapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MusicPrimary,
    secondary = MusicAccent,
    background = Color(0xFF0B0F0D),
    surface = Color(0xFF151A17),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF4F7F5),
    onSurface = Color(0xFFF4F7F5)
)

private val LightColorScheme = lightColorScheme(
    primary = MusicPrimary,
    secondary = MusicAccent,
    background = MusicBgTop,
    surface = MusicSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = MusicTextPrimary,
    onSurface = MusicTextPrimary,
)

@Composable
fun MusicApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // 改成false，原因：Android 12+ 的动态色会根据系统壁纸改颜色，会覆盖你自己的音乐 App 主题，调 UI 时会很乱。
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}