package com.example.musicapplication.data.remote.mapper

import com.example.musicapplication.data.remote.dto.response.AppNotificationResponse
import com.example.musicapplication.domain.model.AppNotification
import com.example.musicapplication.utils.MediaUrlUtils

fun AppNotificationResponse.toAppNotification(): AppNotification {
    return AppNotification(
        notificationId = notificationId,
        title = title.orEmpty().ifBlank { "通知" },
        content = content.orEmpty(),
        imageUrl = MediaUrlUtils.buildMediaUrl(imageUrl),
        createdAt = createdAt,
    )
}
