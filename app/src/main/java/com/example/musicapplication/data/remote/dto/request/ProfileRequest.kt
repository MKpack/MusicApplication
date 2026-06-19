package com.example.musicapplication.data.remote.dto.request

data class ProfileRequest(
    val userId: Long,
    val email: String,
    val nickName: String?,
    val avatarUrl: String?
)