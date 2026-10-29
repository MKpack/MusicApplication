package com.example.musicapplication.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.musicapplication.MainActivity
import com.example.musicapplication.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

/**
 * 真正播放音乐的地方。App 界面、通知栏、耳机线控都通过 MediaSession 控制这里的 ExoPlayer，
 * 播放队列也只存在这里，界面只是显示它。
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    companion object {
        private const val PLAYBACK_NOTIFICATION_ID = 2001
        private const val PLAYBACK_CHANNEL_ID = "music_playback"
        // 通知栏、锁屏封面最大边长，原图太大会占很多内存
        private const val ARTWORK_MAX_SIZE = 1024
    }

    @Inject
    @Named("media")
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var audioCache: AudioCache

    @Inject
    lateinit var coordinator: PlaybackCoordinator

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

        // 在线歌曲（带登录 token）边播边缓存；本地文件、content:// 直接读
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val cachedHttpDataSourceFactory = CacheDataSource.Factory()
            .setCache(audioCache.cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(this, cachedHttpDataSourceFactory))
                    .setLoadErrorHandlingPolicy(PlaybackLoadErrorPolicy())
            )
            // 处理音频焦点：来电、其他 App 放声音时暂停或降低音量
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            // 拔耳机、断开蓝牙耳机时暂停
            .setHandleAudioBecomingNoisy(true)
            // 锁屏后保持 CPU 和 Wi-Fi 唤醒，后台在线播放不中断
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // 点通知栏回到 App
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // 通知栏、锁屏的封面由这里自己下载（带 token），不需要把文件共享给系统
        val bitmapLoader = CacheBitmapLoader(
            DataSourceBitmapLoader(
                DataSourceBitmapLoader.DEFAULT_EXECUTOR_SERVICE.get(),
                DefaultDataSource.Factory(this, httpDataSourceFactory),
                null,
                ARTWORK_MAX_SIZE
            )
        )

        val session = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(bitmapLoader)
            .setCallback(sessionCallback)
            .build()
        mediaSession = session
        coordinator.attach(player, session)
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // 只有自己 App 的界面可以发「加载更多」，通知栏和其他 App 按默认处理
            if (controller.packageName != packageName || session.isMediaNotificationController(controller)) {
                return super.onConnect(session, controller)
            }
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(PlaybackCommands.LOAD_MORE)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == PlaybackCommands.ACTION_LOAD_MORE) {
                coordinator.loadMore()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 从最近任务里划掉 App：先保存进度；没在播放时默认会停止服务
        coordinator.persistNow()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        coordinator.detach()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null

        super.onDestroy()
    }
}
