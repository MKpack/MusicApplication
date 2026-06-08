package com.example.musicapplication.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.song.dao.SongDao
import com.example.musicapplication.data.local.song.dao.SongListDao
import com.example.musicapplication.data.local.song.dao.SongListMetaDao
import com.example.musicapplication.data.local.song.dao.SongRecentPlayDao
import com.example.musicapplication.data.local.song.database.SongDatabase
import com.example.musicapplication.data.local.song.entity.SongListItemEntity
import com.example.musicapplication.data.local.song.entity.SongListMetaEntity
import com.example.musicapplication.data.local.song.mapper.toSong
import com.example.musicapplication.data.remote.api.SongApi
import com.example.musicapplication.data.remote.dto.response.PageResponse
import com.example.musicapplication.data.remote.dto.response.SongResponse
import com.example.musicapplication.data.remote.mapper.toEntity
import com.example.musicapplication.domain.mapper.toSongRecentPlayEntity
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val songApi: SongApi,
    private val db: SongDatabase,
    private val songDao: SongDao,
    private val songListDao: SongListDao,
    private val songListMetaDao: SongListMetaDao,
    private val songRecentPlayDao: SongRecentPlayDao
): SongRepository {

    private val TAG = "SongRepository"
    override fun observeSongs(listKey: SongListKey): Flow<List<Song>> {
        return songDao.observeSongsByList(listKey.value)
            .map { list ->
                list.map { it ->
                    it.toSong()
                }
            }
    }

    override fun observeRecentSongs(): Flow<List<Song>> {
        return songRecentPlayDao.observeRecentSongs(100).map { songRecentPlayEntities ->
            songRecentPlayEntities.map { songRecentPlayEntity ->
                songRecentPlayEntity.toSong()
            }
        }
    }

    /**
     * 刷新歌曲，2种情况
     * 1. 大刷新，从第一首开始。----> refreshSongs,传递了true
     * 2. 下拉刷新，刷新当前页 + 1 -----> loadMoreSongs,传递了false
     */
    override suspend fun refreshSongs(
        listKey: SongListKey,
        pageSize: Long
    ): RepositoryWorkResult<Unit> {
        val result = requestSongs(listKey, 1, pageSize)
        when(result) {
            is RepositoryWorkResult.Success -> {
                savePageToLocal(listKey, result.data, true)
                return RepositoryWorkResult.Success(Unit)
            }
            is RepositoryWorkResult.Failure -> {
                return result
            }
        }
    }

    /**
     * 第二种情况
     */
    override suspend fun loadMoreSongs(
        listKey: SongListKey,
        pageSize: Long
    ): RepositoryWorkResult<Unit> {
        val meta = songListMetaDao.getMeta(listKey.value)
        if (meta != null && meta.current >= meta.pages) {
            return RepositoryWorkResult.Success(Unit)
        }

        val nextPage = (meta?.current ?: 0) + 1

        return when (val result = requestSongs(listKey, nextPage, pageSize)) {
            is RepositoryWorkResult.Success -> {
                savePageToLocal(
                    listKey = listKey,
                    page = result.data,
                    isRefresh = false
                )
                RepositoryWorkResult.Success(Unit)
            }

            is RepositoryWorkResult.Failure -> result
        }
    }

    /**
     * 喜欢歌曲
     */
    override suspend fun favoriteSong(songId: Long): RepositoryWorkResult<Unit> {
        val oldLoved = songDao.getLovedState(songId) ?: false
        val now = System.currentTimeMillis()

        // 先给他值，如果不成功再退回
        songDao.updateLoved(songId, true)
        // 更新一下recent play表状态
        songRecentPlayDao.updateLoved(songId, true)
        try {
            val response = songApi.favoriteSong(songId)

            if (response.code == 200 && response.data != null) {
                db.withTransaction {
                    songDao.upsertSong(response.data.toEntity())
                    songListDao.upsertSongList(
                        // 每次都插入到最新的一页和位置
                        SongListItemEntity(
                            listKey = SongListKey.Loved.value,
                            songId = songId,
                            position = 0,
                            page = 1,
                            updatedAt = now
                        )
                    )
                    songRecentPlayDao.updateLoved(songId, true)
                }
                return RepositoryWorkResult.Success(Unit)
            } else {
                songDao.updateLoved(songId, oldLoved)
                songRecentPlayDao.updateLoved(songId, oldLoved)
                return RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "favoriteSong: ${e.message}", e)
            songDao.updateLoved(songId, oldLoved)
            songRecentPlayDao.updateLoved(songId, oldLoved)
            return RepositoryWorkResult.Failure("无法连接服务器", throwable = e)
        }
    }

    /**
     * 取消喜欢歌曲
     */
    override suspend fun unFavoriteSong(songId: Long): RepositoryWorkResult<Unit> {
        val oldLoved = songDao.getLovedState(songId) ?: false
        val now = System.currentTimeMillis()

        // 先给他值，如果不成功再退回
        songDao.updateLoved(songId, false)
        songRecentPlayDao.updateLoved(songId, false)
        try {
            val response = songApi.unFavoriteSong(songId)

            if (response.code == 200 && response.data != null) {
                db.withTransaction {
                    songDao.upsertSong(response.data.toEntity())
                    songListDao.deleteItem(SongListKey.Loved.value, songId)
                    songRecentPlayDao.updateLoved(songId, false)
                }
                return RepositoryWorkResult.Success(Unit)
            } else {
                songDao.updateLoved(songId, oldLoved)
                songRecentPlayDao.updateLoved(songId, oldLoved)
                return RepositoryWorkResult.Failure(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "favoriteSong: ${e.message}", e)
            songDao.updateLoved(songId, oldLoved)
            songRecentPlayDao.updateLoved(songId, oldLoved)
            return RepositoryWorkResult.Failure("无法连接服务器", throwable = e)
        }
    }

    /**
     * recent play insert
     */
    override suspend fun addRecentPlay(song: Song) {
        songRecentPlayDao.insertRecentSong(song.toSongRecentPlayEntity())
        songRecentPlayDao.trimRecentSongs(100)
    }

    /**
     * 增加歌曲的播放次数
     */
    override suspend fun increaseSongPlayCount(songId: Long): RepositoryWorkResult<Unit> {
        try {
            val result = songApi.increaseSongPlayCount(songId)
            if (result.code == 200) {
                return RepositoryWorkResult.Success(Unit)
            }
            return RepositoryWorkResult.Failure("更新播放次数失败")
        } catch (e: Exception) {
            return RepositoryWorkResult.Failure("更新播放次数失败", throwable = e)
        }
    }

    /**
     * clear recent play songs
     */
    override suspend fun clearRecentPlay() {
        songRecentPlayDao.clearRecentSongs()
    }


    private suspend fun requestSongs(
        listKey: SongListKey,
        pageNum: Long,
        pageSize: Long
    ): RepositoryWorkResult<PageResponse<SongResponse>> {
        try {
            val response = when(listKey) {
                is SongListKey.Hot -> songApi.getHotSongs(pageNum, pageSize)
                is SongListKey.Loved -> songApi.getLovedSongs(pageNum, pageSize)
                is SongListKey.Playlist -> {
                    return RepositoryWorkResult.Failure("歌单接口还没有实现")
                }
                is SongListKey.Search -> {
                    return RepositoryWorkResult.Failure("歌单接口还没有实现")
                }
            }

            return if (response.code == 200 && response.data != null) {
                RepositoryWorkResult.Success(response.data)
            } else {
                RepositoryWorkResult.Failure(response.message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "requestSongs: ${e.message}", e)
            return RepositoryWorkResult.Failure("无法连接服务器", throwable = e)
        }
    }

    private suspend fun savePageToLocal(
        listKey: SongListKey,
        page: PageResponse<SongResponse>,
        isRefresh: Boolean
    ) {

        db.withTransaction {
            songDao.upsertSongs(
                page.records.map { it.toEntity() }
            )

            // 如果是刷新则清空该列表的本地数据
            if (isRefresh) {
                songListDao.clearList(listKey.value)
            }

            val startPosition = if (isRefresh) {
                0
            } else {
                songListDao.getMaxPosition(listKey.value) + 1
            }

            // 更新song_list_items表
            songListDao.upsertList(
                page.records.mapIndexed { index, songResponse ->
                    SongListItemEntity(
                        listKey = listKey.value,
                        songId = songResponse.songId,
                        position = startPosition + index,
                        page = page.current,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            )

            // 更新song_list_meta表
            songListMetaDao.upsertMeta(
                meta = SongListMetaEntity(
                    listKey = listKey.value,
                    current = page.current,
                    pages = page.pages,
                    total = page.total,
                    size = page.size,
                    updatedAt = System.currentTimeMillis()
                )
            )

        }
    }
}
