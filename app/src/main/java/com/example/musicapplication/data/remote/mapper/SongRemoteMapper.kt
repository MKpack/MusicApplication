package com.example.musicapplication.data.remote.mapper

import com.example.musicapplication.data.local.song.entity.SongEntity
import com.example.musicapplication.data.remote.dto.response.SongResponse
import com.example.musicapplication.utils.MediaUrlUtils


// SongResponse ---> songEntity
// coverUrl ---> https://.... 在这已经完成后期无需拼接了
fun SongResponse.toEntity() : SongEntity {
    return SongEntity(
        songId = songId,
        title = title,
        singer = artistName,
        coverUrl = MediaUrlUtils.buildMediaUrl(coverUrl),
        audioUrl = MediaUrlUtils.buildMediaUrl(audioUrl),
        lyricUrl = MediaUrlUtils.buildMediaUrl(lyricUrl),
        isLoved = isUserFavorite,
        updatedAt = System.currentTimeMillis()
    )
}