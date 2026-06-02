package com.example.musicapplication.data.remote.dto.response

data class SongResponse(
    val songId: Long,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val lyricUrl: String,
    val duration: Int,
    val playCount: Int,
    val favoriteCount: Int,
    val isUserFavorite: Boolean
)

//{
//"songId": 20001,
//"title": "方圆几里",
//"artistName": "薛之谦",
//"coverUrl": "/cover/fangyuanjili.png",
//"source": "/audio/fangyuanjili.mp3",
//"lyricUrl": "/lyric/xiangnideye.lrc",
//"duration": 0,
//"playCount": 0,
//"favoriteCount": 0
//},
