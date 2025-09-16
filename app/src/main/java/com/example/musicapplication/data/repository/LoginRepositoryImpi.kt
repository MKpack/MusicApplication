package com.example.musicapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.musicapplication.data.remote.api.LoginApi
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import androidx.core.content.edit
import com.example.musicapplication.data.remote.api.RegisterApi


class LoginRepositoryImpi @Inject constructor(
    private val loginApi: LoginApi,
    private val registerApi: RegisterApi,
    @ApplicationContext private val context: Context
) : LoginRepository {
    private val TAG = "LoginRepositoryImpi"
    override suspend fun login(username: String, password: String): Result<String> {
        val token = loginApi.login(username, password)
        if (!token.isEmpty()) {
            saveToken(token)
            return Result.success(token)
        }
        else {
            return Result.failure(Exception("登陆失败，token为空"))
        }
    }
    //保存token
    private fun saveToken(token: String) {
        val sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        sp.edit {
            putString("token", token)
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
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getCachedToken() {
        TODO("Not yet implemented")
    }

}