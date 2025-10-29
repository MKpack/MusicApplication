package com.example.musicapplication.ui.model

import android.graphics.Bitmap

data class Song(
    var songTitle: String,
    var singer: String,
    var songTimeLong: Int,
    var isLoved: Boolean,
    var bitmap: Int
)
