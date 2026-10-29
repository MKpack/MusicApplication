package com.example.musicapplication.domain.player

/** 界面需要的播放状态，全部来自 PlaybackService 里的播放器 */
data class PlaybackState(
    // 播放器里的队列顺序（拖动排序改的就是它）
    val queue: List<QueueItem> = emptyList(),
    // 当前歌曲在 [queue] 里的位置，没有歌时是 -1
    val currentIndex: Int = -1,
    // 实际播放顺序：[queue] 的下标按播放先后排好。不随机时就是 0..n-1，随机时是打乱后的顺序
    val playbackOrder: List<Int> = emptyList(),
    // 是否处于播放状态（缓冲中也算），播放/暂停按钮看它
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val playMode: PlayMode = PlayMode.Default,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    // 队列来源列表（热门、喜欢、搜索结果）后面还能不能接着加载
    val loadMoreState: QueueLoadMoreState = QueueLoadMoreState.Unsupported
) {
    val currentItem: QueueItem?
        get() = queue.getOrNull(currentIndex)
}

/** 队列来源列表的加载状态，队列面板滑到底时据此显示加载中、到底了或重试 */
enum class QueueLoadMoreState {
    // 队列不是来自分页列表（下载、最近播放等），已经是完整的
    Unsupported,
    // 后面可能还有，滑到底时加载
    Idle,
    Loading,
    EndReached,
    Failed
}
