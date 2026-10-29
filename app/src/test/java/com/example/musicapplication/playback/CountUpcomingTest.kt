package com.example.musicapplication.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class CountUpcomingTest {

    // 按 order 的顺序播放，返回下一首；最后一首之后没有了
    private fun nextIn(order: List<Int>): (Int) -> Int = { index ->
        val position = order.indexOf(index)
        order.getOrNull(position + 1) ?: C.INDEX_UNSET
    }

    @Test
    fun `数当前歌曲后面还有几首`() {
        val order = listOf(0, 1, 2, 3, 4)
        assertEquals(4, countUpcoming(0, 10, nextIn(order)))
        assertEquals(2, countUpcoming(2, 10, nextIn(order)))
        assertEquals(0, countUpcoming(4, 10, nextIn(order)))
    }

    @Test
    fun `随机顺序按实际播放先后数`() {
        // 当前是 3，随机顺序里它后面只剩 0
        val order = listOf(2, 4, 1, 3, 0)
        assertEquals(1, countUpcoming(3, 10, nextIn(order)))
        assertEquals(4, countUpcoming(2, 10, nextIn(order)))
    }

    @Test
    fun `最多数到上限就停`() {
        val order = (0 until 100).toList()
        assertEquals(3, countUpcoming(0, 3, nextIn(order)))
    }
}
