package com.example.musicapplication.data.remote.interceptor

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.musicapplication.data.remote.api.RefreshTokenApi
import com.example.musicapplication.data.remote.dto.ApiResponse
import com.example.musicapplication.data.remote.dto.TokenDto
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
    private val context: Context,
    private val refreshTokenApi: RefreshTokenApi
) : Authenticator {
    private val TAG = "TokenAuthenticator"
    //静态资源
    companion object {
        //静态变量，全局共享的刷新任务
        @Volatile
        private var ongoingRefresh: CompletableFuture<ApiResponse<String>>? = null
    }
    override fun authenticate(route: Route?, response: Response): Request? {

        val prefs = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        val refreshToken: String? = prefs.getString("refresh_token", null)

        //如果已经存在一个线程在更新了那么其他线程等待其完成
        val existingJob = ongoingRefresh
        if (existingJob != null) {
            val result = existingJob.get()      //阻塞等待结果
            Log.d(TAG, "existing:$result")
            return result.let {
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${it.data}")
                    .header("Already-Retried", "true")
                    .build()
            }
        }
        //如果还没有线程更新，就更新
        val newJob = CompletableFuture<ApiResponse<String>>()
        ongoingRefresh = newJob
        try {
            val newToken = try {
                val call = refreshTokenApi.getRefreshedAccessToken(TokenDto(refreshToken))
                val res = call.execute()
                if (res.isSuccessful) res.body()
                else null
            }catch (e: Exception) {
                null
            }
            if (newToken == null)   return null;
            if (!newToken.success){
                Log.d(TAG, newToken.message)
                return null
            }
            Log.d(TAG, "newJob:$newToken")
            //更新本地缓存
            prefs.edit {
                putString("access_token", newToken.data)
            }
            //通知所有等待的线程
            newJob.complete(newToken)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newToken.data}")
                .header("Already-Retried", "true")
                .build()
        }
        finally {
            ongoingRefresh = null
        }
    }
}