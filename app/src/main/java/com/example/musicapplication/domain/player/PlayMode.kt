package com.example.musicapplication.domain.player

import androidx.media3.common.Player

/**
 * 播放模式。播放器本身只有「循环方式」和「是否随机」两个开关，这里把它们组合成界面上的四种模式：
 * 顺序 = 不循环；列表循环 = 循环全部；单曲循环 = 循环一首；随机 = 随机 + 循环全部（一直随机下去）。
 */
enum class PlayMode {
    Sequence, // 顺序播放
    Repeat,   // 循环播放
    Shuffle,  // 随机播放
    RepeatOne; // 单曲循环

    val repeatMode: Int
        get() = when (this) {
            Sequence -> Player.REPEAT_MODE_OFF
            Repeat, Shuffle -> Player.REPEAT_MODE_ALL
            RepeatOne -> Player.REPEAT_MODE_ONE
        }

    val shuffleEnabled: Boolean
        get() = this == Shuffle

    companion object {
        /** 第一次使用、没有保存过模式时用列表循环 */
        val Default = Repeat

        fun from(repeatMode: Int, shuffleEnabled: Boolean): PlayMode = when {
            shuffleEnabled -> Shuffle
            repeatMode == Player.REPEAT_MODE_ONE -> RepeatOne
            repeatMode == Player.REPEAT_MODE_ALL -> Repeat
            else -> Sequence
        }

        fun fromName(name: String?): PlayMode = entries.firstOrNull { it.name == name } ?: Default
    }
}
