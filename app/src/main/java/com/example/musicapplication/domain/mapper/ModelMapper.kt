package com.example.musicapplication.domain.mapper

import com.example.musicapplication.data.local.song.entity.SongRecentPlayEntity
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song

fun Song.toSongRecentPlayEntity(): SongRecentPlayEntity {
    val sourceType : String
    val sourceValue : String?

    when (source) {
        is MusicSource.Remote -> {
            sourceType = "remote"
            sourceValue = (source as MusicSource.Remote).url
        }
        is MusicSource.Local -> {
            sourceType = "local_uri"
            sourceValue = (source as MusicSource.Local).uri.toString()
        }

        null -> {
            sourceType = "remote"
            sourceValue = null
        }
    }

    return SongRecentPlayEntity(
        songId = songId,
        title = songTitle,
        singer = singer,
        cover = cover,
        // as? 是安全转，如果不行就返回null，as是强转，不行就抛异常了
//        source = source.let {
//            it as? MusicSource.Remote
//        }?.url,
        source = sourceValue,
        sourceType = sourceType,
        isLoved = isLoved,
        updatedAt = System.currentTimeMillis(),
        playAt = System.currentTimeMillis()
    )
}