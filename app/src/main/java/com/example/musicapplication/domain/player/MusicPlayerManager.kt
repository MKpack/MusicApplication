package com.example.musicapplication.domain.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * 封装exoplayer用于播放自己的音乐
 */
class MusicPlayerManager(context: Context) {

    private val player = ExoPlayer.Builder(context).build()


}