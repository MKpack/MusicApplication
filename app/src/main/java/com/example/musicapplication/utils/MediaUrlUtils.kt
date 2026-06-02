package com.example.musicapplication.utils

import com.example.musicapplication.BuildConfig

object MediaUrlUtils {

    fun buildMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path

        return BuildConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
    }
}