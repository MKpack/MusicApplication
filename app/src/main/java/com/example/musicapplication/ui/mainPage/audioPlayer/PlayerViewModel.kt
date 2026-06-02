package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.example.musicapplication.R
import com.example.musicapplication.domain.model.Song
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
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient


enum class PlayMode {
    Sequence, // 顺序播放
    Repeat,   // 循环播放
    Shuffle,  // 随机播放
    RepeatOne // 单曲循环
}

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
): ViewModel() {
    private val TAG = "PlayerViewModel"
    //播放控制器
    private val playerManager = MusicPlayerManager(
        context,
        okHttpClient,
        onMusicEnded = { playNext() }
    )
    //监听playerManager.isPlaying 初始值false 懒加载(flow -> stateflow)
    val isPlaying = playerManager.isPlaying.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val currentPosition = playerManager.currentPosition.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val duration = playerManager.duration.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    private val _playMode = MutableStateFlow(PlayMode.Repeat)
    val playMode = _playMode.asStateFlow()

    //直接管理song
    private val _song = MutableStateFlow<Song>(Song(
        songId = -1,
        songTitle = "未知歌曲",
        singer = "未知歌手",
        isLoved = false,
        cover = null,
        source = null
    ))
    val songValue = _song.asStateFlow()

    // 管理列表
    private val _songQueue = MutableStateFlow<List<Song>>(emptyList())
    val songQueue = _songQueue.asStateFlow()
    // 新增原始列表 ----> 应对随机
    private val _originalQueue = MutableStateFlow<List<Song>>(emptyList())
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()

    // indictor progress 进度
    val currentProgress = playerManager.currentProgress.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val isDragging = playerManager.isDragging.stateIn(viewModelScope, SharingStarted.Lazily, false)

    //背景高糊色调
    private fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }

    /**
     * 循环和单曲循环的切换
     */
    fun changeRepeatMode() {
        _playMode.value = when (_playMode.value) {
            PlayMode.Repeat -> PlayMode.RepeatOne
            else -> PlayMode.Repeat
        }
    }

    //暴露给UI层进行调用
    fun updateColorsFromSongCover() {
        viewModelScope.launch {
            val cover = _song.value.cover
            when {
                //本地uri/文件路径
                cover != null -> {
                    try {
                        val uri = cover.toUri()
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        bitmap?.let { updateColorsFromBitmap(bitmap) }
                    }catch (e: Exception) {
                        // Uri 无效或无法读取 → fallback 到默认封面
                        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.default_cover)
                        updateColorsFromBitmap(bitmap)
                    }
                }
                //cover 值为空， 使用默认cover
                else -> {
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.default_cover)
                    updateColorsFromBitmap(bitmap)
                }
            }
        }
    }

    fun changePlayMode(playMode: PlayMode) {
        when (playMode) {
            PlayMode.Shuffle -> enableShuffleMode()
            PlayMode.Sequence,
            PlayMode.Repeat,
            PlayMode.RepeatOne -> {
                if (_playMode.value == PlayMode.Shuffle) {
                    restoreOriginalQueue(playMode)
                } else {
                    _playMode.value = playMode
                }
            }
        }
    }

    // 逻辑层和ui层分离
    fun updateProgress(tmp: Float) = playerManager.updatePosition(tmp)

    fun updateLoveStatus() {
        _song.value = _song.value.copy(isLoved = !_song.value.isLoved)
    }

    fun moveQueueSong(fromIndex: Int, toIndex: Int) {
        val currentQueue = _songQueue.value
        if (fromIndex !in currentQueue.indices) return
        if (toIndex !in currentQueue.indices) return
        if (fromIndex == toIndex) return

        val newQueue = currentQueue.toMutableList()
        val song = newQueue.removeAt(fromIndex)
        newQueue.add(toIndex, song)
        _songQueue.value = newQueue

        _currentIndex.value = newQueue.indexOfFirst {
            it.songId == _song.value.songId
        }
    }

    // 播放音乐
    fun playQueueSong(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        if (index !in songs.indices) return
        _originalQueue.value = songs

        if (_playMode.value == PlayMode.Shuffle) {
            val currentSong = songs[index]
            val shuffledQueue = buildShuffleQueue(songs, currentSong)

            _songQueue.value = shuffledQueue
            _currentIndex.value = 0
            playSong(currentSong)
        } else {
            _songQueue.value = songs
            _currentIndex.value = index
            playSong(songs[index])
        }
    }
    // 打乱音乐列表<随机>
    private fun buildShuffleQueue(
        songs: List<Song>,
        currentSong: Song?
    ): List<Song> {
        if (songs.isEmpty()) return emptyList()

        if (currentSong == null || currentSong.songId.toInt() == -1) {
            return songs.shuffled()
        }

        val rest = songs
            .filter { it.songId != currentSong.songId }
            .shuffled()

        return listOf(currentSong) + rest
    }

    private fun enableShuffleMode() {
        val baseQueue = _originalQueue.value.ifEmpty {
            _songQueue.value
        }

        if (baseQueue.isEmpty()) {
            _playMode.value = PlayMode.Shuffle
            return
        }

        val currentSong = _song.value.takeIf {
            it.songId.toInt() != -1
        }

        val shuffledQueue = buildShuffleQueue(
            songs = baseQueue,
            currentSong = currentSong
        )

        _playMode.value = PlayMode.Shuffle
        _songQueue.value = shuffledQueue

        if (currentSong == null) {
            _currentIndex.value = 0
            playSong(shuffledQueue[0])
        } else {
            _currentIndex.value = 0
        }
    }

    private fun restoreOriginalQueue(nextMode: PlayMode) {
        val originalQueue = _originalQueue.value
        val currentSongId = _song.value.songId

        _playMode.value = nextMode

        if (originalQueue.isEmpty()) return

        _songQueue.value = originalQueue
        _currentIndex.value = originalQueue.indexOfFirst {
            it.songId == currentSongId
        }.coerceAtLeast(0)
    }

    fun playSong(song: Song) {
        song.source ?: return
        _song.value = song
        playerManager.playMusic(song.source)
    }

    // 下一首
    fun playNext() {
        val songs = _songQueue.value
        val index = _currentIndex.value
        if (songs.isEmpty()) return

        val nextIndex = when(_playMode.value) {
            PlayMode.Repeat -> {
                if (index + 1 < songs.size) index + 1 else 0
            }
            PlayMode.RepeatOne -> {
                index
            }
            PlayMode.Shuffle,
            PlayMode.Sequence -> {
                if (index + 1 < songs.size) index + 1
                else {
                    playerManager.pauseAtStart()
                    return
                }
            }
        }
        _currentIndex.value = nextIndex
        playSong(songs[nextIndex])
    }

    // 上一首
    fun playPrevious() {
        val songs = _songQueue.value
        val index = _currentIndex.value
        if (songs.isEmpty()) return
        if (index !in songs.indices) return


        val nextIndex = when (_playMode.value) {
            PlayMode.RepeatOne -> index
            PlayMode.Repeat -> {
                if (index - 1 >= 0) index - 1 else songs.size - 1
            }
            PlayMode.Sequence,
            PlayMode.Shuffle -> {
                if (index > 0) {
                    index - 1
                } else {
                    return
                }
            }
        }
        _currentIndex.value = nextIndex
        playSong(songs[nextIndex])
    }

    // 暂停
    fun pause() = playerManager.pause()
    // 继续
    fun resume() = playerManager.resume()
    // 跳转指定位置
    fun seekTo() = playerManager.seekTo()

    fun changeDraggingStatus(tmp: Boolean) = playerManager.isDraggingStatusChange(tmp)

    // release
    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }
}