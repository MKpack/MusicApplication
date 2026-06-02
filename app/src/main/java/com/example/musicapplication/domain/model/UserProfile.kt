package com.example.musicapplication.domain.model

data class UserProfile(
    val userId: Int,
    val email: String,
    val nickName: String?,
    val avatarUrl: String?
)