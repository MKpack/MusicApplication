package com.example.musicapplication.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.example.musicapplication.BuildConfig
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import java.io.File
import java.io.FileOutputStream

class LocalAudioMetaDataReader {
    companion object {

        fun saveEmbeddedCoverToCache(context: Context, songId: Long, embeddedPicture: ByteArray): String {
            val file = File(context.cacheDir, "cover_$songId.jpg")
            FileOutputStream(file).use { it.write(embeddedPicture) }
            return file.toUri().toString() // 存到 Song.cover
        }
        @JvmStatic
        fun getStaticAudioInfo(uri: Uri, context: Context): Song {
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

                val cover: String? = retriever.embeddedPicture?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                    saveEmbeddedCoverToCache(context, uri.hashCode().toLong(), it)
                }
                Song(
                    songId = -1,
                    songTitle = title ?: uri.lastPathSegment ?: "未知歌曲",
                    singer = artist ?: "未知歌手",
                    isLoved = false,
                    cover = cover,
                    source = MusicSource.Local(-1, uri)
                )
            } finally {
                retriever.release()
            }
        }

        fun buildMediaUrl(path: String?): String? {
            if (path.isNullOrBlank()) return null
            if (path.startsWith("http://") || path.startsWith("https://")) return path

            return BuildConfig.BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
        }
    }
}
