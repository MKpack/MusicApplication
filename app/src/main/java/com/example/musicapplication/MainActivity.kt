package com.example.musicapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.remote.model.Song
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.ui.login.LoginEntry
import com.example.musicapplication.ui.login.LoginViewModel
import com.example.musicapplication.ui.mainPage.MainPage
import com.example.musicapplication.ui.mainPage.MainPageViewModel
import com.example.musicapplication.ui.mainPage.audioPlayer.FullScreenPlayer
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerViewModel
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import com.example.musicapplication.utils.StaticObjectMethod
import dagger.hilt.android.AndroidEntryPoint
import kotlin.toString


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val mContext: Context = this
    private var startD: String = RouterConfig.LOGIN

    // 1. 状态：外部要播放的歌
    private var externalSong by mutableStateOf<Pair<Song?, Uri?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //test externalOpenTest()
        val prefs = this.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        if (prefs.getString("access_token", null) != null
            || prefs.getString("refresh_token", null) != null)
        {
            startD = RouterConfig.MAINPAGE
        }

        //是否从外界打开
        externalSong = parseIntent(intent)

        setContent {
            MusicApplicationTheme {
                AppNavigation(mContext, startD, externalSong,
                    onConsumed = { externalSong = null } ) // 消费掉，避免反复播放)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //更新状态给compose重组
        externalSong = parseIntent(intent)
    }

    //    fun externalOpenTest() {
//        val action = this.intent.action
//        val uri = intent.data
//        val type = intent.type
//        Log.d(TAG, "action: " + action + " uri: " + uri.toString() + " type: " + type)
//   2025-11-08 16:48:57.617 23860-23860 MainActivity
//   com.example.musicapplication   D
//   action: android.intent.action.VIEW
//   uri: content://com.android.fileexplorer.myprovider/external_files/xiangnideye.mp3
//   type: audio/*
//    }

    /** 把 Intent 里可能的歌曲信息解析出来 */
    private fun parseIntent(intent: Intent): Pair<Song?, Uri?>? {
        if (intent.action != Intent.ACTION_VIEW || intent.data == null) return null
        val uri = intent.data ?: return null
        val song = StaticObjectMethod.getStaticAudioInfo(uri, this)
        startD = RouterConfig.PLAYER
        return song to uri
    }

}


@Composable
fun AppNavigation(
    context: Context,
    startD: String,
    externalSong: Pair<Song?, Uri?>?,
    onConsumed: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = startD
    ) {
        composable(RouterConfig.LOGIN) { navBackStackEntry ->
            val loginViewModel: LoginViewModel = hiltViewModel(navBackStackEntry)
            LoginEntry(loginViewModel, context, navController)
        }
        composable(RouterConfig.MAINPAGE) { navBackStackEntry ->
            val mainPageViewModel: MainPageViewModel = hiltViewModel(navBackStackEntry)
            MainPage(mainPageViewModel, context)
        }
        //新增外部打开
        composable(RouterConfig.PLAYER) { navBackStackEntry ->
            val playerViewModel: PlayerViewModel = hiltViewModel(navBackStackEntry)
            LaunchedEffect(externalSong) {
                externalSong?.let { (song, uri)->
                    if (uri != null && song != null) {
                        playerViewModel.selectSong(
                            MusicSource.Local(-1, uri), song
                        )
                    }
                    onConsumed()          // 标记已消费
                }
            }
            FullScreenPlayer(navController, context, playerViewModel)
        }
    }
}