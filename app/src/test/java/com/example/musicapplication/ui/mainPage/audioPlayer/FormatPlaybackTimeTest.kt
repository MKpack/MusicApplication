package com.example.musicapplication.ui.mainPage.audioPlayer

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatPlaybackTimeTest {

    @Test
    fun `毫秒转成分秒`() {
        assertEquals("0:00", formatPlaybackTime(0))
        assertEquals("0:00", formatPlaybackTime(999))
        assertEquals("0:05", formatPlaybackTime(5_400))
        assertEquals("1:23", formatPlaybackTime(83_500))
        assertEquals("10:00", formatPlaybackTime(600_000))
    }

    @Test
    fun `负数按 0 显示`() {
        assertEquals("0:00", formatPlaybackTime(-1_000))
    }
}
