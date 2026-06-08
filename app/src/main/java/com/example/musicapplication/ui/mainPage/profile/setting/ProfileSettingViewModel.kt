package com.example.musicapplication.ui.mainPage.profile.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.local.artwork.ArtworkCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSettingViewModel @Inject constructor(
    private val artworkCacheManager: ArtworkCacheManager
) : ViewModel() {

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message = _message.asSharedFlow()

    fun clearArtworkCache() {
        viewModelScope.launch {
            artworkCacheManager.clearArtworkCache()
            _message.tryEmit("图片缓存已清理")
        }
    }
}
