package com.example.musicapplication.data.local.song.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 表song_list_meta, 存储一个列表的元数据
 */
@Entity(tableName = "song_list_meta")
data class SongListMetaEntity(
    @PrimaryKey val listKey: String,
    val current: Long,  // 当前在第几页
    val pages: Long,    // 后端告诉的总页数
    val total: Long,    // 后端告诉的总数量
    val size: Long,     // 后端告诉的每页数量
    val updatedAt: Long // 上次更新时间
)