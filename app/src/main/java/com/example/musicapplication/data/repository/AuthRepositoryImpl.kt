package com.example.musicapplication.data.repository

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import androidx.core.content.edit
import com.example.musicapplication.data.remote.api.AuthApi
import com.example.musicapplication.data.remote.dto.request.LoginRequest
import com.example.musicapplication.data.session.SessionManager


class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    private val TAG = "AuthRepositoryImpl"

    override suspend fun login(username: String, password: String): String {
        try {
            val apiResponse = authApi.login(LoginRequest(username, password))

            if (apiResponse.code == 200) {
                val token = apiResponse.data
                if (token != null) {
                    sessionManager.onLoginSuccess(token.accessToken, token.refreshToken)
                }
                return "success"
            } else {
                return apiResponse.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "login failed", e)
            return "网络请求失败"
        }
    }

    override suspend fun getAuthCode(email: String): String {
        return try {
            val response = authApi.sendCode(email)
            if (response.code == 200) {
                response.data ?: response.message
            } else {
                response.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "get auth code failed", e)
            "网络请求失败"
        }
    }

    override suspend fun register(
        email: String,
        authCode: String,
        password: String,
        passwordAgain: String
    ): String {
        if (password != passwordAgain)
            return "两次输入密码不同"
        return try {
            val response = authApi.register(email, authCode, password)
            if (response.code == 200) {
                response.data ?: response.message
            } else {
                response.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "register failed", e)
            "网络请求失败"
        }
    }

    override suspend fun resetPassword(
        email: String,
        authCode: String,
        newPassword: String
    ): String? {
        return try {
            val response = authApi.resetPassword(email, authCode, newPassword)
            if (response.code == 200) {
                response.data ?: response.message
            } else {
                response.message
            }
        } catch (e: Exception) {
            Log.e(TAG, "reset password failed", e)
            "网络请求失败"
        }
    }
}