package com.example.musicapplication.data.local.song.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.musicapplication.data.local.song.entity.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * 对song进行查询
 */
@Dao
interface SongDao {

    @Upsert
    suspend fun upsertSong(song: SongEntity)
    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET isLoved = :isLoved WHERE songId = :songId")
    suspend fun updateLoved(songId: Long, isLoved: Boolean)

    @Query("""
        SELECT songs.* 
        FROM songs
        INNER JOIN song_list_items
        ON songs.songId = song_list_items.songId
        WHERE song_list_items.listKey = :listKey
        ORDER BY song_list_items.position ASC
    """)
    fun observeSongsByList(listKey: String): Flow<List<SongEntity>>


    /**
     * sqlite没有真正的boolean，本质变成false -> 0和true -> 1代替
     */
    @Query("""
        SELECT * 
        FROM songs
        WHERE isLoved = 1
        ORDER BY updatedAt DESC
    """)
    fun observeLovedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE songId IN (:songIds)")
    fun observeSongsByIds(songIds: List<Long>): Flow<List<SongEntity>>

    @Query("""
        SELECT songs.*
        FROM songs
        INNER JOIN song_list_items
        ON songs.songId = song_list_items.songId
        WHERE song_list_items.listKey = :listKey
        AND song_list_items.position = :position
        LIMIT 1
    """)
    suspend fun getSongByListPosition(listKey: String, position: Long): SongEntity?

    @Query("SELECT isLoved FROM songs WHERE songId = :songId")
    suspend fun getLovedState(songId: Long): Boolean?
}
