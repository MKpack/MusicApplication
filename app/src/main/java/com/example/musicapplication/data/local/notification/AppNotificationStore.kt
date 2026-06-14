package com.example.musicapplication.data.local.notification

interface AppNotificationStore {
    suspend fun getLastAutoShownNotificationId(): Long?
    suspend fun saveLastAutoShownNotificationId(notificationId: Long)
}
