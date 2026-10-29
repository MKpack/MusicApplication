package com.example.musicapplication.domain.player

import com.example.musicapplication.domain.model.Song

/**
 * 播放队列里的一项。同一首歌可以在队列里出现多次（比如最近播放），所以用 [id] 区分每一项，
 * 它也是播放器里 MediaItem 的 mediaId。
 */
data class QueueItem(
    val id: String,
    // 歌曲本身的信息：远程地址、远程封面、远程歌词。下载过的歌播放时用本地文件，但这里存的还是远程的，
    // 这样下载被删掉以后，最近播放、恢复播放仍然能用
    val song: Song,
    // 从哪个分页列表开始播放的（SongListKey.value）。快播完时按它继续加载后面的歌
    val listKey: String? = null,
    // 队列面板上显示的「来自 xxx」
    val sourceTitle: String? = null,
    // 界面显示用的封面：下载过就是本地文件，否则是远程地址
    val artworkUri: String? = null,
    // 下载过的歌的本地歌词文件
    val localLyricPath: String? = null
)
