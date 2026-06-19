package com.example.musicapplication.domain.model

data class UserProfile(
    val userId: Long,
    val email: String,
    val nickName: String?,
    val avatarUrl: String?
)