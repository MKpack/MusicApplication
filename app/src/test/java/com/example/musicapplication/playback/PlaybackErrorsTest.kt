package com.example.musicapplication.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException

class PlaybackErrorsTest {

    @Test
    fun `断网和超时只暂停不跳歌`() {
        listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_TIMEOUT
        ).forEach { assertEquals(PlaybackErrorKind.Network, classifyPlaybackError(it)) }
    }

    @Test
    fun `地址失效、文件不存在、格式不支持时跳到下一首`() {
        listOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        ).forEach { assertEquals(PlaybackErrorKind.Source, classifyPlaybackError(it)) }
    }

    @Test
    fun `其他错误暂停`() {
        listOf(
            PlaybackException.ERROR_CODE_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
        ).forEach { assertEquals(PlaybackErrorKind.Other, classifyPlaybackError(it)) }
    }

    @Test
    fun `文件不存在时不重试`() {
        assertTrue(isPermanentLoadError(FileNotFoundException("missing")))
        // 播放器包了一层，真正的原因在 cause 里
        assertTrue(isPermanentLoadError(IOException("open failed", FileNotFoundException("missing"))))
    }

    @Test
    fun `断网超时还是要重试`() {
        assertFalse(isPermanentLoadError(SocketTimeoutException()))
        assertFalse(isPermanentLoadError(IOException("connection refused")))
    }
}
