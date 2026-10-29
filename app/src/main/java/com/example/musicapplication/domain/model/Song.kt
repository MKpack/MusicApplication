package com.example.musicapplication.domain.model

data class Song(
    val songId: Long,
    val songTitle: String,
    val singer: String,
    val isLoved: Boolean,
    val cover: String?,
    val lyric: String? = null,
    val source: MusicSource?
)
