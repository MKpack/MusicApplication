package com.example.musicapplication.utils

import com.example.musicapplication.ui.mainPage.audioPlayer.LyricLine

/**
 * 歌词解析器
 */
object LrcParser {
    private val timeRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?](.*)""")

    fun parse(content: String): List<LyricLine> {
        return content
            .lineSequence()
            .mapNotNull { line ->
                val match = timeRegex.find(line) ?: return@mapNotNull null

                val minute = match.groupValues[1].toIntOrNull() ?: 0
                val second = match.groupValues[2].toIntOrNull() ?: 0
                val millisText = match.groupValues[3]

                val millis = when (millisText.length) {
                    1 -> millisText.toInt() * 100
                    2 -> millisText.toInt() * 10
                    3 -> millisText.toInt()
                    else -> 0
                }

                val text = match.groupValues[4].trim()
                if (text.isBlank()) return@mapNotNull null

                LyricLine(
                    timeSeconds = minute * 60 + second + millis / 1000f,
                    text = text
                )
            }
            .sortedBy { it.timeSeconds }
            .toList()
    }
}