package com.example.musicapplication.data.local.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicapplication.ui.theme.MusicThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject


private val Context.themeDataStore by preferencesDataStore(
    name = "theme_prefs"
)


class DataStoreThemeStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ThemeStore {
    private val themePresetKey = stringPreferencesKey("theme_preset")

    override val themePresetFlow = context.themeDataStore.data
        .catch {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }
        .map { prefs ->
            val name = prefs[themePresetKey] ?: MusicThemePreset.Green.name

            runCatching {
                MusicThemePreset.valueOf(name)
            }.getOrDefault(MusicThemePreset.Green)
        }

    override suspend fun saveThemePreset(preset: com.example.musicapplication.ui.theme.MusicThemePreset) {
        context.themeDataStore.edit { prefs ->
            prefs[themePresetKey] = preset.name
        }
    }
}