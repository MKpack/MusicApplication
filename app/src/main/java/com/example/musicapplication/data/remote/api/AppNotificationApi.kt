package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.response.AppNotificationResponse
import retrofit2.http.GET

interface AppNotificationApi {

    @GET("/notification/latest")
    suspend fun getLatestNotification(): ApiResponse<AppNotificationResponse?>
}
