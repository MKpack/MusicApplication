package com.example.musicapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.musicapplication.data.remote.api.LoginApi
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import androidx.core.content.edit
import com.example.musicapplication.data.remote.api.RefreshTokenApi
import com.example.musicapplication.data.remote.api.RegisterApi
import com.example.musicapplication.data.remote.dto.LoginRequest
import com.example.musicapplication.data.remote.dto.TokenDto


class LoginRepositoryImpi @Inject constructor(
    private val loginApi: LoginApi,
    private val registerApi: RegisterApi,
    private val refreshTokenApi: RefreshTokenApi,
    @ApplicationContext private val context: Context
) : LoginRepository {
    private val TAG = "LoginRepositoryImpi"

    override suspend fun login(username: String, password: String): String {
        val apiResponse = loginApi.login(LoginRequest(username, password))
        if (apiResponse.isSuccessful) {
            val res = apiResponse.body()
            if (res?.success == true) {
                val accessToken = res.data?.accessToken
                val refreshToken = res.data?.refreshToken
                Log.d(TAG, accessToken + refreshToken)
                saveToken(accessToken, refreshToken)
                return "success"
            }
            else {
                return res?.message + ""
            }
        }
        else {
            return "请求失败"
        }
    }

    override suspend fun getAuthCode(email: String): String {
        val message = registerApi.sendCode(email)
        Log.d(TAG, message)
        return message
    }

    override suspend fun register(
        email: String,
        authCode: String,
        password: String,
        passwordAgain: String
    ): String {
        if (password != passwordAgain)
            return "两次输入密码不同"
        val message = registerApi.register(email, authCode, password)
        return message
    }

    override suspend fun resetPassword(
        email: String,
        authCode: String,
        newPassword: String
    ): String? {
        val response = loginApi.resetPassword(email, authCode, password = newPassword)
        Log.d(TAG, response.body()?.data.toString())
        return response.body()?.data
    }

    override suspend fun refresh(refreshToken: String): String? {
        Log.d(TAG, "refreshToken: $refreshToken")
        val accessToken = refreshTokenApi.getRefreshed(TokenDto(refreshToken))
        saveToken(accessToken.data, refreshToken)
        return accessToken.data
    }

    //保存token
    private fun saveToken(accessToken: String?, refreshToken: String?) {
        val sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        sp.edit {
            if (accessToken != "")
                putString("access_token", accessToken)
            if (refreshToken != "")
                putString("refresh_token", refreshToken)
        }
    }
    //读取token
    fun getCachedToken(): Map<String, String>  {
        val sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        val tokenMap = mutableMapOf<String, String>()
        val refreshToken = sp.getString("refresh_token", "")
        val accessToken = sp.getString("access_token", "")
        tokenMap["accessToken"] = accessToken.toString()
        tokenMap["refreshToken"] = refreshToken.toString()

        return tokenMap
    }

}