package com.example.musicapplication.data.local.player

import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.player.QueueItem
import java.util.UUID

/** 上次的播放队列、播到哪里、播放模式，下次打开 App 时恢复（恢复后是暂停的） */
data class PlayerSnapshot(
    val songs: List<PlayerSnapshotSong> = emptyList(),
    // 很早的版本只存了 songId
    val songIds: List<Long> = emptyList(),
    val currentIndex: Int,
    val positionMs: Long,
    val playModeName: String
)

data class PlayerSnapshotSong(
    val songId: Long,
    val title: String,
    val singer: String,
    val isLoved: Boolean,
    val cover: String?,
    val lyric: String?,
    val sourceType: String?,
    val sourceValue: String?,
    // 下面三个是后来加的，旧快照里没有，读出来是 null
    val queueItemId: String? = null,
    val listKey: String? = null,
    val sourceTitle: String? = null
)

fun QueueItem.toPlayerSnapshotSong(): PlayerSnapshotSong {
    return PlayerSnapshotSong(
        songId = song.songId,
        title = song.songTitle,
        singer = song.singer,
        isLoved = song.isLoved,
        cover = song.cover,
        lyric = song.lyric,
        sourceType = song.source?.type,
        sourceValue = song.source?.value,
        queueItemId = id,
        listKey = listKey,
        sourceTitle = sourceTitle
    )
}

fun PlayerSnapshotSong.toSong(): Song {
    return Song(
        songId = songId,
        songTitle = title,
        singer = singer,
        isLoved = isLoved,
        cover = cover,
        lyric = lyric,
        source = MusicSource.from(songId, sourceType, sourceValue)
    )
}

fun PlayerSnapshotSong.toQueueItem(): QueueItem {
    return QueueItem(
        id = queueItemId ?: UUID.randomUUID().toString(),
        song = toSong(),
        listKey = listKey,
        sourceTitle = sourceTitle
    )
}
