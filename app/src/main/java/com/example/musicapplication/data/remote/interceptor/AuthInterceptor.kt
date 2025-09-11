package com.example.musicapplication.data.remote.interceptor

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

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