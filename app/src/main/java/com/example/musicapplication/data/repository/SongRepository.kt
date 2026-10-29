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

    fun observeDownloadSongs() : Flow<List<Song>>

    suspend fun refreshSongs(
        listKey: SongListKey,
        pageSize: Long = PAGE_SIZE
    ): RepositoryWorkResult<Unit>

    suspend fun loadMoreSongs(
        listKey: SongListKey,
        pageSize: Long = PAGE_SIZE
    ): RepositoryWorkResult<PageLoadResult>

    suspend fun upsertDownloadSongs(song: Song) : RepositoryWorkResult<String>

    /** 某个列表本地已经加载的歌（按列表顺序），播放器快播完时用来往队列后面接歌 */
    suspend fun getListSongs(listKey: SongListKey): List<Song>

    /** 歌曲的收藏状态，本地没有这首歌时是 null */
    fun observeSongLoved(songId: Long): Flow<Boolean?>

    suspend fun favoriteSong(songId: Long): RepositoryWorkResult<Unit>
    suspend fun unFavoriteSong(songId: Long): RepositoryWorkResult<Unit>

    suspend fun addRecentPlay(song: Song)

    suspend fun increaseSongPlayCount(songId: Long): RepositoryWorkResult<Unit>

    suspend fun clearRecentPlay()

    suspend fun deleteDownloadSongs(songIds: List<Long>)

    companion object {
        // 列表每页的歌曲数。太小的话首屏放不满，往下滑每隔几首就要停下来等网络
        const val PAGE_SIZE = 20L
    }
}
