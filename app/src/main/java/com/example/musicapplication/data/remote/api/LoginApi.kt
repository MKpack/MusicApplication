package com.example.musicapplication.data.remote.api

import com.example.musicapplication.data.remote.model.LoginRequest
import com.example.musicapplication.data.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface LoginApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}