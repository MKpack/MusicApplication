package com.example.musicapplication.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.local.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeStore: ThemeStore
) : ViewModel() {

    // 主题第一次从 DataStore 读出来之前是 false：系统启动页会一直显示到读完，避免先用默认主题色画一帧再变色
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    val themePreset = themeStore.themePresetFlow
        .onEach { _isLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MusicThemePreset.Red
        )

    fun saveThemePreset(themePreset: MusicThemePreset) {
        viewModelScope.launch {
            themeStore.saveThemePreset(themePreset)
        }
    }
}