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
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.utils.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


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
    private val okHttpClient: OkHttpClient,
    private val songRepository: SongRepository
): ViewModel() {
    private val TAG = "PlayerViewModel"
    // 具体播放能力由 MusicPlayerManager 处理；ViewModel 只保存 UI 状态和队列规则。
    private val playerManager = MusicPlayerManager(
        context,
        okHttpClient,
        onMusicEnded = { playNext() }
    )
    //监听playerManager.isPlaying 初始值false 懒加载(flow -> stateflow)
    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    // 播放状态
    private val _playMode = MutableStateFlow(PlayMode.Repeat)
    val playMode = _playMode.asStateFlow()

    // lyric
    private val _lyricsUiState = MutableStateFlow(LyricsUiState())
    val lyricsUiState = _lyricsUiState.asStateFlow()

    // 当前正在播放或准备播放的歌曲，播放器顶部、全屏页、迷你播放器都读这份状态。
    private val _song = MutableStateFlow<Song>(Song(
        songId = -1,
        songTitle = "未知歌曲",
        singer = "未知歌手",
        isLoved = false,
        cover = null,
        source = null
    ))

    private val _songUiState = MutableStateFlow(_song.value.toSongUiState())
    val songUiState = _songUiState.asStateFlow()

    // 当前播放队列。它只存歌曲本身，UI 展示和上一首/下一首都基于这份列表。
    private val _songQueue = MutableStateFlow<List<Song>>(emptyList())
    val songQueue = _songQueue.asStateFlow()

    // 队列项的稳定 key。最近播放允许同一首歌重复出现，所以不能用 songId 当 LazyColumn/Reorderable key。
    // queueKeys 与 songQueue 一一对应；拖动歌曲时，两份列表必须一起移动。
    private val _queueKeys = MutableStateFlow<List<String>>(emptyList())
    val queueKeys = _queueKeys.asStateFlow()
    private var queueKeySeed = 0L

    // 原始队列用于从随机播放切回顺序/循环时恢复原顺序。
    private val _originalQueue = MutableStateFlow<List<Song>>(emptyList())
    private val _originalQueueKeys = MutableStateFlow<List<String>>(emptyList())

    // 当前播放项在 songQueue 中的位置。注意它是队列位置，不是 songId。
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

    fun doFavoriteEvent() {
        val song = _song.value
        if (_songUiState.value.isFavoriteLoading || !_songUiState.value.canFavorite) return
        val targetLoved = !song.isLoved

        updateCurrentSongLoved(
            isLoved = targetLoved,
            isFavoriteLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            val result = if (song.isLoved) {
                songRepository.unFavoriteSong(song.songId)
            } else {
                songRepository.favoriteSong(song.songId)
            }

            if (result is RepositoryWorkResult.Failure) {
                updateCurrentSongLoved(
                    isLoved = song.isLoved,
                    isFavoriteLoading = false,
                    errorMessage = result.message
                )
            } else {
                updateCurrentSongLoved(
                    isLoved = targetLoved,
                    isFavoriteLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun updateCurrentSongLoved(
        isLoved: Boolean,
        isFavoriteLoading: Boolean,
        errorMessage: String?
    ) {
        val currentSong = _song.value.copy(isLoved = isLoved)
        _song.value = currentSong
        _songUiState.value = currentSong.toSongUiState(
            isFavoriteLoading = isFavoriteLoading,
            errorMessage = errorMessage
        )

        val currentKey = _queueKeys.value.getOrNull(_currentIndex.value)
        _songQueue.value = updateSongLovedInQueue(
            songs = _songQueue.value,
            songId = currentSong.songId,
            isLoved = isLoved
        )
        _originalQueue.value = updateSongLovedInQueue(
            songs = _originalQueue.value,
            songId = currentSong.songId,
            isLoved = isLoved
        )

        if (currentKey != null) {
            _currentIndex.value = _queueKeys.value.indexOf(currentKey).coerceAtLeast(0)
        }
    }

    private fun updateSongLovedInQueue(
        songs: List<Song>,
        songId: Long,
        isLoved: Boolean
    ): List<Song> {
        return songs.map { song ->
            if (song.songId == songId) {
                song.copy(isLoved = isLoved)
            } else {
                song
            }
        }
    }

    // 移动歌曲位置时调用
    fun moveQueueSong(fromIndex: Int, toIndex: Int) {
        val currentQueue = _songQueue.value
        if (fromIndex !in currentQueue.indices) return
        if (toIndex !in currentQueue.indices) return
        if (fromIndex == toIndex) return

        val currentKey = _queueKeys.value.getOrNull(_currentIndex.value)
        val newQueue = currentQueue.toMutableList()
        val newKeys = _queueKeys.value.toMutableList()
        val song = newQueue.removeAt(fromIndex)
        val key = newKeys.removeAt(fromIndex)
        newQueue.add(toIndex, song)
        newKeys.add(toIndex, key)
        _songQueue.value = newQueue
        _queueKeys.value = newKeys

        _currentIndex.value = newKeys.indexOf(currentKey).coerceAtLeast(0)
    }

    // 外部页面点击某个列表时调用。这里会创建一个新的播放队列，并给队列每一项生成稳定 key。
    fun playQueueSong(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        if (index !in songs.indices) return
        val queueKeys = songs.map { nextQueueKey(it) }
        _originalQueue.value = songs
        _originalQueueKeys.value = queueKeys

        if (_playMode.value == PlayMode.Shuffle) {
            val pairedQueue = songs.zip(queueKeys)
            val currentItem = pairedQueue[index]
            val shuffledQueue = buildShuffleQueue(pairedQueue, currentItem)

            _songQueue.value = shuffledQueue.map { it.first }
            _queueKeys.value = shuffledQueue.map { it.second }
            _currentIndex.value = 0
            playSong(currentItem.first)
        } else {
            _songQueue.value = songs
            _queueKeys.value = queueKeys
            _currentIndex.value = index
            playSong(songs[index])
        }
    }

    // 构造随机播放队列。当前歌曲固定在第一位，剩余歌曲随机，避免切换随机后立刻换歌。
    private fun buildShuffleQueue(
        songs: List<Pair<Song, String>>,
        currentSong: Pair<Song, String>?
    ): List<Pair<Song, String>> {
        if (songs.isEmpty()) return emptyList()

        if (currentSong == null || currentSong.first.songId.toInt() == -1) {
            return songs.shuffled()
        }

        val rest = songs
            .filter { it.second != currentSong.second }
            .shuffled()

        return listOf(currentSong) + rest
    }

    private fun enableShuffleMode() {
        // 优先基于原始队列打乱；如果没有原始队列，就用当前队列。
        val baseQueue = _originalQueue.value.ifEmpty {
            _songQueue.value
        }
        val baseKeys = _originalQueueKeys.value.ifEmpty {
            _queueKeys.value
        }

        if (baseQueue.isEmpty()) {
            _playMode.value = PlayMode.Shuffle
            return
        }

        val currentKey = _queueKeys.value.getOrNull(_currentIndex.value)
        val pairedQueue = baseQueue.zip(baseKeys)
        val currentItem = pairedQueue.find {
            it.second == currentKey
        } ?: _song.value.takeIf {
            it.songId.toInt() != -1
        }?.let { song ->
            pairedQueue.find { it.first == song }
        }

        val shuffledQueue = buildShuffleQueue(
            songs = pairedQueue,
            currentSong = currentItem
        )

        _playMode.value = PlayMode.Shuffle
        _songQueue.value = shuffledQueue.map { it.first }
        _queueKeys.value = shuffledQueue.map { it.second }

        if (currentItem == null) {
            _currentIndex.value = 0
            playSong(shuffledQueue[0].first)
        } else {
            _currentIndex.value = 0
        }
    }

    private fun restoreOriginalQueue(nextMode: PlayMode) {
        // 从随机播放切回其他模式时，恢复原始队列，并用 key 找回当前歌曲的位置。
        val originalQueue = _originalQueue.value
        val originalKeys = _originalQueueKeys.value
        val currentKey = _queueKeys.value.getOrNull(_currentIndex.value)

        _playMode.value = nextMode

        if (originalQueue.isEmpty()) return

        _songQueue.value = originalQueue
        _queueKeys.value = originalKeys
        _currentIndex.value = originalKeys.indexOf(currentKey).coerceAtLeast(0)
    }

    private fun loadLyrics(lyricUrl: String?) {
        if (lyricUrl.isNullOrEmpty()) {
            _lyricsUiState.value = LyricsUiState(
                lines = emptyList(),
                errorMessage = "暂无歌词"
            )
            return
        }

        viewModelScope.launch {
            _lyricsUiState.value = LyricsUiState(isLoading = true)
            try {
                val request = Request.Builder()
                    .url(lyricUrl)
                    .build()

                val text = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        response.body?.string()
                    }
                }

                val lines = text
                    ?.let { LrcParser.parse(it) }
                    .orEmpty()

                _lyricsUiState.value = LyricsUiState(
                    isLoading = false,
                    lines = lines,
                    errorMessage = if (lines.isEmpty()) "暂无歌词" else null
                )
            } catch (e: Exception) {
                _lyricsUiState.value = LyricsUiState(
                    isLoading = false,
                    lines = emptyList(),
                    errorMessage = "暂无歌词"
                )
            }
        }
    }

    fun playSong(song: Song) {
        song.source ?: return
        _song.value = song
        _songUiState.value = song.toSongUiState()
        playerManager.playMusic(song.source)
        loadLyrics(song.lyric)
        // 只要开始播放，就追加一条最近播放记录；同一首歌重复播放也会重复记录。
        viewModelScope.launch {
            songRepository.addRecentPlay(song)
        }
    }

    // 队列内部点击歌曲时调用，只改变当前 index，不重新创建队列和 key。
    fun playQueueIndex(index: Int) {
        val songs = _songQueue.value
        if (index !in songs.indices) return
        _currentIndex.value = index
        playSong(songs[index])
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

    private fun nextQueueKey(song: Song): String {
        // songId 可能重复，source 也可能重复，所以加自增 seed 让每个队列项实例都唯一。
        queueKeySeed += 1
        return "${queueKeySeed}-${song.songId}-${song.source}"
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
