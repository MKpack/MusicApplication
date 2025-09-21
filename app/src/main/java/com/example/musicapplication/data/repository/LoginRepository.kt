package com.example.musicapplication.data.repository


interface LoginRepository {
    suspend fun login(username: String, password: String): String
    suspend fun getAuthCode(email: String): String
    suspend fun register(email: String, authCode: String, password: String, passwordAgain: String): String
    suspend fun resetPassword(email: String, authCode: String, newPassword: String): String?
    //测试accessToken刷新
    suspend fun refresh(refreshToken: String): String?
}