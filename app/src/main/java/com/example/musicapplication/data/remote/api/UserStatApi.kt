package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.response.UserStatResponse
import retrofit2.http.GET
import retrofit2.http.POST

interface UserStatApi {

    @GET("/userStat/get")
    suspend fun getUserStat() : ApiResponse<UserStatResponse>

    @POST("/userStat/increase")
    suspend fun increasePlayStat() : ApiResponse<UserStatResponse>

//    @POST("/userStat/decrease")
//    suspend fun decreasePlayStat() : ApiResponse<UserStatResponse>
}