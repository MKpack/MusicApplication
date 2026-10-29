package com.example.musicapplication.playback

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.player.PlayerSnapshot
import com.example.musicapplication.data.local.player.PlayerStateStore
import com.example.musicapplication.data.local.player.toPlayerSnapshotSong
import com.example.musicapplication.data.local.player.toQueueItem
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.data.repository.UserStatRepository
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.SongListKey
import com.example.musicapplication.domain.player.PlayMode
import com.example.musicapplication.domain.player.QueueLoadMoreState
import com.example.musicapplication.domain.player.QueueItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/** App 和播放服务之间的自定义命令 */
object PlaybackCommands {
    // Service → App：播放出错、跳过歌曲时给用户的提示，App 收到后弹 Toast
    const val ACTION_MESSAGE = "com.example.musicapplication.playback.MESSAGE"
    const val KEY_MESSAGE = "message"
    val MESSAGE = SessionCommand(ACTION_MESSAGE, Bundle.EMPTY)

    // App → Service：队列面板滑到底了，加载来源列表的下一页接到队列后面
    const val ACTION_LOAD_MORE = "com.example.musicapplication.playback.LOAD_MORE"
    val LOAD_MORE = SessionCommand(ACTION_LOAD_MORE, Bundle.EMPTY)

    // 加载状态（QueueLoadMoreState 的名字）放在 session extras 里，App 从这里读
    const val EXTRA_LOAD_MORE_STATE = "load_more_state"
}

/**
 * 播放器旁边的「管家」，跟着 PlaybackService 创建和销毁。不管歌是从 App 里、通知栏还是耳机线控切的，都由它统一处理：
 * - 歌曲真正开始播放时记一次最近播放和播放次数（加载失败的不算，单曲循环重播不重复记）；
 * - 保存播放队列和进度，服务启动时恢复上次的队列（暂停状态）；
 * - 分页列表（热门、喜欢、搜索结果）快播完时自动加载后面的歌接到队列后面；
 * - 随机播放时让当前歌曲排在随机顺序的第一位，这一轮把其他歌都放完再循环；
 * - 播放出错：断网就暂停并提示，歌曲本身放不了就跳到下一首，连续失败 3 首停止。
 */
@OptIn(UnstableApi::class)
class PlaybackCoordinator @Inject constructor(
    private val songRepository: SongRepository,
    private val userStatRepository: UserStatRepository,
    private val playerStateStore: PlayerStateStore,
    private val resolver: PlaybackItemResolver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 写快照、记播放次数不能跟着服务一起取消，否则服务销毁前的最后一次保存可能写不完
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 快照按顺序写，只保留最新的一份；null 表示清空
    private val snapshotWrites = Channel<PlayerSnapshot?>(Channel.CONFLATED)

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    // 当前歌曲还没有记录播放，开始播放时记一次
    private var unrecordedItemId: String? = null
    private var consecutiveFailures = 0

    // 恢复上次的队列完成之前不能保存：这时播放器是空的，一保存就把上次的快照清掉了
    private var restored = false
    private var persistJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var loadMoreJob: Job? = null

    // 当前队列来源列表的加载状态；换了来源列表（播放了新的列表）就重新开始
    private var loadMoreListKey: String? = null
    private var loadMoreState = QueueLoadMoreState.Unsupported

    init {
        ioScope.launch {
            for (snapshot in snapshotWrites) {
                runCatching {
                    if (snapshot == null) playerStateStore.clearSnapshot() else playerStateStore.saveSnapshot(snapshot)
                }.onFailure { Log.e(TAG, "save player snapshot failed", it) }
            }
        }
    }

    fun attach(player: ExoPlayer, session: MediaSession) {
        this.player = player
        this.session = session
        player.repeatMode = PlayMode.Default.repeatMode
        player.addListener(listener)
        scope.launch {
            runCatching { restore(player) }.onFailure { Log.e(TAG, "restore player failed", it) }
            restored = true
            // 恢复期间发生的变化（比如用户已经点了别的歌）这时才能保存
            maybeLoadMore()
            schedulePersist()
        }
    }

    fun detach() {
        persistNow()
        snapshotWrites.close()
        player?.removeListener(listener)
        scope.cancel()
        player = null
        session = null
    }

    /** 立刻保存当前队列和进度（App 被划掉、服务销毁时调用） */
    fun persistNow() {
        val player = player ?: return
        if (!restored) return
        val items = player.queueItems()
        val snapshot = if (items.isEmpty()) null else PlayerSnapshot(
            songs = items.map { it.toPlayerSnapshotSong() },
            currentIndex = player.currentMediaItemIndex.coerceIn(items.indices),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            playModeName = PlayMode.from(player.repeatMode, player.shuffleModeEnabled).name
        )
        snapshotWrites.trySend(snapshot)
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 单曲循环重播同一首不算新的一次播放
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                unrecordedItemId = mediaItem?.mediaId
            }
            val player = player ?: return
            // 换了新队列：随机播放时从新点的这首开始随机
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && player.shuffleModeEnabled) {
                shuffleFromCurrent(player)
            }
            updateLoadMoreSource(mediaItem)
            maybeLoadMore()
            schedulePersist()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (timeline.isEmpty) {
                // 队列被清空（退出登录）：马上清掉快照，不等合并保存
                persistJob?.cancel()
                persistNow()
                updateLoadMoreSource(null)
                return
            }
            maybeLoadMore()
            schedulePersist()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                consecutiveFailures = 0
                startPeriodicSave()
            } else {
                stopPeriodicSave()
                schedulePersist()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            // 自动切到下一首时一直在播放，不会再收到 onIsPlayingChanged，所以每批事件之后都检查一次
            if (player.isPlaying) recordIfNeeded()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // 用户自己点了播放，重新开始计算连续失败
            if (playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                consecutiveFailures = 0
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // 顺序播放放完最后一首：停在这首的开头，和点暂停一样
            if (playbackState == Player.STATE_ENDED) {
                player?.run {
                    pause()
                    seekTo(0L)
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            player?.let { if (shuffleModeEnabled) shuffleFromCurrent(it) }
            maybeLoadMore()
            schedulePersist()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            maybeLoadMore()
            schedulePersist()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) schedulePersist()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "onPlayerError: ${error.errorCodeName}", error)
            handleError(error)
        }
    }

    private suspend fun restore(player: ExoPlayer) {
        val snapshot = playerStateStore.snapshotFlow.first() ?: return
        val items = if (snapshot.songs.isNotEmpty()) {
            snapshot.songs.map { it.toQueueItem() }
        } else {
            songRepository.observeSongsByIds(snapshot.songIds).first()
                .map { QueueItem(id = newQueueItemId(), song = it) }
        }
        if (items.isEmpty()) return

        val resolved = resolver.resolve(items.map { it.song })
        // 恢复的时候用户已经点了别的歌，以用户的为准
        if (player.mediaItemCount > 0) return

        // 没有播放地址的歌放不进播放器，跳过它们，起始位置跟着调整
        val mediaItems = items.mapIndexedNotNull { index, item ->
            resolved[index].toMediaItem(item.id, item.listKey, item.sourceTitle)?.let { index to it }
        }
        if (mediaItems.isEmpty()) return
        val startIndex = mediaItems.indexOfFirst { it.first >= snapshot.currentIndex }.takeIf { it >= 0 } ?: 0
        val startPosition = if (mediaItems[startIndex].first == snapshot.currentIndex) snapshot.positionMs else 0L
        val playMode = PlayMode.fromName(snapshot.playModeName)

        player.setMediaItems(mediaItems.map { it.second }, startIndex, startPosition)
        player.repeatMode = playMode.repeatMode
        player.shuffleModeEnabled = playMode.shuffleEnabled
        // 只准备好，不自动播放
        player.prepare()
    }

    private fun recordIfNeeded() {
        val item = player?.currentMediaItem ?: return
        if (item.mediaId != unrecordedItemId) return
        unrecordedItemId = null

        val song = item.toQueueItem().song
        ioScope.launch {
            runCatching { songRepository.addRecentPlay(song) }
                .onFailure { Log.e(TAG, "addRecentPlay failed", it) }
            if (song.source is MusicSource.Remote && song.songId > 0) {
                songRepository.increaseSongPlayCount(song.songId)
            }
            userStatRepository.increaseUserStatPlayCount()
        }
    }

    private fun handleError(error: PlaybackException) {
        val player = player ?: return
        // 暂停状态下加载失败（比如断网时恢复上次的队列）不打扰用户，等用户点播放时再重试
        if (!player.playWhenReady) return

        val title = player.currentMediaItem?.mediaMetadata?.title ?: "这首歌"
        when (classifyPlaybackError(error.errorCode)) {
            PlaybackErrorKind.Network -> {
                player.pause()
                sendMessage("网络连接失败，请检查网络后重试")
            }

            PlaybackErrorKind.Other -> {
                player.pause()
                sendMessage("播放出错了，请稍后重试")
            }

            PlaybackErrorKind.Source -> {
                consecutiveFailures++
                val nextIndex = player.nextMediaItemIndex
                when {
                    consecutiveFailures >= MAX_CONSECUTIVE_FAILURES -> {
                        player.pause()
                        sendMessage("连续 $MAX_CONSECUTIVE_FAILURES 首歌曲无法播放，已暂停")
                    }

                    nextIndex == C.INDEX_UNSET || nextIndex == player.currentMediaItemIndex -> {
                        player.pause()
                        sendMessage("无法播放「$title」")
                    }

                    else -> {
                        sendMessage("无法播放「$title」，已跳到下一首")
                        player.seekToDefaultPosition(nextIndex)
                        player.prepare()
                    }
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        session?.broadcastCustomCommand(
            PlaybackCommands.MESSAGE,
            bundleOf(PlaybackCommands.KEY_MESSAGE to message)
        )
    }

    /** 随机顺序重新排：当前歌曲排第一，其余随机。这样这一轮会把其他歌都放一遍 */
    private fun shuffleFromCurrent(player: ExoPlayer) {
        val count = player.mediaItemCount
        if (count <= 1) return
        val current = player.currentMediaItemIndex
        val order = listOf(current) + (0 until count).filter { it != current }.shuffled()
        player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), Random.nextLong()))
    }

    /** App 的队列面板滑到底了：不管还剩几首，都加载来源列表的下一页 */
    fun loadMore() = maybeLoadMore(userRequested = true)

    /**
     * 当前队列来自分页列表时，把列表后面的歌接到队列后面：先用本地已经加载、但还不在队列里的歌，
     * 没有了再从服务器加载下一页。
     * 自动续播在按实际播放顺序快播完（后面不超过 2 首）时触发；[userRequested] 是队列面板滑到底时触发。
     */
    private fun maybeLoadMore(userRequested: Boolean = false) {
        val player = player ?: return
        if (!restored || loadMoreJob?.isActive == true) return
        val current = player.currentMediaItem?.toQueueItem() ?: return
        val listKeyValue = current.listKey ?: return
        val listKey = SongListKey.fromValue(listKeyValue) ?: return
        if (!userRequested) {
            if (loadMoreState == QueueLoadMoreState.EndReached) return
            val timeline = player.currentTimeline
            val upcoming = countUpcoming(player.currentMediaItemIndex, LOAD_MORE_THRESHOLD + 1) { index ->
                timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
            }
            if (upcoming > LOAD_MORE_THRESHOLD) return
        }

        loadMoreJob = scope.launch {
            setLoadMoreState(QueueLoadMoreState.Loading)
            val queued = player.queueItems().map { it.song.songId }.toSet()
            var fresh = songRepository.getListSongs(listKey).filter { it.songId !in queued }
            // 本地还有没接上的歌时不知道服务器上还有没有，按「还能加载」算
            var endReached = false
            if (fresh.isEmpty()) {
                // 列表已经到底时这里不会访问网络
                when (val result = songRepository.loadMoreSongs(listKey)) {
                    is RepositoryWorkResult.Failure -> {
                        setLoadMoreState(QueueLoadMoreState.Failed)
                        return@launch
                    }

                    is RepositoryWorkResult.Success -> endReached = result.data.isEndReached
                }
                fresh = songRepository.getListSongs(listKey).filter { it.songId !in queued }
            }
            val resolved = resolver.resolve(fresh)

            // 加载期间换了队列，就不接了
            if (player.currentMediaItem?.toQueueItem()?.listKey != listKeyValue) return@launch
            val stillQueued = player.queueItems().map { it.song.songId }.toSet()
            val mediaItems = resolved
                .filter { it.song.songId !in stillQueued }
                .mapNotNull { it.toMediaItem(listKey = listKeyValue, sourceTitle = current.sourceTitle) }
            if (mediaItems.isNotEmpty()) {
                val oldCount = player.mediaItemCount
                player.addMediaItems(mediaItems)
                if (player.shuffleModeEnabled) appendToShuffleOrder(player, oldCount)
            }
            setLoadMoreState(if (endReached) QueueLoadMoreState.EndReached else QueueLoadMoreState.Idle)
        }
    }

    /** 当前歌曲换了来源列表（播放了新的列表、队列被清空）时，加载状态重新开始 */
    private fun updateLoadMoreSource(mediaItem: MediaItem?) {
        val listKey = mediaItem?.toQueueItem()?.listKey?.takeIf { SongListKey.fromValue(it) != null }
        if (listKey == loadMoreListKey) return
        loadMoreListKey = listKey
        loadMoreJob?.cancel()
        setLoadMoreState(if (listKey == null) QueueLoadMoreState.Unsupported else QueueLoadMoreState.Idle)
    }

    private fun setLoadMoreState(state: QueueLoadMoreState) {
        if (loadMoreState == state) return
        loadMoreState = state
        session?.setSessionExtras(bundleOf(PlaybackCommands.EXTRA_LOAD_MORE_STATE to state.name))
    }

    /**
     * 播放器默认把新加的歌随机插到整个随机顺序里，可能插到已经放过的位置，要等下一轮才放到。
     * 这里改成接在随机顺序的最后（新加的几首之间再打乱），已经排好的顺序不变，队列面板上也是出现在最下面。
     */
    private fun appendToShuffleOrder(player: ExoPlayer, firstNewIndex: Int) {
        val order = player.currentTimeline.playbackOrder(true).filter { it < firstNewIndex }
        val newOrder = order + (firstNewIndex until player.mediaItemCount).shuffled()
        player.setShuffleOrder(DefaultShuffleOrder(newOrder.toIntArray(), Random.nextLong()))
    }

    /** 播放状态频繁变化（拖动进度、连续切歌）时合并成一次保存 */
    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            persistNow()
        }
    }

    private fun startPeriodicSave() {
        if (periodicSaveJob?.isActive == true) return
        periodicSaveJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_SAVE_MS)
                persistNow()
            }
        }
    }

    private fun stopPeriodicSave() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
    }

    private companion object {
        const val TAG = "PlaybackCoordinator"
        const val MAX_CONSECUTIVE_FAILURES = 3
        const val LOAD_MORE_THRESHOLD = 2
        const val PERSIST_DEBOUNCE_MS = 500L
        const val PERIODIC_SAVE_MS = 15_000L
    }
}

/**
 * 从 [current] 往后数还有几首（最多数到 [limit]），[next] 返回下一首的下标，没有了返回 [C.INDEX_UNSET]。
 */
internal fun countUpcoming(current: Int, limit: Int, next: (Int) -> Int): Int {
    var count = 0
    var index = next(current)
    while (index != C.INDEX_UNSET && count < limit) {
        count++
        index = next(index)
    }
    return count
}
