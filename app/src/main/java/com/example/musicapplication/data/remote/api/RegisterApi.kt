package com.example.musicapplication.data.remote.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RegisterApi {
    @FormUrlEncoded
    @POST("/sendCode")
    suspend fun sendCode(@Field("email") email: String): String

    @FormUrlEncoded
    @POST("/register")
    suspend fun register(
        @Field("email") email: String,
        @Field("auCode") auCode: String,
        @Field("password") password: String
    ): String
}