package com.example.musicapplication.data.local.song.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 最近播放的表recent_song_play
 */
@Entity(tableName = "recent_song_play")
data class SongRecentPlayEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val songId: Long,
    val title: String,
    val singer: String,
    val cover: String?,
    val source: String?,
    val sourceType: String,
    val isLoved: Boolean,


    val updatedAt: Long,
    val playAt: Long
)
