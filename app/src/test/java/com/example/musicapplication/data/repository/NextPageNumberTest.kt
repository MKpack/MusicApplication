package com.example.musicapplication.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class NextPageNumberTest {

    @Test
    fun `还没加载过时从第一页开始`() {
        assertEquals(1L, nextPageNumber(0, 0, 20))
    }

    @Test
    fun `每页数量没变时就是下一页`() {
        assertEquals(2L, nextPageNumber(1, 20, 20))
        assertEquals(4L, nextPageNumber(3, 20, 20))
    }

    @Test
    fun `每页数量改大后按已加载的数量换算，不跳过中间的歌`() {
        // 以前每页 5 首加载了 3 页（15 首）：第 1 页（20 首）还没加载完，重新加载第 1 页
        assertEquals(1L, nextPageNumber(3, 5, 20))
        // 加载了 4 页（20 首）：正好是新的第 1 页，接着加载第 2 页
        assertEquals(2L, nextPageNumber(4, 5, 20))
        assertEquals(2L, nextPageNumber(7, 5, 20))
    }
}
