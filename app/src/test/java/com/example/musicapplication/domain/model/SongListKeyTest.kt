package com.example.musicapplication.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongListKeyTest {

    @Test
    fun `value 能还原成原来的列表`() {
        listOf(
            SongListKey.Hot,
            SongListKey.Loved,
            SongListKey.Playlist(42),
            SongListKey.Search("周杰伦"),
            SongListKey.Search("a_b c")
        ).forEach { key ->
            assertEquals(key, SongListKey.fromValue(key.value))
        }
    }

    @Test
    fun `认不出来的值返回 null`() {
        assertNull(SongListKey.fromValue(null))
        assertNull(SongListKey.fromValue(""))
        assertNull(SongListKey.fromValue("recent"))
        assertNull(SongListKey.fromValue("playlist_abc"))
    }
}
