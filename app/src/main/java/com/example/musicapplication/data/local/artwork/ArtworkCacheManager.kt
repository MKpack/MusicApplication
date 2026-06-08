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
import javax.inject.Singleton

@Singleton
class ArtworkCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val artworkDir: File
        get() = File(context.cacheDir, "artwork")

    fun getCachedArtworkUri(song: Song): Uri? {
        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return cover.toUri()

        val file = artworkFile(song)
        return if (file.exists() && file.length() > 0L) {
            file.toArtworkUri()
        } else {
            null
        }
    }

    /**
     * 有缓存返回，无缓存网络请求先缓存
     */
    suspend fun getOrDownloadArtworkUri(song: Song): Uri? {
        getCachedArtworkUri(song)?.let { return it }

        val cover = song.cover ?: return null
        if (!cover.startsWith("http")) return cover.toUri()

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

                    if (file.length() > 0L) file.toArtworkUri() else null
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
