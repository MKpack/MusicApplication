package com.example.musicapplication.data.local.song.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 表song，存储song的一些属性
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val songId: Long,
    val title: String,
    val singer: String,
    val cover: String?,
    val audioUrl: String?,
    val isLoved: Boolean,
    val updatedAt: Long
)