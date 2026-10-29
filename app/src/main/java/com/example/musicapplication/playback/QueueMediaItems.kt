package com.example.musicapplication.playback

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.player.QueueItem
import java.util.UUID

/**
 * 播放队列里的歌和播放器的 MediaItem 互相转换。
 * 歌曲信息放在 MediaMetadata 的 extras 里，App 和通知栏看到的都是播放器里的这一份队列。
 */
private object Extras {
    const val SONG_ID = "song_id"
    const val COVER = "song_cover"
    const val LYRIC = "song_lyric"
    const val LOVED = "song_loved"
    const val SOURCE_TYPE = "source_type"
    const val SOURCE_VALUE = "source_value"
    const val LIST_KEY = "list_key"
    const val SOURCE_TITLE = "source_title"
    const val LOCAL_LYRIC = "local_lyric"
}

fun newQueueItemId(): String = UUID.randomUUID().toString()

/** 没有可播放地址的歌返回 null，不能放进播放器 */
fun ResolvedSong.toMediaItem(
    id: String = newQueueItemId(),
    listKey: String? = null,
    sourceTitle: String? = null
): MediaItem? {
    val uri = playbackUri ?: return null
    val extras = Bundle().apply {
        putLong(Extras.SONG_ID, song.songId)
        putString(Extras.COVER, song.cover)
        putString(Extras.LYRIC, song.lyric)
        putBoolean(Extras.LOVED, song.isLoved)
        putString(Extras.SOURCE_TYPE, song.source?.type)
        putString(Extras.SOURCE_VALUE, song.source?.value)
        putString(Extras.LIST_KEY, listKey)
        putString(Extras.SOURCE_TITLE, sourceTitle)
        putString(Extras.LOCAL_LYRIC, localLyricPath)
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.songTitle)
                .setArtist(song.singer)
                .setArtworkUri(artworkUri)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setExtras(extras)
                .build()
        )
        .build()
}

fun MediaItem.toQueueItem(): QueueItem {
    val metadata = mediaMetadata
    val extras = metadata.extras ?: Bundle.EMPTY
    val songId = extras.getLong(Extras.SONG_ID, -1L)
    return QueueItem(
        id = mediaId,
        song = Song(
            songId = songId,
            songTitle = metadata.title?.toString() ?: "未知歌曲",
            singer = metadata.artist?.toString() ?: "未知歌手",
            isLoved = extras.getBoolean(Extras.LOVED, false),
            cover = extras.getString(Extras.COVER),
            lyric = extras.getString(Extras.LYRIC),
            source = MusicSource.from(
                songId = songId,
                type = extras.getString(Extras.SOURCE_TYPE),
                value = extras.getString(Extras.SOURCE_VALUE)
            )
        ),
        listKey = extras.getString(Extras.LIST_KEY),
        sourceTitle = extras.getString(Extras.SOURCE_TITLE),
        artworkUri = metadata.artworkUri?.toString(),
        localLyricPath = extras.getString(Extras.LOCAL_LYRIC)
    )
}

/** 播放器里的整个队列 */
fun Player.queueItems(): List<QueueItem> = List(mediaItemCount) { getMediaItemAt(it).toQueueItem() }

/**
 * 实际播放顺序（队列下标按播放先后排列）。不随机时就是 0..n-1；随机时按播放器打乱后的顺序，
 * 当前这一轮随机里还没播的歌排在当前歌曲后面。
 */
fun Timeline.playbackOrder(shuffleEnabled: Boolean): List<Int> {
    if (isEmpty) return emptyList()
    if (!shuffleEnabled) return List(windowCount) { it }
    val order = ArrayList<Int>(windowCount)
    var index = getFirstWindowIndex(true)
    while (index != C.INDEX_UNSET && order.size < windowCount) {
        order += index
        index = getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
    }
    return order
}
