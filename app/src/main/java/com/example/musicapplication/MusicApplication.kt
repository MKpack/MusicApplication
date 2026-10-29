package com.example.musicapplication

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidApp
class MusicApplication: Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    // 图片请求用不打印响应体的那个客户端（带登录 token），否则每张图片都会整个读进内存打印到日志里
    @Inject
    @Named("media")
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // 那个客户端为了在线播放没有读取超时，图片请求不能一直等，单独加上（连接池是共用的）
            .okHttpClient { okHttpClient.newBuilder().readTimeout(15, TimeUnit.SECONDS).build() }
            .crossfade(true)
            .build()
    }
}