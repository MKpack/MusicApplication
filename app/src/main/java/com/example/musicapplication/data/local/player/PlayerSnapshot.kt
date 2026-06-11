package com.example.musicapplication.data.local.player

data class PlayerSnapshot(
    val songIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val playModeName: String
)
