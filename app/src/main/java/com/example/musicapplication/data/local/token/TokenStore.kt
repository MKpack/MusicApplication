package com.example.musicapplication.data.local.token

interface TokenStore {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String?, refreshToken: String?)
    fun updateAccessToken(accessToken: String?)
    fun clear()
    fun hasLoginState(): Boolean
}