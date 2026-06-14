package com.example.musicapplication.data.local.song.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicapplication.data.local.song.dao.DownloadSongDao
import com.example.musicapplication.data.local.song.dao.SongDao
import com.example.musicapplication.data.local.song.dao.SongListDao
import com.example.musicapplication.data.local.song.dao.SongListMetaDao
import com.example.musicapplication.data.local.song.dao.SongRecentPlayDao
import com.example.musicapplication.data.local.song.entity.DownloadSongEntity
import com.example.musicapplication.data.local.song.entity.SongEntity
import com.example.musicapplication.data.local.song.entity.SongListItemEntity
import com.example.musicapplication.data.local.song.entity.SongListMetaEntity
import com.example.musicapplication.data.local.song.entity.SongRecentPlayEntity


@Database(
    entities = [
        SongEntity::class,
        SongListItemEntity::class,
        SongListMetaEntity::class,
        SongRecentPlayEntity::class,
        DownloadSongEntity::class
    ],
    version = 1
)
abstract class SongDatabase : RoomDatabase() {
    abstract fun songDao() : SongDao
    abstract fun songListDao() : SongListDao
    abstract fun songListMetaDao() : SongListMetaDao

    abstract fun songRecentPlayDao() : SongRecentPlayDao

    abstract fun downloadSongDao() : DownloadSongDao
}