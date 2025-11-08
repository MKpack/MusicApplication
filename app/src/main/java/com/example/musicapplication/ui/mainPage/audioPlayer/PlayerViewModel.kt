package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    private val TAG = "PlayerViewModel"
    private val appContext = context
    //播放控制器
    private val playerManager = MusicPlayerManager(context)
    //监听playerManager.isPlaying 初始值false 懒加载(flow -> stateflow)
    val isPlaying = playerManager.isPlaying.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val currentPosition = playerManager.currentPosition.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val duration = playerManager.duration.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    //直接管理song
    private val _song = MutableStateFlow<Song>(Song(songId=-1, songTitle="未知歌曲", singer="未知歌手", isLoved=false, cover=null))
    val songValue = _song.asStateFlow()
//    //song Title
//    private val _songTitle = MutableStateFlow(song.songTitle)
//    val songTitle: StateFlow<String> = _songTitle
//
//    //song singer
//    private val _songSinger = MutableStateFlow(song.singer)
//    val songSinger: StateFlow<String> = _songSinger
//
//    //song bitmap
//    private val _songCover = MutableStateFlow(song.cover)
//    val songCover: StateFlow<String?> = _songCover
//
//    //song isLove
//    private val _isLoved = MutableStateFlow(song.isLoved)
//    val songIsLove = _isLoved

    //indictor progress 进度
    val currentProgress = playerManager.currentProgress.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val isDragging = playerManager.isDragging.stateIn(viewModelScope, SharingStarted.Lazily, false)
    //left_time, all_time
//    private val _leftTime = MutableStateFlow(0)
//    val leftTime: StateFlow<Int> = _leftTime
//    private val _allTime = MutableStateFlow(song.songTimeLong)
//    val allTime: StateFlow<Int> = _allTime

    //背景高糊色调
    private fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }
    //暴露给UI层进行调用
    fun updateColorsFromSongCover() {
        val cover = _song.value.cover
        when {
            //网络封面 url
            cover != null && cover.startsWith("http") -> {
                viewModelScope.launch {

                }
            }
            //本地uri/文件路径
            cover != null -> {
                try {
                    val uri = cover.toUri()
                    val inputStream = appContext.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bitmap?.let { updateColorsFromBitmap(bitmap) }
                }catch (e: Exception) {
                    // Uri 无效或无法读取 → fallback 到默认封面
                    val bitmap = BitmapFactory.decodeResource(appContext.resources, R.drawable.default_cover)
                    updateColorsFromBitmap(bitmap)
                }
            }
            //cover 值为空， 使用默认cover
            else -> {
                val bitmap = BitmapFactory.decodeResource(appContext.resources, R.drawable.default_cover)
                updateColorsFromBitmap(bitmap)
            }
        }
    }

    //逻辑层和ui层分离
    fun updateProgress(tmp: Float) = playerManager.updatePosition(tmp)

    fun updateLoveStatus() {
        _song.value = _song.value.copy(isLoved = !_song.value.isLoved)
    }

    //播放音乐
    fun selectSong(musicSource: MusicSource, song: Song) {
        _song.value = song
        playerManager.playMusic(musicSource)
    }
    //暂停
    fun pause() = playerManager.pause()
    //继续
    fun resume() = playerManager.resume()
    //跳转指定位置
    fun seekTo() = playerManager.seekTo()

    fun changeDraggingStatus(tmp: Boolean) = playerManager.isDraggingStatusChange(tmp)
}