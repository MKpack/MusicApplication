package com.example.musicapplication.domain.model

import java.io.File

sealed class MusicSource(val id: Int) {
    class Remote(id: Int): MusicSource(id)
    class Local(id: Int, val path: File): MusicSource(id)
}