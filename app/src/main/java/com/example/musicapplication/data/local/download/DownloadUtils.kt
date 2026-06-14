package com.example.musicapplication.data.local.download

import android.content.Context
import com.example.musicapplication.domain.model.MusicSource
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

/**
 * 下载工具类拆分出来
 */
@Singleton
class DownloadUtils @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("media") private val mediaOkHttpClient: OkHttpClient
) {

    suspend fun downloadSongFiles(song: Song) : DownloadedSongFiles {
        val source = song.source as? MusicSource.Remote
            ?: throw IllegalArgumentException("本地音乐不需要下载")

        return withContext(Dispatchers.IO) {
            // finalDir = 下载完成后正式存放的位置
            // tempDir = 下载过程中临时存放的位置
            val finalDir = File(context.filesDir, "downloads/songs/${song.songId}")
            val tempDir = File(context.filesDir, "downloads/tmp/${song.songId}_${System.currentTimeMillis()}")

            try {
                tempDir.mkdirs()

                val audioFile = downloadRequiredFile(
                    url = source.url,
                    dir = tempDir,
                    prefix = "audio",
                    fallbackExtension = "mp3"
                )

                val coverFile = downloadOptionalFile(
                    url = song.cover,
                    dir = tempDir,
                    prefix = "cover",
                    fallbackExtension = "jpg"
                )

                val lyricFile = downloadOptionalFile(
                    url = song.lyric,
                    dir = tempDir,
                    prefix = "lyric",
                    fallbackExtension = "lrc"
                )

                // 删除旧的正式的下载目录，然后创建他的父目录，如果父目录不存在
                finalDir.deleteRecursively()
                finalDir.parentFile?.mkdirs()

                // 直接移动，移动失败才进行拷贝
                if (!tempDir.renameTo(finalDir)) {
                    tempDir.copyRecursively(finalDir, overwrite = true)
                    tempDir.deleteRecursively()
                }

                val finalAudioFile = File(finalDir, audioFile.name)
                val finalCoverFile = coverFile?.let { File(finalDir, it.name) }
                val finalLyricFile = lyricFile?.let { File(finalDir, it.name) }

                DownloadedSongFiles(
                    audioPath = finalAudioFile.absolutePath,
                    coverPath = finalCoverFile?.absolutePath,
                    lyricPath = finalLyricFile?.absolutePath,
                    fileSize = listOfNotNull(finalAudioFile, finalCoverFile, finalLyricFile)
                        .sumOf { it.length() }
                )
            } catch (e: Exception) {
                tempDir.deleteRecursively()
                throw e
            }
        }
    }

    private fun downloadRequiredFile(
        url: String,
        dir: File,
        prefix: String,
        fallbackExtension: String
    ) : File {
        val file = File(dir, buildFileName(url, prefix, fallbackExtension))

        val request = Request.Builder()
            .url(url)
            .build()
        mediaOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("下载失败: ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("响应体为空")
            file.outputStream().use { outputStream ->
                body.byteStream().use { input ->
                    input.copyTo(outputStream)
                }
            }
        }

        if (!file.exists() || file.length() <= 0L) {
            throw IllegalStateException("下载文件为空")
        }

        return file
    }

    private fun downloadOptionalFile(
        url: String?,
        dir: File,
        prefix: String,
        fallbackExtension: String
    ): File? {
        if (url.isNullOrBlank() || !url.startsWith("http")) return null

        return try {
            downloadRequiredFile(url, dir, prefix, fallbackExtension)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteSongFiles(songId: Long) {
        withContext(Dispatchers.IO) {
            File(context.filesDir, "downloads/songs/$songId").deleteRecursively()
        }
    }

    private fun buildFileName(
        url: String,
        prefix: String,
        fallbackExtension: String
    ): String {
        val extension = url
            .substringBefore("?")
            .substringAfterLast(".", missingDelimiterValue = fallbackExtension)
            .takeIf { it.length in 2..5 }
            ?: fallbackExtension

        return "$prefix.$extension"
    }
}