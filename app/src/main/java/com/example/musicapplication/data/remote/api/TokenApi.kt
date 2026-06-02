package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.dto.request.RefreshTokenRequest
import com.example.musicapplication.data.remote.dto.response.ApiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenApi {
    //自动刷新accessToken
    @POST("/refreshToken")
    public fun getRefreshedAccessToken(@Body refreshToken: RefreshTokenRequest?): Call<ApiResponse<String>>

    //测试accessToken的刷新
    @POST("/refreshToken")
    public suspend fun getRefreshed(@Body refreshToken: RefreshTokenRequest?): ApiResponse<String>
}