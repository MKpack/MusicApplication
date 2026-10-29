package com.example.musicapplication.data.local.song.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.musicapplication.data.local.song.entity.DownloadSongEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface DownloadSongDao {

    @Upsert
    suspend fun upsertDownloadSong(downloadSongEntity: DownloadSongEntity)

    /**
     * observe 监听room中的downloadSongs数据
     */
    @Query("""
        SELECT downloadSongs.*
        FROM downloadSongs
        ORDER BY downloadSongs.downloadedAt DESC
    """)
    fun observeDownloadSongs() : Flow<List<DownloadSongEntity>>

    /**
     * 查这些歌里哪些已经下载过，播放时优先用本地文件
     */
    @Query("SELECT * FROM downloadSongs WHERE songId IN (:songIds)")
    suspend fun getDownloadSongsByIds(songIds: List<Long>): List<DownloadSongEntity>


    /**
     * 批量删除传入的songId
     */
    @Query("""
        DELETE FROM downloadSongs
        WHERE songId in (:items)
    """)
    suspend fun deleteDownloadSongs(items: List<Long>)
}