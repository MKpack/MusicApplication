package com.example.musicapplication.domain.model

data class Song(
    var songId: Long,
    var songTitle: String,
    var singer: String,
    var isLoved: Boolean,
    var cover: String?,
    var source: MusicSource?
)