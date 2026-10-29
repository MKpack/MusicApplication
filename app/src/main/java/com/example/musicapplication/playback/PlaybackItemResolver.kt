package com.example.musicapplication.playback

import android.net.Uri
import androidx.core.net.toUri
import com.example.musicapplication.data.local.song.dao.DownloadSongDao
import com.example.musicapplication.data.local.song.dao.SongDao
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** 一首歌真正播放时用的东西 */
data class ResolvedSong(
    // 歌曲信息，来源、封面、歌词都是远程的（本地没有记录的歌保持原样）
    val song: Song,
    // 实际播放的地址：下载过就是本地文件，null 表示没有可播放的地址
    val playbackUri: Uri?,
    // 封面：下载过就是本地文件
    val artworkUri: Uri?,
    // 下载过的歌的本地歌词
    val localLyricPath: String?
)

/**
 * 歌曲加入播放队列前，先确定怎么播：
 * 1. 本地 songs 表里有这首歌，就用表里的远程地址、封面、歌词补全（最近播放里的歌没有歌词，
 *    下载列表里的歌只有本地路径，补全以后都能收藏、能显示歌词）；
 * 2. 下载过而且文件还在，就播放本地文件，不走网络。
 */
class PlaybackItemResolver @Inject constructor(
    private val songDao: SongDao,
    private val downloadSongDao: DownloadSongDao
) {
    suspend fun resolve(songs: List<Song>): List<ResolvedSong> = withContext(Dispatchers.IO) {
        val songIds = songs.map { it.songId }.filter { it > 0 }.distinct()
        val entities = if (songIds.isEmpty()) emptyMap() else {
            songDao.getSongsByIds(songIds).associateBy { it.songId }
        }
        val downloads = if (songIds.isEmpty()) emptyMap() else {
            downloadSongDao.getDownloadSongsByIds(songIds)
                .filter { File(it.localAudioPath).exists() }
                .associateBy { it.songId }
        }

        songs.map { song ->
            val entity = entities[song.songId]
            val canonical = if (entity == null) song else song.copy(
                isLoved = entity.isLoved,
                cover = entity.coverUrl ?: song.cover,
                lyric = entity.lyricUrl ?: song.lyric,
                source = entity.audioUrl?.let { MusicSource.Remote(song.songId, it) } ?: song.source
            )
            val download = downloads[song.songId]

            ResolvedSong(
                song = canonical,
                playbackUri = download?.let { File(it.localAudioPath).toUri() } ?: canonical.source?.toUri(),
                artworkUri = download?.localCoverPath?.takeIf { File(it).exists() }?.let { File(it).toUri() }
                    ?: canonical.cover?.toArtworkUri(),
                localLyricPath = download?.localLyricPath?.takeIf { File(it).exists() }
            )
        }
    }

    private fun MusicSource.toUri(): Uri = when (this) {
        is MusicSource.Remote -> url.toUri()
        is MusicSource.Local -> uri
    }

    /** 封面可能是网址、file:// / content:// 地址，也可能是本地文件的绝对路径 */
    private fun String.toArtworkUri(): Uri? = when {
        isBlank() -> null
        startsWith("/") -> File(this).toUri()
        else -> toUri()
    }
}
