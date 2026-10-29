package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.musicapplication.R
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import com.example.musicapplication.domain.player.PlayMode
import com.example.musicapplication.domain.player.PlaybackState
import com.example.musicapplication.domain.player.PlayerController
import com.example.musicapplication.utils.LrcParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/**
 * 播放器界面的 ViewModel。播放队列、播放状态都在 PlaybackService 里，这里只负责：
 * 把播放状态整理成界面要的样子、加载歌词和封面颜色、处理收藏和下载。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("media") private val okHttpClient: OkHttpClient,
    private val songRepository: SongRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playerController.state

    // 播放进度（毫秒），只有进度条、歌词在显示时才会去取
    val positionMs: StateFlow<Long> = playerController.positionMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val isPlaying: StateFlow<Boolean> = playbackState
        .map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 当前播放的那一项，同一项继续播放时不重复触发
    private val currentItem = playbackState
        .map { it.currentItem }
        .distinctUntilChanged { old, new -> old?.id == new?.id && old?.artworkUri == new?.artworkUri }

    // 正在收藏 / 取消收藏的歌
    private val favoriteLoadingIds = MutableStateFlow<Set<Long>>(emptySet())

    // 收藏状态以本地数据库为准，在别的页面收藏了这里也会跟着变
    val songUiState: StateFlow<SongUiState> = currentItem
        .flatMapLatest { item ->
            if (item == null) {
                flowOf(SongUiState())
            } else {
                combine(
                    songRepository.observeSongLoved(item.song.songId),
                    favoriteLoadingIds
                ) { loved, loadingIds ->
                    item.toSongUiState(
                        isLoved = loved ?: item.song.isLoved,
                        isFavoriteLoading = item.song.songId in loadingIds
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SongUiState())

    // 下载过的歌优先读本地歌词
    val lyricsUiState: StateFlow<LyricsUiState> = currentItem
        .map { item -> item?.let { it.localLyricPath ?: it.song.lyric } }
        .distinctUntilChanged()
        .transformLatest { path ->
            if (path.isNullOrEmpty()) {
                emit(LyricsUiState(errorMessage = "暂无歌词"))
                return@transformLatest
            }
            emit(LyricsUiState(isLoading = true))
            val lines = loadLyrics(path)
            emit(LyricsUiState(lines = lines, errorMessage = if (lines.isEmpty()) "暂无歌词" else null))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LyricsUiState())

    // 全屏播放器背景用的重点颜色，从封面里挑
    val coverColors: StateFlow<CoverColors> = currentItem
        .map { it?.artworkUri ?: it?.song?.cover }
        .distinctUntilChanged()
        .mapLatest { cover -> loadCoverColors(cover) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CoverColors.Default)

    private val localMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    // 需要弹 Toast 的提示：播放出错、跳过歌曲、下载、收藏失败
    val message: Flow<String> = merge(playerController.messages, localMessages)

    init {
        playerController.connect()
    }

    /** 播放一组完整的歌（下载、最近播放、最近搜索） */
    fun playQueueSong(songs: List<Song>, index: Int, sourceTitle: String? = null) {
        playerController.playList(songs, index, listKey = null, sourceTitle = sourceTitle)
    }

    /** 播放分页列表里的歌，快播完时会自动加载列表后面的歌 */
    fun playQueueSong(listKey: SongListKey, songs: List<Song>, index: Int) {
        playerController.playList(songs, index, listKey = listKey, sourceTitle = listKey.title())
    }

    /** 播放外部打开的音频文件 */
    fun playSong(song: Song) {
        playerController.playList(listOf(song), 0, listKey = null, sourceTitle = "本地音频")
    }

    fun togglePlayPause() = playerController.togglePlayPause()

    fun pause() = playerController.pause()

    fun resume() = playerController.play()

    fun playNext() = playerController.next()

    fun playPrevious() = playerController.previous()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    /** 播放队列里的第 [index] 首（播放器队列里的下标） */
    fun playQueueIndex(index: Int) = playerController.skipTo(index)

    fun moveQueueSong(fromIndex: Int, toIndex: Int) = playerController.move(fromIndex, toIndex)

    /** 队列面板滑到底：加载来源列表后面的歌 */
    fun loadMoreQueue() = playerController.loadMore()

    fun changePlayMode(playMode: PlayMode) = playerController.setPlayMode(playMode)

    /**
     * 收藏的点击事件
     */
    fun doFavoriteEvent() {
        val song = songUiState.value
        if (!song.canFavorite || song.isFavoriteLoading) return

        favoriteLoadingIds.update { it + song.songId }
        viewModelScope.launch {
            val result = if (song.isLoved) {
                songRepository.unFavoriteSong(song.songId)
            } else {
                songRepository.favoriteSong(song.songId)
            }
            favoriteLoadingIds.update { it - song.songId }
            if (result is RepositoryWorkResult.Failure) {
                localMessages.emit(result.message)
            }
        }
    }

    fun downloadCurrentSong() {
        val song = playbackState.value.currentItem?.song ?: return
        if (song.source !is MusicSource.Remote) {
            localMessages.tryEmit("本地歌曲不需要下载")
            return
        }

        viewModelScope.launch {
            localMessages.emit("开始下载")
            when (val result = songRepository.upsertDownloadSongs(song)) {
                is RepositoryWorkResult.Success -> localMessages.emit(result.data)
                is RepositoryWorkResult.Failure -> localMessages.emit(result.message)
            }
        }
    }

    /** 歌词可能是网址，也可能是下载到本地的文件 */
    private suspend fun loadLyrics(path: String): List<LyricLine> = withContext(Dispatchers.IO) {
        runCatching {
            val text = if (path.startsWith("http")) {
                val request = Request.Builder().url(path).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            } else {
                File(path).takeIf { it.exists() }?.readText()
            }
            text?.let { LrcParser.parse(it) }.orEmpty()
        }.getOrDefault(emptyList())
    }

    /** 用 Coil 读封面（和界面共用缓存），挑出重点颜色；没有封面时用默认封面 */
    private suspend fun loadCoverColors(cover: String?): CoverColors {
        val request = ImageRequest.Builder(context)
            .data(cover ?: R.drawable.default_cover)
            .error(R.drawable.default_cover)
            .size(COLOR_SAMPLE_SIZE)
            // Palette 要读像素，不能用硬件位图
            .allowHardware(false)
            .build()
        val drawable = context.imageLoader.execute(request).drawable ?: return CoverColors.Default
        return withContext(Dispatchers.Default) {
            runCatching { extractCoverColors(drawable.toBitmap()) }.getOrDefault(CoverColors.Default)
        }
    }

    private fun SongListKey.title(): String = when (this) {
        SongListKey.Hot -> "热门歌曲"
        SongListKey.Loved -> "喜欢的歌曲"
        is SongListKey.Search -> "搜索“$keyword”"
        is SongListKey.Playlist -> "歌单"
    }

    private companion object {
        const val COLOR_SAMPLE_SIZE = 128
    }
}
