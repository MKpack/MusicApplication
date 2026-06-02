package com.example.musicapplication.data.remote.interceptor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.musicapplication.data.local.token.TokenStore
import com.example.musicapplication.data.remote.api.AuthApi
import com.example.musicapplication.data.remote.api.TokenApi
import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.example.musicapplication.data.remote.dto.request.RefreshTokenRequest
import com.example.musicapplication.data.session.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.CompletableFuture

/**
 * 如果返回401或403进入该authenticate方法，在该方法中我们可以进行access的刷新
 * 问题：如果多个线程同时访问会多次刷新access（已解决）重复刷新我没有能力避免。
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val tokenApi: TokenApi,
    private val sessionManager: SessionManager
) : Authenticator {
    private val TAG = "TokenAuthenticator"
    private val lock = Any()


    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.request.header("Already-Retried") == "true") {
            sessionManager.onSessionExpired()
            return null
        }

        return synchronized(lock) {
            val requestToken = response.request.header("Authorization")
            val latestAccessToken = tokenStore.getAccessToken()

            if (!latestAccessToken.isNullOrBlank() &&
                requestToken != "Bearer $latestAccessToken") {
                // return@synchronized 只从 synchronized 这个代码块返回，不是从整个函数返回
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .header("Already-Retried", "true")
                    .build()
            }
            val refreshToken = tokenStore.getRefreshToken()

            if (refreshToken.isNullOrBlank()) {
                sessionManager.onSessionExpired()
                return@synchronized null
            }

            val newToken = try {
                val res = tokenApi.getRefreshedAccessToken(RefreshTokenRequest(refreshToken))
                    .execute()
                if (res.isSuccessful) {
                    res.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            if (newToken?.code != 200 || newToken.data.isNullOrBlank()) {
                sessionManager.onSessionExpired()
                return@synchronized null
            }
            tokenStore.updateAccessToken(newToken.data)

            response.request.newBuilder()
                .header("Authorization", "Bearer ${newToken.data}")
                .header("Already-Retried", "true")
                .build()
        }
    }
}