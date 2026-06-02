package com.example.musicapplication.data.local.song.mapper

import android.net.Uri
import com.example.musicapplication.data.local.song.entity.SongEntity
import com.example.musicapplication.data.local.song.entity.SongRecentPlayEntity
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.utils.MediaUrlUtils
import androidx.core.net.toUri

fun SongEntity.toSong(): Song {
    return Song(
        songId = songId,
        songTitle = title,
        singer = singer,
        cover = MediaUrlUtils.buildMediaUrl(cover),
        isLoved = isLoved,
        source = MediaUrlUtils.buildMediaUrl(audioUrl)?.let { MusicSource.Remote(songId, it) }
    )
}


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