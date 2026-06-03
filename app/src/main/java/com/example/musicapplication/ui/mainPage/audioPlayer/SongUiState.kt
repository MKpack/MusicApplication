package com.example.musicapplication.ui.mainPage.audioPlayer

import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song

data class SongUiState(
    val songId: Long = -1,
    val songTitle: String = "未知歌曲",
    val singer: String = "未知歌手",
    val cover: String? = null,
    val isLoved: Boolean = false,
    val canFavorite: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val errorMessage: String? = null
)

fun Song.toSongUiState(
    isFavoriteLoading: Boolean = false,
    errorMessage: String? = null
): SongUiState {
    return SongUiState(
        songId = songId,
        songTitle = songTitle,
        singer = singer,
        cover = cover,
        isLoved = isLoved,
        canFavorite = source is MusicSource.Remote && songId > 0,
        isFavoriteLoading = isFavoriteLoading,
        errorMessage = errorMessage
    )
}
