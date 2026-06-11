package com.example.musicapplication.data.local.artwork

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.core.content.FileProvider
import com.example.musicapplication.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ArtworkCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("media")
    private val okHttpClient: OkHttpClient
) {
    private val artworkDir: File
        get() = File(context.cacheDir, "artwork")


    // 这里是给systemUI使用的，他需要fileProvide去拿app内的数据
    fun getCachedArtworkUri(song: Song): Uri? {
        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return cover.toUri()

        return getCachedArtworkFile(song)?.toArtworkUri()
    }

    /**
     * 给 app 自己读缓存文件使用，比如 Palette 提取背景色。
     * 读取包内 cache 不需要额外权限。
     */
    fun getCachedArtworkFile(song: Song): File? {
        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return File(cover)

        val file = artworkFile(song)
        return if (file.exists() && file.length() > 0L) file else null
    }

    /**
     * 有缓存返回，无缓存网络请求先缓存
     * 这里返回 content Uri，给 SystemUI / 通知栏这类外部进程读取。
     */
    suspend fun getOrDownloadArtworkUri(song: Song): Uri? {
        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return cover.toUri()

        return getOrDownloadArtworkFile(song)?.toArtworkUri()
    }

    /**
     * 有缓存返回缓存文件，无缓存则带 token 请求网络并写入 app cache。
     * 这里返回 File，适合 app 内部直接读取。
     */
    suspend fun getOrDownloadArtworkFile(song: Song): File? {
        getCachedArtworkFile(song)?.let { return it }

        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return File(cover)

        return withContext(Dispatchers.IO) {
            try {
                val file = artworkFile(song)
                file.parentFile?.mkdirs()

                val request = Request.Builder()
                    .url(cover)
                    .build()

                // .use { } 用完之后自动关闭资源
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null

                    file.outputStream().use { output ->
                        body.byteStream().copyTo(output)
                    }

                    if (file.length() > 0L) file else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }


    suspend fun clearArtworkCache() {
        withContext(Dispatchers.IO) {
            artworkDir.deleteRecursively()
        }
    }

    /**
     * 把cover从http转成文件uri
     * https://xxx.com/cover.jpg?token=abc
     * -> https://xxx.com/cover.jpg
     * -> .jpg
     * -> cache/artwork/songId.jpg
     */
    private fun artworkFile(song: Song): File {
        val extension = song.cover
            ?.substringBeforeLast("?")
            ?.substringAfterLast(".", missingDelimiterValue = "jpg")
            ?.takeIf { it.length in 2..5 }
            ?: "jpg"

        return File(artworkDir, "${song.songId}.$extension")
    }

    private fun File.toArtworkUri(): Uri {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            this
        )
        context.grantUriPermission(
            "com.android.systemui",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        return uri
    }
}
