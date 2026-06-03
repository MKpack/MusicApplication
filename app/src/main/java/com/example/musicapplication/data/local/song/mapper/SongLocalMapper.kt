package com.example.musicapplication.data.local.song.mapper

import com.example.musicapplication.data.local.song.entity.SongEntity
import com.example.musicapplication.data.local.song.entity.SongRecentPlayEntity
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import androidx.core.net.toUri

/**
 * 后端传输的歌曲实体转Song
 */
fun SongEntity.toSong(): Song {
    return Song(
        songId = songId,
        songTitle = title,
        singer = singer,
        cover = coverUrl,
        lyric = lyricUrl,
        isLoved = isLoved,
        source = audioUrl?.let { MusicSource.Remote(songId, it) }
    )
}


/**
 * 最近播放的表实体转Song
 */
fun SongRecentPlayEntity.toSong(): Song {
    val sourceValue: MusicSource? = if (sourceType == "remote" && source != null) {
        MusicSource.Remote(songId, source)
    } else if (sourceType == "local_uri" && source != null) {
        MusicSource.Local(songId.toInt(), source.toUri())
    } else {
        null
    }

    return Song(
        songId = songId,
        songTitle = title,
        singer = singer,
        isLoved = isLoved,
        cover = cover,
        source = sourceValue
    )
}