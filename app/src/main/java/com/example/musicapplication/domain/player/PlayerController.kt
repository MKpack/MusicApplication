package com.example.musicapplication.domain.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.example.musicapplication.data.local.player.PlayerStateStore
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import com.example.musicapplication.playback.PlaybackCommands
import com.example.musicapplication.playback.PlaybackItemResolver
import com.example.musicapplication.playback.PlaybackService
import com.example.musicapplication.playback.playbackOrder
import com.example.musicapplication.playback.queueItems
import com.example.musicapplication.playback.toMediaItem
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App 这一侧控制播放的唯一入口：连接 PlaybackService，把播放器的状态整理成 [PlaybackState] 给界面，
 * 界面的操作转成对播放器的命令。队列只存在播放器里，这里不另外维护一份。
 * 所有方法都在主线程调用。
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolver: PlaybackItemResolver,
    private val playerStateStore: PlayerStateStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))

    private var controller: MediaController? = null
    private var connection: CompletableDeferred<MediaController?>? = null
    private var playJob: Job? = null

    // 队列只在播放器的队列变化时重新整理，播放进度、播放状态变化时直接复用
    private var queue: List<QueueItem> = emptyList()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    // 播放出错、跳过歌曲等需要告诉用户的提示
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 当前播放进度（毫秒），有界面在收集时每 200ms 取一次 */
    val positionMs: Flow<Long> = flow {
        while (true) {
            emit(controller?.currentPosition?.coerceAtLeast(0L) ?: 0L)
            delay(POSITION_INTERVAL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Main.immediate)

    /** 连接播放服务，已经连上或正在连时什么都不做 */
    fun connect() {
        if (connection != null) return
        val deferred = CompletableDeferred<MediaController?>()
        connection = deferred

        val future = MediaController.Builder(context, sessionToken)
            .setListener(controllerListener)
            .buildAsync()
        future.addListener(
            {
                val mediaController = try {
                    future.get()
                } catch (e: Exception) {
                    Log.e(TAG, "connect PlaybackService failed", e)
                    null
                }
                if (mediaController == null) {
                    // 下次有操作时重新连接
                    connection = null
                } else {
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    updateState(mediaController, queueChanged = true)
                }
                deferred.complete(mediaController)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    /**
     * 用一组歌替换播放队列，从 [startIndex] 开始播放。
     * [listKey] 是分页列表时，快播完会自动加载列表后面的歌；[sourceTitle] 显示在队列面板的「来自 xxx」。
     */
    fun playList(
        songs: List<Song>,
        startIndex: Int,
        listKey: SongListKey? = null,
        sourceTitle: String? = null
    ) {
        if (startIndex !in songs.indices) return
        // 连续点了几首歌，只播最后点的那首
        playJob?.cancel()
        playJob = scope.launch {
            val controller = awaitController() ?: return@launch
            val resolved = resolver.resolve(songs)
            // 没有播放地址的歌放不进播放器，跳过它们
            val mediaItems = resolved.mapIndexedNotNull { index, song ->
                song.toMediaItem(listKey = listKey?.value, sourceTitle = sourceTitle)?.let { index to it }
            }
            val start = mediaItems.indexOfFirst { it.first == startIndex }
            if (start < 0) {
                _messages.tryEmit("这首歌暂时无法播放")
                return@launch
            }
            controller.setMediaItems(mediaItems.map { it.second }, start, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() = withController { Util.handlePlayPauseButtonAction(it) }

    fun play() = withController { Util.handlePlayButtonAction(it) }

    fun pause() = withController { it.pause() }

    fun next() = withController {
        if (!it.hasNextMediaItem()) return@withController
        it.seekToNextMediaItem()
        it.startPlaying()
    }

    /** 播放超过 3 秒时回到这首的开头，否则上一首 */
    fun previous() = withController {
        it.seekToPrevious()
        it.startPlaying()
    }

    fun seekTo(positionMs: Long) = withController {
        it.seekTo(positionMs.coerceAtLeast(0L))
    }

    /** 播放队列里的第 [index] 首（播放器队列里的下标） */
    fun skipTo(index: Int) = withController {
        if (index !in 0 until it.mediaItemCount) return@withController
        it.seekToDefaultPosition(index)
        it.startPlaying()
    }

    fun move(fromIndex: Int, toIndex: Int) = withController {
        val count = it.mediaItemCount
        if (fromIndex !in 0 until count || toIndex !in 0 until count || fromIndex == toIndex) return@withController
        it.moveMediaItem(fromIndex, toIndex)
    }

    /** 队列面板滑到底了：让播放服务加载来源列表的下一页，接到队列后面 */
    fun loadMore() = withController {
        if (it.isSessionCommandAvailable(PlaybackCommands.LOAD_MORE)) {
            it.sendCustomCommand(PlaybackCommands.LOAD_MORE, Bundle.EMPTY)
        }
    }

    fun setPlayMode(playMode: PlayMode) = withController {
        // 已经是随机播放时再点一次随机：重新打乱
        if (playMode == PlayMode.Shuffle && it.shuffleModeEnabled) {
            it.shuffleModeEnabled = false
        }
        it.repeatMode = playMode.repeatMode
        it.shuffleModeEnabled = playMode.shuffleEnabled
    }

    /** 停止播放并清空队列和保存的快照（退出登录时用） */
    fun clear() {
        playJob?.cancel()
        scope.launch {
            runCatching { playerStateStore.clearSnapshot() }
        }
        controller?.run {
            stop()
            clearMediaItems()
        }
    }

    private fun withController(action: (MediaController) -> Unit) {
        val connected = controller
        if (connected != null) {
            action(connected)
            return
        }
        scope.launch { awaitController()?.let(action) }
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        connect()
        return connection?.await()
    }

    /** 出错停下（IDLE）的播放器要先重新准备才能播 */
    private fun Player.startPlaying() {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }

    private fun updateState(player: Player, queueChanged: Boolean) {
        if (queueChanged || queue.size != player.mediaItemCount) {
            queue = player.queueItems()
        }
        _state.value = PlaybackState(
            queue = queue,
            currentIndex = if (queue.isEmpty()) -1 else player.currentMediaItemIndex,
            playbackOrder = player.currentTimeline.playbackOrder(player.shuffleModeEnabled),
            isPlaying = !Util.shouldShowPlayButton(player),
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            playMode = PlayMode.from(player.repeatMode, player.shuffleModeEnabled),
            hasNext = player.hasNextMediaItem(),
            hasPrevious = queue.isNotEmpty(),
            loadMoreState = loadMoreState()
        )
    }

    private fun loadMoreState(): QueueLoadMoreState {
        val name = controller?.sessionExtras?.getString(PlaybackCommands.EXTRA_LOAD_MORE_STATE)
        return QueueLoadMoreState.entries.firstOrNull { it.name == name } ?: QueueLoadMoreState.Unsupported
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player, queueChanged = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (command.customAction == PlaybackCommands.ACTION_MESSAGE) {
                args.getString(PlaybackCommands.KEY_MESSAGE)?.let { _messages.tryEmit(it) }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(controller, command, args)
        }

        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            // 加载更多的状态变了
            updateState(controller, queueChanged = false)
        }

        override fun onDisconnected(controller: MediaController) {
            // 播放服务被销毁了，下次有操作时重新连接（服务重建时会恢复上次的队列）
            this@PlayerController.controller = null
            connection = null
            queue = emptyList()
            _state.value = PlaybackState()
        }
    }

    private companion object {
        const val TAG = "PlayerController"
        const val POSITION_INTERVAL_MS = 200L
    }
}
