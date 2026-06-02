package com.example.musicapplication.domain.model

sealed class SongListKey(val value: String) {
    data object Hot : SongListKey("hot")
    data object Loved : SongListKey("favorite")

    data class Playlist(val playlistId: Long) : SongListKey("playlist_$playlistId")
    data class Search(val keyword: String) : SongListKey("search_$keyword")
}