package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.ApiResponse
import com.example.musicapplication.data.remote.dto.LoginRequest
import com.example.musicapplication.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface LoginApi {
    @POST("/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<ApiResponse<TokenResponse>>

    @FormUrlEncoded
    @POST("/resetPassword")
    suspend fun resetPassword(
        @Field("email") email: String,
        @Field("auCode") auCode: String,
        @Field("password") password: String
    ): Response<ApiResponse<String>>
}