package com.example.musicapplication.domain.model

import android.net.Uri
import androidx.core.net.toUri

sealed class MusicSource(val id: Int) {
    class Remote(id: Long, val url: String): MusicSource(id.toInt())
    class Local(id: Int, val uri: Uri): MusicSource(id)

    /** 存到本地（播放快照、播放队列）时用的类型标记，和最近播放表里的 sourceType 一致 */
    val type: String
        get() = when (this) {
            is Remote -> TYPE_REMOTE
            is Local -> TYPE_LOCAL
        }

    /** 存到本地时用的值：远程是 url，本地是 uri 字符串 */
    val value: String
        get() = when (this) {
            is Remote -> url
            is Local -> uri.toString()
        }

    companion object {
        const val TYPE_REMOTE = "remote"
        const val TYPE_LOCAL = "local_uri"

        /** [type]、[value] 还原成 MusicSource，认不出来就返回 null */
        fun from(songId: Long, type: String?, value: String?): MusicSource? {
            if (value.isNullOrEmpty()) return null
            return when (type) {
                TYPE_REMOTE -> Remote(songId, value)
                TYPE_LOCAL -> Local(songId.toInt(), value.toUri())
                else -> null
            }
        }
    }
}
