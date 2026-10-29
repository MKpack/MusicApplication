package com.example.musicapplication.domain.model

sealed class SongListKey(val value: String) {
    data object Hot : SongListKey("hot")
    data object Loved : SongListKey("favorite")

    data class Playlist(val playlistId: Long) : SongListKey("playlist_$playlistId")
    data class Search(val keyword: String) : SongListKey("search_$keyword")

    companion object {
        private const val PLAYLIST_PREFIX = "playlist_"
        private const val SEARCH_PREFIX = "search_"

        /** [value] 还原成 SongListKey（播放队列里只存了字符串），认不出来返回 null */
        fun fromValue(value: String?): SongListKey? = when {
            value == null -> null
            value == Hot.value -> Hot
            value == Loved.value -> Loved
            value.startsWith(PLAYLIST_PREFIX) -> value.removePrefix(PLAYLIST_PREFIX).toLongOrNull()?.let { Playlist(it) }
            value.startsWith(SEARCH_PREFIX) -> Search(value.removePrefix(SEARCH_PREFIX))
            else -> null
        }
    }
}
