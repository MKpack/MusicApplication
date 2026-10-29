package com.example.musicapplication.domain.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayModeTest {

    @Test
    fun `每种模式对应播放器的循环方式和随机开关`() {
        assertEquals(Player.REPEAT_MODE_OFF, PlayMode.Sequence.repeatMode)
        assertFalse(PlayMode.Sequence.shuffleEnabled)

        assertEquals(Player.REPEAT_MODE_ALL, PlayMode.Repeat.repeatMode)
        assertFalse(PlayMode.Repeat.shuffleEnabled)

        assertEquals(Player.REPEAT_MODE_ONE, PlayMode.RepeatOne.repeatMode)
        assertFalse(PlayMode.RepeatOne.shuffleEnabled)

        // 随机播放一直循环下去
        assertEquals(Player.REPEAT_MODE_ALL, PlayMode.Shuffle.repeatMode)
        assertTrue(PlayMode.Shuffle.shuffleEnabled)
    }

    @Test
    fun `从播放器状态还原出的模式和设置的一致`() {
        PlayMode.entries.forEach { mode ->
            assertEquals(mode, PlayMode.from(mode.repeatMode, mode.shuffleEnabled))
        }
    }

    @Test
    fun `随机开着时不管循环方式都算随机播放`() {
        assertEquals(PlayMode.Shuffle, PlayMode.from(Player.REPEAT_MODE_OFF, true))
        assertEquals(PlayMode.Shuffle, PlayMode.from(Player.REPEAT_MODE_ONE, true))
    }

    @Test
    fun `保存的模式名认不出来时用默认的列表循环`() {
        assertEquals(PlayMode.RepeatOne, PlayMode.fromName("RepeatOne"))
        assertEquals(PlayMode.Repeat, PlayMode.fromName(""))
        assertEquals(PlayMode.Repeat, PlayMode.fromName(null))
        assertEquals(PlayMode.Repeat, PlayMode.fromName("Unknown"))
    }
}
