package com.example.musicapplication.playback

import android.util.Log
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicapplication.R
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val PLAYBACK_NOTIFICATION_ID = 2001
        private const val PLAYBACK_CHANNEL_ID = "music_playback"
    }

    @Inject
    @Named("media")
    lateinit var okHttpClient: OkHttpClient

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(PLAYBACK_NOTIFICATION_ID)
            .setChannelId(PLAYBACK_CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_notification_music)
            }
        setMediaNotificationProvider(notificationProvider)

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            httpDataSourceFactory
        )
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
            )
            .build()
            .apply {
                // 可以被打断和打断
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(audioAttributes, true)

                // 设置监听器回掉
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                    }

                    override fun onPlayWhenReadyChanged(
                        playWhenReady: Boolean,
                        reason: Int
                    ) {
                        Log.d(TAG, "onPlayWhenReadyChanged: $playWhenReady, reason=$reason")
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d(TAG, "onPlaybackStateChanged: $playbackState")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "onPlayerError: ${error.errorCodeName}", error)
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null

        super.onDestroy()
    }
}
