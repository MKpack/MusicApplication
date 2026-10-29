package com.example.musicapplication.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 在线歌曲的音频缓存：边播边存，再听同一首歌不用重新下载，网络差时也更流畅。
 * 最多 256MB，满了先删最久没听的。同一个目录只能有一个 SimpleCache 实例，所以必须是单例。
 */
@OptIn(UnstableApi::class)
@Singleton
class AudioCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val cache: SimpleCache by lazy {
        SimpleCache(
            File(context.cacheDir, "audio"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }

    /** 清空缓存。正在播放的歌也会被清掉，之后会从网络重新读 */
    suspend fun clear() = withContext(Dispatchers.IO) {
        cache.keys.toList().forEach { key ->
            runCatching { cache.removeResource(key) }
        }
    }

    private companion object {
        const val MAX_BYTES = 256L * 1024 * 1024
    }
}
