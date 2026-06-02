package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.request.ProfileRequest
import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.response.ProfileResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {

    @GET("userProfile")
    suspend fun getUserProfile(): ApiResponse<ProfileResponse>

    @PUT("/user/profile")
    suspend fun updateUserProfile(@Body profileRequest: ProfileRequest): ApiResponse<ProfileResponse>

    @Multipart
    @POST("/user/avatar")
    suspend fun updateUserAvatar(@Part avatar: MultipartBody.Part): ApiResponse<ProfileResponse>
}