package com.example.musicapplication.data.local.song.entity

import androidx.room.Entity

/**
 * song_list_items表，存储每首歌对应的列表如 “喜欢” “热门”
 */
@Entity(
    tableName = "song_list_items",
    primaryKeys = ["listKey", "songId"]
)
data class SongListItemEntity(
    val listKey: String,
    val songId: Long,
    val position: Long,     // 在这个列表中的位置
    val page: Long,         // 在这个列表中的页
    val updatedAt: Long
)
