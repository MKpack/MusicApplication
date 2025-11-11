package com.example.musicapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.remote.model.Song
import com.example.musicapplication.ui.appNaviagtion.AppNavigation
import com.example.musicapplication.ui.appNaviagtion.NavigationViewModel
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import com.example.musicapplication.utils.StaticObjectMethod
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private val mContext: Context = this
//    private var startD: String = RouterConfig.LOGIN
    val navigationViewModel = NavigationViewModel()
    // 1. 状态：外部要播放的歌
//    private var externalSong by mutableStateOf<Pair<Song?, Uri?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

//        //test externalOpenTest()
//        val prefs = this.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
//        if (prefs.getString("access_token", null) != null
//            || prefs.getString("refresh_token", null) != null) {
//            startD = RouterConfig.MAINPAGE
//        }
        //是否从外界打开
        navigationViewModel.changeOpenExternalSong(parseIntent(intent))
        setContent {
            MusicApplicationTheme {
                AppNavigation(mContext, navigationViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //更新状态给compose重组
        navigationViewModel.changeOpenExternalSong(parseIntent(intent))
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
//        startD = RouterConfig.PLAYER
        return song to uri
    }

}