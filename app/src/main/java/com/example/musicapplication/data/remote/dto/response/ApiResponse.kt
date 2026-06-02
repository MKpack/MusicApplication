package com.example.musicapplication.data.remote.dto.response

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
)