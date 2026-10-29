package com.example.musicapplication.ui.mainPage.audioPlayer

import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.player.QueueItem

data class SongUiState(
    val songId: Long = -1,
    val songTitle: String = "未知歌曲",
    val singer: String = "未知歌手",
    val cover: String? = null,
    val isLoved: Boolean = false,
    val canFavorite: Boolean = false,
    val isFavoriteLoading: Boolean = false
)

fun QueueItem.toSongUiState(
    isLoved: Boolean,
    isFavoriteLoading: Boolean
): SongUiState {
    return SongUiState(
        songId = song.songId,
        songTitle = song.songTitle,
        singer = song.singer,
        // 下载过的歌显示本地封面，断网也能看到
        cover = artworkUri ?: song.cover,
        isLoved = isLoved,
        canFavorite = song.source is MusicSource.Remote && song.songId > 0,
        isFavoriteLoading = isFavoriteLoading
    )
}
