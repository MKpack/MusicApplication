package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.domain.model.PageLoadResult
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import kotlinx.coroutines.flow.Flow

interface SongRepository {

    fun observeSongs(listKey: SongListKey): Flow<List<Song>>

    fun observeSongsByIds(songIds: List<Long>): Flow<List<Song>>

    fun observeRecentSongs(): Flow<List<Song>>

    suspend fun refreshSongs(
        listKey: SongListKey,
        pageSize: Long = 5
    ): RepositoryWorkResult<Unit>

    suspend fun loadMoreSongs(
        listKey: SongListKey,
        pageSize: Long = 5
    ): RepositoryWorkResult<PageLoadResult>

    suspend fun getSongPosition(
        listKey: SongListKey,
        songId: Long
    ): Long?

    suspend fun ensureSongAtPosition(
        listKey: SongListKey,
        position: Long,
        pageSize: Long = 5
    ): RepositoryWorkResult<Song?>

    suspend fun getSongListTotal(listKey: SongListKey): Long?

    suspend fun favoriteSong(songId: Long): RepositoryWorkResult<Unit>
    suspend fun unFavoriteSong(songId: Long): RepositoryWorkResult<Unit>

    suspend fun addRecentPlay(song: Song)

    suspend fun increaseSongPlayCount(songId: Long): RepositoryWorkResult<Unit>

    suspend fun clearRecentPlay()
}
