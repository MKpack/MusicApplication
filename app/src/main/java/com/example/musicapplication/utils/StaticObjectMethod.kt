package com.example.musicapplication.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.example.musicapplication.data.remote.model.Song
import java.io.File
import java.io.FileOutputStream

class StaticObjectMethod {
    companion object {

        fun saveEmbeddedCoverToCache(context: Context, songId: Long, embeddedPicture: ByteArray): String {
            val file = File(context.cacheDir, "cover_$songId.jpg")
            FileOutputStream(file).use { it.write(embeddedPicture) }
            return file.toUri().toString() // 存到 Song.cover
        }
        @JvmStatic
        fun getStaticAudioInfo(uri: Uri, context: Context): Song {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val coverBitmap = retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

            val cover: String? = retriever.embeddedPicture?.let {
                saveEmbeddedCoverToCache(context, uri.hashCode().toLong(), it)
            }
            retriever.release()
            return Song(
                songId = -1,
                songTitle = title ?: uri.lastPathSegment ?: "未知歌曲",
                singer = artist ?: "未知歌手",
                isLoved = false,
                cover = cover
            )
        }
    }
}