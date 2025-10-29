package com.example.musicapplication.utils

class MusicProgressUtils {
    //伴生---伪静态
    companion object {
        @JvmStatic
        fun convertSecondsToMinutes(seconds: Int) : String {
            val tmp = seconds % 60
            return "" + (seconds / 60) + ":" + if (tmp < 10) "0$tmp" else tmp
        }
    }
}