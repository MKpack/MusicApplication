package com.example.musicapplication.data.local.theme

import com.example.musicapplication.ui.theme.MusicThemePreset
import kotlinx.coroutines.flow.Flow

interface ThemeStore {
    val themePresetFlow: Flow<MusicThemePreset>

    suspend fun saveThemePreset(preset: MusicThemePreset)

    /** 自定义页面背景图（本地文件的绝对路径），没有设置时是 null */
    val backgroundImageFlow: Flow<String?>

    suspend fun saveBackgroundImage(path: String?)
}
