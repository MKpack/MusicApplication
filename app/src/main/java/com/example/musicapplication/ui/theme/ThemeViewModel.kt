package com.example.musicapplication.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.local.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeStore: ThemeStore
) : ViewModel() {

    val themePreset = themeStore.themePresetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MusicThemePreset.Green
        )

    fun saveThemePreset(themePreset: MusicThemePreset) {
        viewModelScope.launch {
            themeStore.saveThemePreset(themePreset)
        }
    }
}