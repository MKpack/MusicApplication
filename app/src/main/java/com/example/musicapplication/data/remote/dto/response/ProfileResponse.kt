package com.example.musicapplication.data.remote.dto.response

data class ProfileResponse(
    val userId: Int,
    val email: String,
    val nickName: String?,
    val avatarUrl: String?
)