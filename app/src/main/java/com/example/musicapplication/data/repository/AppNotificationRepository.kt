package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.domain.model.AppNotification

interface AppNotificationRepository {
    suspend fun getLatestNotification(): RepositoryWorkResult<AppNotification?>
    suspend fun getLastAutoShownNotificationId(): Long?
    suspend fun saveLastAutoShownNotificationId(notificationId: Long)
}
