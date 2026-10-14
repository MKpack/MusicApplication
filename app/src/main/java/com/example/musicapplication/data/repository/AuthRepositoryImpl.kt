package com.example.musicapplication.data.repository

import android.util.Log
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.common.toRepositoryFailure
import com.example.musicapplication.data.remote.api.AuthApi
import com.example.musicapplication.data.remote.dto.request.LoginRequest
import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.session.SessionManager
import javax.inject.Inject


class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    private val TAG = "AuthRepositoryImpl"

    override suspend fun login(email: String, password: String): RepositoryWorkResult<Unit> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            val token = response.data

            // 拿不到 token 也算失败，否则进入主页后所有请求都会 401
            if (response.code == 200 && token != null) {
                sessionManager.onLoginSuccess(token.accessToken, token.refreshToken)
                RepositoryWorkResult.Success(Unit)
            } else {
                RepositoryWorkResult.Failure(response.message, response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "login failed", e)
            e.toRepositoryFailure("网络请求失败")
        }
    }

    override suspend fun sendCode(email: String): RepositoryWorkResult<Unit> {
        return request("send code") { authApi.sendCode(email) }
    }

    override suspend fun register(
        email: String,
        authCode: String,
        password: String
    ): RepositoryWorkResult<Unit> {
        return request("register") { authApi.register(email, authCode, password) }
    }

    override suspend fun resetPassword(
        email: String,
        authCode: String,
        newPassword: String
    ): RepositoryWorkResult<Unit> {
        return request("reset password") { authApi.resetPassword(email, authCode, newPassword) }
    }

    /**
     * 后端成功时 data 是一句提示文字（如"成功注册"），不能拿它判断成功与否，只看 code == 200
     */
    private suspend fun request(
        action: String,
        call: suspend () -> ApiResponse<*>
    ): RepositoryWorkResult<Unit> {
        return try {
            val response = call()
            if (response.code == 200) {
                RepositoryWorkResult.Success(Unit)
            } else {
                RepositoryWorkResult.Failure(response.message, response.code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "$action failed", e)
            e.toRepositoryFailure("网络请求失败")
        }
    }
}
