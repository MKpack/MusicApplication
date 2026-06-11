package com.example.musicapplication.domain.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicapplication.data.local.artwork.ArtworkCacheManager
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControllerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkCacheManager: ArtworkCacheManager
) {
    private val TAG = "MediaControllerManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionToken = SessionToken(
        context,
        ComponentName(context, PlaybackService::class.java)
    )

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: kotlinx.coroutines.Job? = null
    // 歌曲或歌曲队列
    private var pendingSong: Song? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    private var pendingRestoreQueue: Triple<List<Song>, Int, Long>? = null
    private var currentQueue: List<Song> = emptyList()
    private var desiredRepeatMode: Int = Player.REPEAT_MODE_OFF

    //
    private var playbackCommandVersion = 0L

    // 当前播放的时间
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    // 歌曲时长
    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    // 是否正在播放
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // 当前播放位置
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress = _currentProgress.asStateFlow()

    // 拖动
    private val _isDragging = MutableStateFlow(false)
    val isDragging = _isDragging.asStateFlow()

    private val _playbackEndedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playbackEndedEvents = _playbackEndedEvents.asSharedFlow()

    private val _mediaItemTransitionEvents = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val mediaItemTransitionEvents = _mediaItemTransitionEvents.asSharedFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
                if (controller?.playWhenReady == false) {
                    playbackCommandVersion += 1
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> updateDuration()
                Player.STATE_ENDED -> {
                    _isPlaying.value = false
                    resetProgress()
                    stopProgressUpdate()
                    _playbackEndedEvents.tryEmit(Unit)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val currentIndex = controller?.currentMediaItemIndex ?: return
            if (currentIndex >= 0) {
                _mediaItemTransitionEvents.tryEmit(currentIndex)
                preloadNextArtwork(currentIndex)
            }
        }
    }

    fun connect() {
        if (controller != null || controllerFuture != null) return

        // 这是个异步过程，这里完成callback下方的listener
        val future = MediaController.Builder(context, sessionToken)
            .buildAsync()
        controllerFuture = future

        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController.repeatMode = desiredRepeatMode
                    mediaController.addListener(playerListener)
                    syncFromController(mediaController)

                    pendingRestoreQueue?.let { (songs, index, positionMs) ->
                        pendingRestoreQueue = null
                        restoreQueue(songs, index, positionMs)
                        return@addListener
                    }

                    pendingQueue?.let { (songs, index) ->
                        pendingQueue = null
                        playQueue(songs, index)
                        return@addListener
                    }

                    pendingSong?.let { song ->
                        pendingSong = null
                        playSong(song)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "connect MediaController failed", e)
                    controllerFuture = null
                }
            },
            context.mainExecutor
        )
    }

    fun playSong(song: Song) {
        val mediaController = controller
        if (mediaController == null) {
            pendingSong = song
            pendingQueue = null
            connect()
            return
        }

        val commandVersion = ++playbackCommandVersion
        currentQueue = listOf(song)

        scope.launch {
            val artworkUri = artworkCacheManager.getOrDownloadArtworkUri(song)
            val mediaItem = song.toMediaItem(artworkUri) ?: return@launch
            if (commandVersion != playbackCommandVersion) return@launch

            withContext(Dispatchers.Main) {
                if (commandVersion != playbackCommandVersion) return@withContext
                resetProgress()
                mediaController.setMediaItem(mediaItem)
                mediaController.prepare()
                mediaController.play()
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        if (startIndex !in songs.indices) return

        val mediaController = controller
        if (mediaController == null) {
            pendingQueue = songs to startIndex
            pendingSong = null
            connect()
            return
        }

        val commandVersion = ++playbackCommandVersion
        currentQueue = songs

        scope.launch {
            val mediaItems = songs.mapIndexedNotNull { index, song ->
                val artworkUri = if (index == startIndex) {
                    artworkCacheManager.getOrDownloadArtworkUri(song)
                } else {
                    artworkCacheManager.getCachedArtworkUri(song)
                }
                song.toMediaItem(artworkUri)
            }
            if (mediaItems.isEmpty()) return@launch
            if (commandVersion != playbackCommandVersion) return@launch

            withContext(Dispatchers.Main) {
                if (commandVersion != playbackCommandVersion) return@withContext
                resetProgress()
                mediaController.setMediaItems(mediaItems, startIndex, 0L)
                mediaController.prepare()
                mediaController.play()
            }

            preloadNextArtwork(startIndex)
        }
    }

    fun restoreQueue(songs: List<Song>, startIndex: Int, positionMs: Long) {
        if (songs.isEmpty()) return
        if (startIndex !in songs.indices) return

        val mediaController = controller
        if (mediaController == null) {
            pendingRestoreQueue = Triple(songs, startIndex, positionMs)
            pendingQueue = null
            pendingSong = null
            connect()
            return
        }

        val commandVersion = ++playbackCommandVersion
        currentQueue = songs

        scope.launch {
            val mediaItems = songs.mapIndexedNotNull { index, song ->
                val artworkUri = if (index == startIndex) {
                    artworkCacheManager.getOrDownloadArtworkUri(song)
                } else {
                    artworkCacheManager.getCachedArtworkUri(song)
                }
                song.toMediaItem(artworkUri)
            }
            if (mediaItems.isEmpty()) return@launch
            if (commandVersion != playbackCommandVersion) return@launch

            withContext(Dispatchers.Main) {
                if (commandVersion != playbackCommandVersion) return@withContext
                mediaController.setMediaItems(
                    mediaItems,
                    startIndex,
                    positionMs.coerceAtLeast(0L)
                )
                mediaController.prepare()
                mediaController.pause()
                _isPlaying.value = false
                _currentPosition.value = (positionMs / 1000L).toInt()
            }

            preloadNextArtwork(startIndex)
        }
    }

    fun playQueueIndex(index: Int) {
        val mediaController = controller ?: return
        if (index < 0 || index >= mediaController.mediaItemCount) return

        scope.launch {
            ensureArtworkForIndexNow(index)
            withContext(Dispatchers.Main) {
                mediaController.seekToDefaultPosition(index)
                mediaController.play()
            }
            ensureArtworkForIndexNow(index)
            preloadNextArtwork(index)
        }
    }

    fun updateQueue(songs: List<Song>, currentIndex: Int) {
        if (songs.isEmpty()) return
        if (currentIndex !in songs.indices) return

        val mediaController = controller
        if (mediaController == null) {
            pendingQueue = songs to currentIndex
            pendingSong = null
            connect()
            return
        }

        val commandVersion = ++playbackCommandVersion
        currentQueue = songs

        scope.launch {
            val currentPositionMs = withContext(Dispatchers.Main) {
                mediaController.currentPosition
            }
            val shouldKeepPlaying = withContext(Dispatchers.Main) {
                mediaController.isPlaying || mediaController.playWhenReady
            }
            val mediaItems = songs.mapIndexedNotNull { index, song ->
                val artworkUri = if (index == currentIndex) {
                    artworkCacheManager.getOrDownloadArtworkUri(song)
                } else {
                    artworkCacheManager.getCachedArtworkUri(song)
                }
                song.toMediaItem(artworkUri)
            }
            if (mediaItems.isEmpty()) return@launch
            if (commandVersion != playbackCommandVersion) return@launch

            withContext(Dispatchers.Main) {
                if (commandVersion != playbackCommandVersion) return@withContext
                mediaController.playWhenReady = shouldKeepPlaying
                mediaController.setMediaItems(mediaItems, currentIndex, currentPositionMs)
                mediaController.prepare()
                if (shouldKeepPlaying) {
                    mediaController.play()
                }
            }

            preloadNextArtwork(currentIndex)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int, songs: List<Song>, currentIndex: Int) {
        if (songs.isEmpty()) return
        if (fromIndex !in songs.indices) return
        if (toIndex !in songs.indices) return
        if (currentIndex !in songs.indices) return

        val mediaController = controller
        if (mediaController == null) {
            pendingQueue = songs to currentIndex
            pendingSong = null
            connect()
            return
        }

        currentQueue = songs
        scope.launch {
            withContext(Dispatchers.Main) {
                if (fromIndex >= mediaController.mediaItemCount) return@withContext
                mediaController.moveMediaItem(fromIndex, toIndex)
            }
        }
        ensureArtworkForIndex(currentIndex)
        preloadNextArtwork(currentIndex)
    }

    fun reorderQueueByMoves(
        moves: List<Pair<Int, Int>>,
        songs: List<Song>,
        currentIndex: Int
    ) {
        if (songs.isEmpty()) return
        if (currentIndex !in songs.indices) return

        val mediaController = controller
        if (mediaController == null) {
            pendingQueue = songs to currentIndex
            pendingSong = null
            connect()
            return
        }

        currentQueue = songs
        scope.launch {
            withContext(Dispatchers.Main) {
                moves.forEach { (fromIndex, toIndex) ->
                    if (fromIndex in 0 until mediaController.mediaItemCount &&
                        toIndex in 0 until mediaController.mediaItemCount
                    ) {
                        mediaController.moveMediaItem(fromIndex, toIndex)
                    }
                }
            }
        }
        ensureArtworkForIndex(currentIndex)
        preloadNextArtwork(currentIndex)
    }

    fun setRepeatMode(repeatMode: Int) {
        desiredRepeatMode = repeatMode
        val mediaController = controller ?: return

        scope.launch {
            withContext(Dispatchers.Main) {
                mediaController.repeatMode = repeatMode
            }
        }
    }

    fun pause() {
        playbackCommandVersion += 1
        val mediaController = controller ?: return
        scope.launch {
            withContext(Dispatchers.Main) {
                mediaController.pause()
            }
        }
    }

    fun resume() {
        val mediaController = controller ?: return
        scope.launch {
            withContext(Dispatchers.Main) {
                mediaController.play()
            }
        }
    }

    fun pauseAtStart() {
        playbackCommandVersion += 1
        val mediaController = controller
        scope.launch {
            withContext(Dispatchers.Main) {
                mediaController?.pause()
                mediaController?.seekTo(0)
            }
        }
        _isPlaying.value = false
        resetProgress()
        stopProgressUpdate()
    }

    fun updatePosition(progress: Float) {
        val safeProgress = progress.coerceIn(0f, 1f)
        _currentProgress.value = safeProgress
        _currentPosition.value = (safeProgress * _duration.value).toInt()
    }

    fun seekTo() {
        val mediaController = controller ?: return
        scope.launch {
            withContext(Dispatchers.Main) {
                mediaController.seekTo((_currentPosition.value * 1000L))
            }
        }
    }

    fun isDraggingStatusChange(isDragging: Boolean) {
        _isDragging.value = isDragging
    }

    fun release() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        pendingSong = null
        pendingQueue = null
        pendingRestoreQueue = null
        currentQueue = emptyList()
        stopProgressUpdate()
        scope.cancel()
    }

    // 重连时需要的sync同步
    private fun syncFromController(mediaController: MediaController) {
        _isPlaying.value = mediaController.isPlaying
        updateDuration()
        scope.launch {
            updateProgressFromController()
        }
        if (mediaController.isPlaying) {
            startProgressUpdate()
        }
    }

    private fun updateDuration() {
        val durationMs = controller?.duration ?: 0L
        _duration.value = if (durationMs > 0) {
            (durationMs / 1000).toInt()
        } else {
            0
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                if (_isDragging.value) {
                    delay(100)
                    continue
                }

                updateProgressFromController()
                delay(500)
            }
        }
    }

    private suspend fun updateProgressFromController() {
        val positionMs = withContext(Dispatchers.Main) {
            controller?.currentPosition ?: 0L
        }
        _currentPosition.value = (positionMs / 1000).toInt()
        _currentProgress.value = if (_duration.value > 0) {
            _currentPosition.value.toFloat() / _duration.value.toFloat()
        } else {
            0f
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun resetProgress() {
        _currentProgress.value = 0f
        _currentPosition.value = 0
        _duration.value = 0
    }

    private fun Song.toMediaItem(artworkUri: Uri?): MediaItem? {
        val uri = when (val musicSource = source) {
            is MusicSource.Remote -> musicSource.url.toUri()
            is MusicSource.Local -> musicSource.uri
            null -> return null
        }

        return MediaItem.Builder()
            .setMediaId(songId.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(songTitle)
                    .setArtist(singer)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()
    }

    private fun preloadNextArtwork(currentIndex: Int) {
        val nextIndex = currentIndex + 1
        ensureArtworkForIndex(nextIndex)
    }

    private fun ensureArtworkForIndex(index: Int) {
        scope.launch {
            ensureArtworkForIndexNow(index)
        }
    }

    private suspend fun ensureArtworkForIndexNow(index: Int) {
        val song = currentQueue.getOrNull(index) ?: return
        val artworkUri = artworkCacheManager.getOrDownloadArtworkUri(song) ?: return
        val mediaItem = song.toMediaItem(artworkUri) ?: return

        withContext(Dispatchers.Main) {
            val mediaController = controller ?: return@withContext
            if (index !in 0 until mediaController.mediaItemCount) return@withContext
            if (currentQueue.getOrNull(index)?.songId != song.songId) return@withContext

            mediaController.replaceMediaItem(index, mediaItem)
        }
    }
}
