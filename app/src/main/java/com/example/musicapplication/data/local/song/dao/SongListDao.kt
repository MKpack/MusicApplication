package com.example.musicapplication.data.local.song.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.musicapplication.data.local.song.entity.SongListItemEntity



@Dao
interface SongListDao {

    @Query("DELETE FROM song_list_items WHERE listKey = :listKey")
    suspend fun clearList(listKey: String)

    @Upsert
    suspend fun upsertSongList(songList: SongListItemEntity)

    @Upsert
    suspend fun upsertList(list: List<SongListItemEntity>)


    /**
     * 查某个列表当前最大的 position，如果这个列表还没有数据，就返回 -1
     * coalesce 是联合的意思
     */
    @Query("""
        SELECT COALESCE(MAX(position), -1) 
        FROM song_list_items 
        WHERE listKey = :listKey
    """)
    suspend fun getMaxPosition(listKey: String): Long

    @Query("DELETE FROM song_list_items WHERE listKey = :listKey AND songId = :songId")
    suspend fun deleteItem(listKey: String, songId: Long)
}