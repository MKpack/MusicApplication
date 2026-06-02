package com.example.musicapplication

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class MusicApplication: Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient = okHttpClient)
            .crossfade(true)
            .build()
    }
}