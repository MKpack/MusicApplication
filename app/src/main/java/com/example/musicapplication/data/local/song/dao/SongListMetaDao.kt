package com.example.musicapplication.data.local.song.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.musicapplication.data.local.song.entity.SongListMetaEntity

@Dao
interface SongListMetaDao {

    @Upsert
    suspend fun upsertMeta(meta: SongListMetaEntity)

    @Query("SELECT * FROM song_list_meta WHERE listKey = :listKey")
    suspend fun getMeta(listKey: String): SongListMetaEntity?
}