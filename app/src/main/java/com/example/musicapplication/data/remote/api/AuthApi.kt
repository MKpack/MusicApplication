package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.request.LoginRequest
import com.example.musicapplication.data.remote.dto.request.RefreshTokenRequest
import com.example.musicapplication.data.remote.dto.response.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApi {

    @POST("/login")
    suspend fun login(@Body loginRequest: LoginRequest): ApiResponse<TokenResponse>

    @FormUrlEncoded
    @POST("/register")
    suspend fun register(
        @Field("email") email: String,
        @Field("auCode") auCode: String,
        @Field("password") password: String
    ): ApiResponse<String>

    @FormUrlEncoded
    @POST("/sendCode")
    suspend fun sendCode(@Field("email") email: String): ApiResponse<String>

    @FormUrlEncoded
    @POST("/resetPassword")
    suspend fun resetPassword(
        @Field("email") email: String,
        @Field("auCode") auCode: String,
        @Field("password") password: String
    ): ApiResponse<String>

}