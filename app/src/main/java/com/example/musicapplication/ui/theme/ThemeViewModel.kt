package com.example.musicapplication.ui.theme

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.local.theme.BackgroundImageFiles
import com.example.musicapplication.data.local.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeStore: ThemeStore,
    private val backgroundImageFiles: BackgroundImageFiles,
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

    /** 自定义页面背景图的本地路径，用默认背景（纯白 / 纯黑）时是 null */
    val backgroundImage: StateFlow<String?> = themeStore.backgroundImageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // 换背景的结果，需要弹 Toast
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _isSavingBackground = MutableStateFlow(false)
    val isSavingBackground = _isSavingBackground.asStateFlow()

    fun saveThemePreset(themePreset: MusicThemePreset) {
        viewModelScope.launch {
            themeStore.saveThemePreset(themePreset)
        }
    }

    /** 用相册里选的图做背景：存一份到本地（只在本机），再删掉之前那张 */
    fun setBackgroundImage(uri: Uri) {
        if (_isSavingBackground.value) return
        viewModelScope.launch {
            _isSavingBackground.value = true
            try {
                val path = backgroundImageFiles.import(uri)
                themeStore.saveBackgroundImage(path)
                backgroundImageFiles.deleteAllExcept(path)
                _messages.emit("背景已更换")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "setBackgroundImage failed", e)
                _messages.emit("这张图片读取失败，换一张试试")
            } finally {
                _isSavingBackground.value = false
            }
        }
    }

    /** 恢复默认背景，并删掉本地存的背景图 */
    fun resetBackground() {
        viewModelScope.launch {
            themeStore.saveBackgroundImage(null)
            backgroundImageFiles.deleteAllExcept(null)
            _messages.emit("已恢复默认背景")
        }
    }

    private companion object {
        const val TAG = "ThemeViewModel"
    }
}
