package com.example.musicapplication.data.remote.dto.request

data class ProfileRequest(
    val userId: Int,
    val email: String,
    val nickName: String?,
    val avatarUrl: String?
)