package com.example.musicapplication.data.remote.dto.response

data class AppNotificationResponse(
    val notificationId: Long,
    val title: String?,
    val content: String?,
    val status: Int?,
    val startTime: String?,
    val endTime: String?,
    val createdAt: String?,
    val updatedAt: String?
)
