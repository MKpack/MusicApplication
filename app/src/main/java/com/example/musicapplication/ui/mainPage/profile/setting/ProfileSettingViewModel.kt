package com.example.musicapplication.ui.mainPage.profile.setting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.example.musicapplication.playback.AudioCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileSettingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioCache: AudioCache
) : ViewModel() {

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message = _message.asSharedFlow()

    private var isClearing = false

    /** 清除歌曲音频缓存和图片缓存（封面、头像）。已下载的歌不受影响 */
    fun clearCache() {
        if (isClearing) return
        isClearing = true
        viewModelScope.launch {
            audioCache.clear()
            val imageLoader = context.imageLoader
            imageLoader.memoryCache?.clear()
            withContext(Dispatchers.IO) {
                imageLoader.diskCache?.clear()
                // 旧版本单独存的封面
                File(context.cacheDir, "artwork").deleteRecursively()
            }
            isClearing = false
            _message.tryEmit("缓存已清除")
        }
    }
}
