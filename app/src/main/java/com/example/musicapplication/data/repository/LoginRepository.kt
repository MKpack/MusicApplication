package com.example.musicapplication.data.repository


interface LoginRepository {
    suspend fun login(username: String, password: String)
    suspend fun register(email: String, authCode: String, password: String, passwordAgain: String)
    suspend fun resetPassword(email: String, authCode: String, newPassword: String)

    suspend fun getCachedToken()
}