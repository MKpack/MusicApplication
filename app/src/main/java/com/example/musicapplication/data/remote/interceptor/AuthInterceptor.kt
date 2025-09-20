package com.example.musicapplication.data.remote.interceptor

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 拦截器
 * 每次进行网络请求需要带token，
 * 该类每次重新从sharedPreferences中读取token，
 * 保证每次都是正确的
 * 重写了AuthInterceptor拦截器，
 * 每次请求都会重新走到这个interceptor方法，获取最新token
 */
class AuthInterceptor(
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sp = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        val accessToken = sp.getString("access_token", "")
        //如果有token就加进请求头
        val request = if (accessToken != "") {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}