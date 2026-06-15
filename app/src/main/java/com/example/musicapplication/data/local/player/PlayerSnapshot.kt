package com.example.musicapplication.data.local.player

import androidx.core.net.toUri
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song

data class PlayerSnapshot(
    val songs: List<PlayerSnapshotSong> = emptyList(),
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
    val sourceValue: String?
)

fun Song.toPlayerSnapshotSong(): PlayerSnapshotSong {
    val source = source
    val sourceType: String?
    val sourceValue: String?

    when (source) {
        is MusicSource.Remote -> {
            sourceType = "remote"
            sourceValue = source.url
        }
        is MusicSource.Local -> {
            sourceType = "local_uri"
            sourceValue = source.uri.toString()
        }
        null -> {
            sourceType = null
            sourceValue = null
        }
    }

    return PlayerSnapshotSong(
        songId = songId,
        title = songTitle,
        singer = singer,
        isLoved = isLoved,
        cover = cover,
        lyric = lyric,
        sourceType = sourceType,
        sourceValue = sourceValue
    )
}

fun PlayerSnapshotSong.toSong(): Song {
    val musicSource = when (sourceType) {
        "remote" -> sourceValue?.let { MusicSource.Remote(songId, it) }
        "local_uri" -> sourceValue?.let { MusicSource.Local(songId.toInt(), it.toUri()) }
        else -> null
    }

    return Song(
        songId = songId,
        songTitle = title,
        singer = singer,
        isLoved = isLoved,
        cover = cover,
        lyric = lyric,
        source = musicSource
    )
}
