package com.example.musicapplication.domain.model

data class AppNotification(
    val notificationId: Long,
    val title: String,
    val content: String,
    val createdAt: String?
)
