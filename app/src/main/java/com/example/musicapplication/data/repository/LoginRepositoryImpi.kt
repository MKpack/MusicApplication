package com.example.musicapplication.data.repository

import com.example.musicapplication.data.remote.api.LoginApi
import com.example.musicapplication.data.remote.model.LoginRequest
import com.example.musicapplication.data.remote.model.LoginResponse
import jakarta.inject.Inject

class LoginRepositoryImpi @Inject constructor(
    private val api: LoginApi,
) : LoginRepository {
    override suspend fun login(username: String, password: String) {
        val response = api.login(LoginRequest(username, password))
        val loginResponse = response.body()

    }

    override suspend fun register(
        email: String,
        authCode: String,
        password: String,
        passwordAgain: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun resetPassword(
        email: String,
        authCode: String,
        newPassword: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getCachedToken() {
        TODO("Not yet implemented")
    }

}