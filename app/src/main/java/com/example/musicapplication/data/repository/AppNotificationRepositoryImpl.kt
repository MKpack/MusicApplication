package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.notification.AppNotificationStore
import com.example.musicapplication.data.remote.api.AppNotificationApi
import com.example.musicapplication.data.remote.mapper.toAppNotification
import com.example.musicapplication.domain.model.AppNotification
import javax.inject.Inject

class AppNotificationRepositoryImpl @Inject constructor(
    private val appNotificationApi: AppNotificationApi,
    private val appNotificationStore: AppNotificationStore
) : AppNotificationRepository {

    override suspend fun getLatestNotification(): RepositoryWorkResult<AppNotification?> {
        return try {
            val response = appNotificationApi.getLatestNotification()
            if (response.code == 200) {
                RepositoryWorkResult.Success(response.data?.toAppNotification())
            } else {
                RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            RepositoryWorkResult.Failure("无法连接服务器", throwable = e)
        }
    }

    override suspend fun getLastAutoShownNotificationId(): Long? {
        return appNotificationStore.getLastAutoShownNotificationId()
    }

    override suspend fun saveLastAutoShownNotificationId(notificationId: Long) {
        appNotificationStore.saveLastAutoShownNotificationId(notificationId)
    }
}
