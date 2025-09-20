package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

interface MainPageApi {
    @GET("/demo")
    public suspend fun test(): Response<ApiResponse<String>>
}