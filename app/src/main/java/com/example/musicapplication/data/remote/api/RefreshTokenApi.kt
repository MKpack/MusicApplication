package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.ApiResponse
import com.example.musicapplication.data.remote.dto.TokenDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RefreshTokenApi {
    //自动刷新accessToken
    @POST("/refreshToken")
    public fun getRefreshedAccessToken(@Body refreshToken: TokenDto?): Call<ApiResponse<String>>

    //测试accessToken的刷新
    @POST("/refreshToken")
    public suspend fun getRefreshed(@Body refreshToken: TokenDto?): ApiResponse<String>
}