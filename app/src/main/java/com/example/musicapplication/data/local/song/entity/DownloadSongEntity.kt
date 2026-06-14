package com.example.musicapplication.data.local.song.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloadSongs")
data class DownloadSongEntity(
    @PrimaryKey val songId: Long,

    val title: String,
    val singer: String,

    val localAudioPath: String,
    val localCoverPath: String?,
    val localLyricPath: String?,

    val fileSize: Long,
    val downloadedAt: Long
)