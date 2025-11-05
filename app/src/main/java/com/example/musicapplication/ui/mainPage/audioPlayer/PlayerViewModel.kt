package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.musicapplication.R
import com.example.musicapplication.data.remote.model.Song
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.player.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    private val TAG = "PlayerViewModel"
    //播放控制器
    private val playerManager = MusicPlayerManager(context)
    private val song = Song(20000,"想你的夜", "关喆", true, R.drawable.xiangnideye)

    //监听playerManager.isPlaying 初始值false 懒加载(flow -> stateflow)
    val isPlaying = playerManager.isPlaying.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val currentPosition = playerManager.currentPosition.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val duration = playerManager.duration.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    //song Title
    private val _songTitle = MutableStateFlow(song.songTitle)
    val songTitle: StateFlow<String> = _songTitle

    //song singer
    private val _songSinger = MutableStateFlow(song.singer)
    val songSinger: StateFlow<String> = _songSinger

    //song bitmap
    private val _songBitmap = MutableStateFlow(song.bitmap)
    val songBitmap: StateFlow<Int> = _songBitmap

    //song isLove
    private val _isLoved = MutableStateFlow(song.isLoved)
    val songIsLove = _isLoved

    //indictor progress 进度
    val currentProgress = playerManager.currentProgress.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val isDragging = playerManager.isDragging.stateIn(viewModelScope, SharingStarted.Lazily, false)
    //left_time, all_time
//    private val _leftTime = MutableStateFlow(0)
//    val leftTime: StateFlow<Int> = _leftTime
//    private val _allTime = MutableStateFlow(song.songTimeLong)
//    val allTime: StateFlow<Int> = _allTime

    var isFirst = true
    //背景高糊色调
    fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }

    //逻辑层和ui层分离
    fun updateProgress(tmp: Float) = playerManager.updatePosition(tmp)

    fun updateLoveStatus() {
        _isLoved.value = !_isLoved.value
    }
    //播放音乐
    fun play(musicSource: MusicSource){
        if (isFirst) {
            playerManager.playMusic(musicSource)
            isFirst = false
        }
        else {
            resume()
        }
    }
    //暂停
    fun pause() = playerManager.pause()
    //继续
    fun resume() = playerManager.resume()
    //跳转指定位置
    fun seekTo() = playerManager.seekTo()

    fun changeDraggingStatus(tmp: Boolean) = playerManager.isDraggingStatusChange(tmp)
}