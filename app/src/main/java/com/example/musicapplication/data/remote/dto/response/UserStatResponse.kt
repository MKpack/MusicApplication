package com.example.musicapplication.data.remote.dto.response

data class UserStatResponse(
    val userId: Long,
    val playCount: Long,
    val favoriteCount: Long
)