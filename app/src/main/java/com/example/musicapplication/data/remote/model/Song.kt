package com.example.musicapplication.data.remote.model


data class Song(
    var songId: Int,
    var songTitle: String,
    var singer: String,
    var isLoved: Boolean,
    var cover: String?
)