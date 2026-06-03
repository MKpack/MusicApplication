package com.example.musicapplication.domain.player

import android.content.Context
import android.nfc.Tag
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicapplication.domain.model.MusicSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * 封装exoplayer用于播放自己的音乐
 */
@UnstableApi
class MusicPlayerManager(
    context: Context,
    okHttpClient: OkHttpClient,
    onMusicEnded: () -> Unit
) {
    private val TAG = "MusicPlayerManager"

    // 这样就可以处理网络和本地的音频请求了
    private val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
    private val dataSourceFactory = DefaultDataSource.Factory(
        context,
        httpDataSourceFactory
    )
    private val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(dataSourceFactory)
        )
        .build()

    // 播放位置 （当前已经播放的时长）
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    // 歌曲时长
    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // MusicId
    private val _currentMusicId = MutableStateFlow<Int?>(null)

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // indictor progress 进度
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress = _currentProgress.asStateFlow()

    // 如果有拖动状态不能更新progress值
    private val _isDragging = MutableStateFlow(false)
    val isDragging = _isDragging.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying)  startProgressUpdate()
                else    stopProgressUpdate()
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        val durationMs = player.duration
                        _duration.value = if (durationMs > 0) {
                            (durationMs / 1000).toInt()
                        } else {
                            0
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        resetProgress()
                        stopProgressUpdate()
                        onMusicEnded()
                    }
                    else -> { }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "player error: code=${error.errorCode}, name=${error.errorCodeName}, message=${error.message}", error)
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    _currentMusicId.value?.let { musicId ->
                        scope.launch {

                        }
                    }
                }
            }
        })
    }

    // 播放音乐
    fun playMusic(musicSource: MusicSource?) {
        if (musicSource == null) return
        _currentMusicId.value = musicSource.id
        resetProgress()
        stopProgressUpdate()
        scope.launch {
            val uri = when (musicSource) {
                is MusicSource.Remote -> {
                    Log.d(TAG, "remote url: ${musicSource.url}")
                    musicSource.url.toUri()
                }
                is MusicSource.Local -> {
                    Log.d(TAG, "local uri: ${musicSource.uri}")
                    musicSource.uri
                }
            }
            withContext(Dispatchers.Main) {
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.playWhenReady = true
            }
        }
    }

    // 暂停
    fun pause() {
        player.pause()
    }
    //恢复播放
    fun resume() {
        player.play()
    }


    fun pauseAtStart() {
        player.pause()
        player.seekTo(0)
        _isPlaying.value = false
        resetProgress()
        stopProgressUpdate()
    }

    //跳转到指定时间位置
    fun seekTo() = player.seekTo((_currentPosition.value * 1000).toLong())

    //释放资源
    fun release() {
        player.release()
        stopProgressUpdate()
        scope.cancel()
    }

    fun updatePosition(tmp: Float) {
        _currentProgress.value = tmp
        _currentPosition.value = (tmp * _duration.value).toInt()
    }

    //更新dragging状态
    fun isDraggingStatusChange(tmp: Boolean) {
        _isDragging.value = tmp
    }

    //更新进度条
    private fun startProgressUpdate() {
        //先取消，避免重复
        progressJob?.cancel()

        progressJob = scope.launch {
            while (true) {
                if (_isDragging.value) {
                    delay(100)
                    continue
                }
                val position = withContext(Dispatchers.Main) {
                    player.currentPosition / 1000
                }
                _currentPosition.value = position.toInt()
                _currentProgress.value = if (_duration.value > 0f) _currentPosition.value.toFloat() / _duration.value.toFloat() else 0f
                delay(500)
//                Log.d(TAG, " currentPosition" + _currentPosition.value + " duration" + _duration.value + " currentProgress" + _currentProgress.value)
            }
        }
    }
    //停止更新
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    // 清空进度条
    private fun resetProgress() {
        _currentProgress.value = 0f
        _currentPosition.value = 0
        _duration.value = 0
    }
}
