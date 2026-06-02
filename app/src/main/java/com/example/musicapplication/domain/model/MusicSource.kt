package com.example.musicapplication.domain.model

import android.net.Uri

sealed class MusicSource(val id: Int) {
    class Remote(id: Long, val url: String): MusicSource(id.toInt())
    class Local(id: Int, val uri: Uri): MusicSource(id)
}