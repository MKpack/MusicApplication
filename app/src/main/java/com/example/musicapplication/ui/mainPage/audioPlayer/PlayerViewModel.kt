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
import com.example.musicapplication.domain.model.SongListKey
import com.example.musicapplication.domain.player.MediaControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.artwork.ArtworkCacheManager
import com.example.musicapplication.data.local.player.PlayerSnapshot
import com.example.musicapplication.data.local.player.PlayerStateStore
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.data.repository.UserStatRepository
import com.example.musicapplication.utils.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random


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
    private val songRepository: SongRepository,
    private val userStatRepository: UserStatRepository,
    private val mediaControllerManager: MediaControllerManager,
    private val artworkCacheManager: ArtworkCacheManager,
    private val playerStateStore: PlayerStateStore
): ViewModel() {
    private val TAG = "PlayerViewModel"
    // 具体播放能力由 PlaybackService 中的 ExoPlayer 处理；ViewModel 只保存 UI 状态和队列规则。
    val isPlaying = mediaControllerManager.isPlaying
    val currentPosition = mediaControllerManager.currentPosition
    val duration = mediaControllerManager.duration.stateIn(viewModelScope, SharingStarted.Lazily, 0)

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

    private var playbackListKey: SongListKey? = null
    private var playbackListPosition: Long? = null
    private val playbackPageSize = 20L

    // indictor progress 进度
    val currentProgress = mediaControllerManager.currentProgress.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val isDragging = mediaControllerManager.isDragging

    init {
        mediaControllerManager.connect()
        applyMediaRepeatMode(_playMode.value)
        restoreLastPlayerState()
        startPlayerSnapshotAutoSave()
        viewModelScope.launch {
            mediaControllerManager.playbackEndedEvents.collect {
                playNext()
            }
        }
        viewModelScope.launch {
            mediaControllerManager.mediaItemTransitionEvents.collect { index ->
                handleControllerMediaItemTransition(index)
            }
        }
    }

    //背景高糊色调
    private fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }


    // 新增方法获取bitmap
    private suspend fun loadColorBitmap() : Bitmap? {
        val song = _song.value
        val cover = song.cover ?: return null

        return withContext(Dispatchers.IO) {
            try {
                when {
                    // cache
                    cover.startsWith("http") -> {
                        val file = artworkCacheManager.getCachedArtworkFile(song)
                        file?.takeIf { it.exists() && it.length() > 0f }
                            ?.let {
                                BitmapFactory.decodeFile(file.absolutePath)
                            }
                    }
                    // 本地外部打开
                    else -> {
                        context.contentResolver.openInputStream(cover.toUri()).use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    // 暴露给UI层进行调用
    fun updateColorsFromSongCover() {
        viewModelScope.launch {
            val bitmap = loadColorBitmap() ?: BitmapFactory.decodeResource(context.resources, R.drawable.default_cover)
            updateColorsFromBitmap(bitmap)
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
        applyMediaRepeatMode(_playMode.value)
        savePlayerSnapshot()
    }

    private fun applyMediaRepeatMode(playMode: PlayMode) {
        val repeatMode = when (playMode) {
            PlayMode.Repeat -> Player.REPEAT_MODE_ALL
            PlayMode.RepeatOne -> Player.REPEAT_MODE_ONE
            PlayMode.Sequence,
            PlayMode.Shuffle -> Player.REPEAT_MODE_OFF
        }
        mediaControllerManager.setRepeatMode(repeatMode)
    }

    // 逻辑层和ui层分离
    fun updateProgress(tmp: Float) = mediaControllerManager.updatePosition(tmp)

    /**
     * 增加音乐播放次数
     */
    private fun increaseSongPlayCountIfRemote(song: Song) {
        val source = song.source

        if (source !is MusicSource.Remote) return
        if (song.songId <= 0) return

        viewModelScope.launch {
            songRepository.increaseSongPlayCount(song.songId)
        }
    }

    private fun increaseUserPlayCount() {
        viewModelScope.launch {
            userStatRepository.increaseUserStatPlayCount()
        }
    }

    /**
     * 收藏的点击事件
     */
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
        if (_playMode.value != PlayMode.Shuffle) {
            _originalQueue.value = newQueue
            _originalQueueKeys.value = newKeys
        }
        mediaControllerManager.moveQueueItem(
            fromIndex = fromIndex,
            toIndex = toIndex,
            songs = newQueue,
            currentIndex = _currentIndex.value
        )
        savePlayerSnapshot()
    }

    // 外部页面点击某个列表时调用。这里会创建一个新的播放队列，并给队列每一项生成稳定 key。
    fun playQueueSong(songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        if (index !in songs.indices) return
        playbackListKey = null
        playbackListPosition = null
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
            mediaControllerManager.playQueue(_songQueue.value, 0)
            onSongStarted(currentItem.first)
        } else {
            _songQueue.value = songs
            _queueKeys.value = queueKeys
            _currentIndex.value = index
            mediaControllerManager.playQueue(songs, index)
            onSongStarted(songs[index])
        }
        savePlayerSnapshot()
    }

    fun playQueueSong(listKey: SongListKey, songs: List<Song>, index: Int) {
        if (songs.isEmpty()) return
        if (index !in songs.indices) return

        val song = songs[index]
        viewModelScope.launch {
            val position = songRepository.getSongPosition(listKey, song.songId) ?: index.toLong()
            playbackListKey = listKey
            playbackListPosition = position

            val queueKeys = songs.map { nextQueueKey(it) }
            _originalQueue.value = songs
            _originalQueueKeys.value = queueKeys
            _songQueue.value = songs
            _queueKeys.value = queueKeys
            _currentIndex.value = index

            mediaControllerManager.playQueue(songs, index)
            onSongStarted(song)
            savePlayerSnapshot()
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

    // 随机排序
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
        val oldKeys = _queueKeys.value
        val shuffledSongs = shuffledQueue.map { it.first }
        val shuffledKeys = shuffledQueue.map { it.second }

        _playMode.value = PlayMode.Shuffle
        applyMediaRepeatMode(PlayMode.Shuffle)
        _songQueue.value = shuffledSongs
        _queueKeys.value = shuffledKeys

        if (currentItem == null) {
            _currentIndex.value = 0
            mediaControllerManager.playQueue(_songQueue.value, 0)
            onSongStarted(shuffledQueue[0].first)
        } else {
            _currentIndex.value = 0
            mediaControllerManager.reorderQueueByMoves(
                moves = buildQueueMoveOperations(oldKeys, shuffledKeys),
                songs = shuffledSongs,
                currentIndex = 0
            )
        }
        savePlayerSnapshot()
    }

    private fun restoreOriginalQueue(nextMode: PlayMode) {
        // 从随机播放切回其他模式时，恢复原始队列，并用 key 找回当前歌曲的位置。
        val originalQueue = _originalQueue.value
        val originalKeys = _originalQueueKeys.value
        val currentKey = _queueKeys.value.getOrNull(_currentIndex.value)

        _playMode.value = nextMode
        applyMediaRepeatMode(nextMode)

        if (originalQueue.isEmpty()) return

        val oldKeys = _queueKeys.value
        _songQueue.value = originalQueue
        _queueKeys.value = originalKeys
        _currentIndex.value = originalKeys.indexOf(currentKey).coerceAtLeast(0)
        mediaControllerManager.reorderQueueByMoves(
            moves = buildQueueMoveOperations(oldKeys, originalKeys),
            songs = originalQueue,
            currentIndex = _currentIndex.value
        )
        savePlayerSnapshot()
    }

    private fun buildQueueMoveOperations(
        currentKeys: List<String>,
        targetKeys: List<String>
    ): List<Pair<Int, Int>> {
        if (currentKeys.size != targetKeys.size) return emptyList()

        val workingKeys = currentKeys.toMutableList()
        val moves = mutableListOf<Pair<Int, Int>>()

        targetKeys.forEachIndexed { targetIndex, targetKey ->
            val fromIndex = workingKeys.indexOf(targetKey)
            if (fromIndex == -1) return@forEachIndexed
            if (fromIndex != targetIndex) {
                val key = workingKeys.removeAt(fromIndex)
                workingKeys.add(targetIndex, key)
                moves += fromIndex to targetIndex
            }
        }

        return moves
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

    /**
     * 播放音乐
     */
    fun playSong(song: Song) {
        song.source ?: return
        playbackListKey = null
        playbackListPosition = null
        val queueKey = nextQueueKey(song)
        _songQueue.value = listOf(song)
        _queueKeys.value = listOf(queueKey)
        _originalQueue.value = listOf(song)
        _originalQueueKeys.value = listOf(queueKey)
        _currentIndex.value = 0
        mediaControllerManager.playSong(song)
        onSongStarted(song)
        savePlayerSnapshot()
    }

    private fun onSongStarted(song: Song) {
        _song.value = song
        _songUiState.value = song.toSongUiState()
        loadLyrics(song.lyric)
        // 增加音乐播放次数
        increaseSongPlayCountIfRemote(song)
        // 增加用户播放次数
        increaseUserPlayCount()
        // 只要开始播放，就追加一条最近播放记录；同一首歌重复播放也会重复记录。
        viewModelScope.launch {
            songRepository.addRecentPlay(song)
        }
    }

    private fun handleControllerMediaItemTransition(index: Int) {
        if (index !in _songQueue.value.indices) return
        if (index == _currentIndex.value) return

        _currentIndex.value = index
        onSongStarted(_songQueue.value[index])
        savePlayerSnapshot()
    }

    // 队列内部点击歌曲时调用，只改变当前 index，不重新创建队列和 key。
    fun playQueueIndex(index: Int) {
        val songs = _songQueue.value
        if (index !in songs.indices) return
        _currentIndex.value = index
        mediaControllerManager.playQueueIndex(index)
        onSongStarted(songs[index])
        savePlayerSnapshot()
    }

    // 下一首
    fun playNext() {
        val listKey = playbackListKey
        val listPosition = playbackListPosition
        if (listKey != null && listPosition != null) {
            viewModelScope.launch {
                playNextFromPagedSource(listKey, listPosition)
            }
            return
        }

        val songs = _songQueue.value
        val index = _currentIndex.value
        if (songs.isEmpty()) return
        if (index !in songs.indices) return

        val nextIndex = when(_playMode.value) {
            PlayMode.Repeat -> {
                if (index + 1 < songs.size) index + 1 else 0
            }
            PlayMode.RepeatOne -> {
                if (index + 1 < songs.size) index + 1 else 0
            }
            PlayMode.Shuffle,
            PlayMode.Sequence -> {
                if (index + 1 < songs.size) index + 1
                else {
                    mediaControllerManager.pauseAtStart()
                    savePlayerSnapshot()
                    return
                }
            }
        }
        _currentIndex.value = nextIndex
        mediaControllerManager.playQueueIndex(nextIndex)
        onSongStarted(songs[nextIndex])
        savePlayerSnapshot()
    }

    private suspend fun playNextFromPagedSource(
        listKey: SongListKey,
        currentPosition: Long
    ) {
        val nextPosition = when (_playMode.value) {
            PlayMode.Shuffle -> randomPosition(listKey, currentPosition) ?: return
            PlayMode.Repeat,
            PlayMode.RepeatOne,
            PlayMode.Sequence -> currentPosition + 1
        }

        when (val result = songRepository.ensureSongAtPosition(listKey, nextPosition, playbackPageSize)) {
            is RepositoryWorkResult.Success -> {
                val song = result.data
                if (song != null) {
                    playPagedSourceSong(listKey, nextPosition, song)
                    return
                }

                if (_playMode.value == PlayMode.Repeat || _playMode.value == PlayMode.RepeatOne) {
                    val firstSong = songRepository.ensureSongAtPosition(listKey, 0, playbackPageSize)
                    if (firstSong is RepositoryWorkResult.Success && firstSong.data != null) {
                        playPagedSourceSong(listKey, 0, firstSong.data)
                    }
                } else {
                    mediaControllerManager.pauseAtStart()
                    savePlayerSnapshot()
                }
            }
            is RepositoryWorkResult.Failure -> {
                _songUiState.value = _songUiState.value.copy(errorMessage = result.message)
            }
        }
    }

    private fun nextQueueKey(song: Song): String {
        // songId 可能重复，source 也可能重复，所以加自增 seed 让每个队列项实例都唯一。
        queueKeySeed += 1
        return "${queueKeySeed}-${song.songId}-${song.source}"
    }

    // 上一首
    fun playPrevious() {
        val listKey = playbackListKey
        val listPosition = playbackListPosition
        if (listKey != null && listPosition != null) {
            viewModelScope.launch {
                playPreviousFromPagedSource(listKey, listPosition)
            }
            return
        }

        val songs = _songQueue.value
        val index = _currentIndex.value
        if (songs.isEmpty()) return
        if (index !in songs.indices) return


        val nextIndex = when (_playMode.value) {
            PlayMode.RepeatOne,
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
        mediaControllerManager.playQueueIndex(nextIndex)
        onSongStarted(songs[nextIndex])
        savePlayerSnapshot()
    }

    private suspend fun playPreviousFromPagedSource(
        listKey: SongListKey,
        currentPosition: Long
    ) {
        val nextPosition = when (_playMode.value) {
            PlayMode.Shuffle -> randomPosition(listKey, currentPosition) ?: return
            PlayMode.Repeat,
            PlayMode.RepeatOne -> if (currentPosition > 0) currentPosition - 1 else {
                val total = songRepository.getSongListTotal(listKey) ?: return
                (total - 1).coerceAtLeast(0)
            }
            PlayMode.Sequence -> if (currentPosition > 0) currentPosition - 1 else return
        }

        when (val result = songRepository.ensureSongAtPosition(listKey, nextPosition, playbackPageSize)) {
            is RepositoryWorkResult.Success -> {
                result.data?.let { song ->
                    playPagedSourceSong(listKey, nextPosition, song)
                }
            }
            is RepositoryWorkResult.Failure -> {
                _songUiState.value = _songUiState.value.copy(errorMessage = result.message)
            }
        }
    }

    private suspend fun randomPosition(
        listKey: SongListKey,
        currentPosition: Long
    ): Long? {
        val total = songRepository.getSongListTotal(listKey) ?: return null
        if (total <= 0) return null
        if (total == 1L) return 0L

        var nextPosition: Long
        do {
            nextPosition = Random.nextLong(0, total)
        } while (nextPosition == currentPosition)

        return nextPosition
    }

    private fun playPagedSourceSong(
        listKey: SongListKey,
        position: Long,
        song: Song
    ) {
        playbackListKey = listKey
        playbackListPosition = position

        val currentQueue = _songQueue.value
        val existingIndex = currentQueue.indexOfFirst { it.songId == song.songId }
        if (existingIndex >= 0) {
            _currentIndex.value = existingIndex
            mediaControllerManager.playQueueIndex(existingIndex)
        } else {
            val newQueue = currentQueue + song
            val newKeys = _queueKeys.value + nextQueueKey(song)
            _songQueue.value = newQueue
            _queueKeys.value = newKeys
            _currentIndex.value = newQueue.lastIndex
            mediaControllerManager.playQueue(newQueue, newQueue.lastIndex)
        }

        onSongStarted(song)
        savePlayerSnapshot()
    }

    // 暂停
    fun pause() {
        mediaControllerManager.pause()
        savePlayerSnapshot()
    }
    // 继续
    fun resume() = mediaControllerManager.resume()
    // 跳转指定位置
    fun seekTo() {
        mediaControllerManager.seekTo()
        savePlayerSnapshot()
    }

    fun changeDraggingStatus(tmp: Boolean) = mediaControllerManager.isDraggingStatusChange(tmp)

    private fun restoreLastPlayerState() {
        viewModelScope.launch {
            val snapshot = playerStateStore.snapshotFlow.first() ?: return@launch
            val songs = songRepository.observeSongsByIds(snapshot.songIds).first()
            if (songs.isEmpty()) return@launch

            val safeIndex = snapshot.currentIndex.coerceIn(songs.indices)
            val queueKeys = songs.map { nextQueueKey(it) }
            val playMode = runCatching {
                PlayMode.valueOf(snapshot.playModeName)
            }.getOrDefault(PlayMode.Repeat)

            _playMode.value = playMode
            applyMediaRepeatMode(playMode)
            _songQueue.value = songs
            _queueKeys.value = queueKeys
            _originalQueue.value = songs
            _originalQueueKeys.value = queueKeys
            _currentIndex.value = safeIndex
            setCurrentSongForRestore(songs[safeIndex])

            mediaControllerManager.restoreQueue(
                songs = songs,
                startIndex = safeIndex,
                positionMs = snapshot.positionMs
            )
        }
    }

    private fun setCurrentSongForRestore(song: Song) {
        _song.value = song
        _songUiState.value = song.toSongUiState()
        loadLyrics(song.lyric)
    }

    private fun startPlayerSnapshotAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                savePlayerSnapshot()
            }
        }
    }

    private fun savePlayerSnapshot() {
        val songs = _songQueue.value
        val currentIndex = _currentIndex.value
        if (songs.isEmpty()) return
        if (currentIndex !in songs.indices) return

        viewModelScope.launch {
            playerStateStore.saveSnapshot(
                PlayerSnapshot(
                    songIds = songs.map { it.songId },
                    currentIndex = currentIndex,
                    positionMs = currentPosition.value * 1000L,
                    playModeName = _playMode.value.name
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
