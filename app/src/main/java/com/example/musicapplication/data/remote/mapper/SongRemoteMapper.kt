package com.example.musicapplication.data.remote.mapper

import com.example.musicapplication.data.local.song.entity.SongEntity
import com.example.musicapplication.data.remote.dto.response.SongResponse


// SongResponse ---> songEntity
fun SongResponse.toEntity() : SongEntity {
    return SongEntity(
        songId = songId,
        title = title,
        singer = artistName,
        cover = coverUrl,
        audioUrl = audioUrl,
        isLoved = isUserFavorite,
        updatedAt = System.currentTimeMillis()
    )
}

// TODO
fun SongResponse.toSong() {

}