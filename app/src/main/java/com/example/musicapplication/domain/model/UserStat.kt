package com.example.musicapplication.domain.model

data class UserStat(
    val userId: Long,
    val playCount: Long = 0L,
    val favoriteCount: Long = 0L
)
