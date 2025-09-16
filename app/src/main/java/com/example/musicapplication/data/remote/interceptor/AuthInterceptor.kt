package com.example.musicapplication.data.remote.interceptor

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 拦截器
 * 每次进行网络请求需要带token，
 * 该类每次重新从sharedPreferences中读取token，
 * 保证每次都是正确的
 */
class AuthInterceptor(private val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
        val newRequest = chain.request().newBuilder().apply {
            if (token.isNotEmpty()) {
                addHeader("Authorization", "Bearer$token")
            }
        }
            .build()
        return chain.proceed(newRequest)
    }

}