package com.example.musicapplication.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val userId: String,
    val password: String
)