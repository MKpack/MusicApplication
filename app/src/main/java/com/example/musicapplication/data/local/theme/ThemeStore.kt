package com.example.musicapplication.data.local.theme

import com.example.musicapplication.ui.theme.MusicThemePreset
import kotlinx.coroutines.flow.Flow

interface ThemeStore {
    val themePresetFlow: Flow<MusicThemePreset>

    suspend fun saveThemePreset(preset: MusicThemePreset)
}