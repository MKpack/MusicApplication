package com.example.musicapplication.data.local.download

data class DownloadedSongFiles(
    val audioPath: String,
    val coverPath: String?,
    val lyricPath: String?,
    val fileSize: Long
)
