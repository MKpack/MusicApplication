package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult


interface AuthRepository {
    suspend fun login(email: String, password: String): RepositoryWorkResult<Unit>
    suspend fun sendCode(email: String): RepositoryWorkResult<Unit>
    suspend fun register(email: String, authCode: String, password: String): RepositoryWorkResult<Unit>
    suspend fun resetPassword(email: String, authCode: String, newPassword: String): RepositoryWorkResult<Unit>
}
