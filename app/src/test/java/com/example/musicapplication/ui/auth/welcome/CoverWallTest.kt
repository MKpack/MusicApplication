package com.example.musicapplication.ui.auth.welcome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverWallTest {

    @Test
    fun columns_neighborColumnsNeverShareCovers() {
        val columns = coverColumns((0 until 16).toList())

        assertEquals(4, columns.size)
        columns.forEach { assertEquals(8, it.size) }
        // 相邻两列不会出现同一张封面
        for (i in 0 until columns.size - 1) {
            assertTrue(columns[i].intersect(columns[i + 1].toSet()).isEmpty())
        }
    }

    @Test
    fun columns_adjacentTilesInColumnDiffer() {
        // 每列上下相邻的两张不重复，首尾相接循环时也不重复
        coverColumns((0 until 16).toList()).forEach { column ->
            column.indices.forEach { i -> assertNotEquals(column[i], column[(i + 1) % column.size]) }
        }
    }

    @Test
    fun columns_fewCovers_stillFillEveryTile() {
        val columns = coverColumns(listOf("a", "b", "c"))

        columns.forEach { assertEquals(8, it.size) }
        assertTrue(columns.flatten().all { it in setOf("a", "b", "c") })
    }

    @Test
    fun columns_noCovers_emptyColumns() {
        assertTrue(coverColumns(emptyList<Int>()).all { it.isEmpty() })
    }
}
