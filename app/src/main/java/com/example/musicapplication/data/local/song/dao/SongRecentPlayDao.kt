package com.example.musicapplication.data.local.song.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.musicapplication.data.local.song.entity.SongRecentPlayEntity
import kotlinx.coroutines.flow.Flow

/**
 * 最近播放的查询
 */
@Dao
interface SongRecentPlayDao {

    @Query("""
        SELECT * 
        FROM recent_song_play
        ORDER BY playAt DESC
        LIMIT :limit
    """)
    fun observeRecentSongs(limit: Int) : Flow<List<SongRecentPlayEntity>>

    @Insert
    suspend fun insertRecentSong(song: SongRecentPlayEntity)

    @Query("""
        DELETE FROM recent_song_play
        WHERE id NOT IN (
            SELECT id FROM recent_song_play
            ORDER BY playAt DESC
            LIMIT :limit
        )
    """)
    suspend fun trimRecentSongs(limit: Int)


    @Query("DELETE FROM recent_song_play")
    suspend fun clearRecentSongs()
}