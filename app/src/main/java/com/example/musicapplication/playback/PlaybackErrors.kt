package com.example.musicapplication.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.FileNotFoundException

/** 播放出错以后怎么处理 */
enum class PlaybackErrorKind {
    // 网络断了、超时：换下一首也一样放不了，暂停等用户处理
    Network,
    // 这首歌本身有问题（地址失效、文件被删、格式不支持）：跳到下一首
    Source,
    // 其他错误（比如音频设备出错）：暂停
    Other
}

fun classifyPlaybackError(errorCode: Int): PlaybackErrorKind = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_TIMEOUT -> PlaybackErrorKind.Network

    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
    PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> PlaybackErrorKind.Source

    else -> PlaybackErrorKind.Other
}

/**
 * 加载失败时的重试规则：文件不存在、服务器明确拒绝（4xx）时重试也没用，直接报错让播放器跳到下一首，
 * 不用白等几秒；断网、超时、服务器 5xx 还是按默认规则重试几次。
 */
@OptIn(UnstableApi::class)
class PlaybackLoadErrorPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        if (isPermanentLoadError(loadErrorInfo.exception)) return C.TIME_UNSET
        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}

@OptIn(UnstableApi::class)
internal fun isPermanentLoadError(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.any { cause ->
        cause is FileNotFoundException ||
            (cause is HttpDataSource.InvalidResponseCodeException &&
                cause.responseCode in 400..499 &&
                cause.responseCode != 408 &&
                cause.responseCode != 429)
    }
