package com.example.musicapplication.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface LoginApi {
    @POST("login")
    suspend fun login(username: String, password: String): String
}