package com.example.musicapplication.ui.appNaviagtion

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.ui.login.LoginEntry
import com.example.musicapplication.ui.login.LoginViewModel
import com.example.musicapplication.ui.mainPage.MainPage
import com.example.musicapplication.ui.mainPage.MainPageViewModel
import com.example.musicapplication.ui.mainPage.audioPlayer.FullScreenPlayer
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerViewModel
import com.example.musicapplication.ui.splash.SplashScreen


@Composable
fun AppNavigation(
    context: Context,
    navigationViewModel: NavigationViewModel
) {
    val TAG = "AppNavigation"
    val navController = rememberNavController()
    val externalSong by navigationViewModel.openExternalSong.collectAsState()
    NavHost(
        navController = navController,
        startDestination = RouterConfig.SPLASH
    ) {
        composable(RouterConfig.SPLASH) {
            SplashScreen(navController, context, externalSong)
        }
        composable(RouterConfig.LOGIN + "?redirect={redirect}") { navBackStackEntry ->
            val redirect = navBackStackEntry.arguments?.getString("redirect")
            val loginViewModel: LoginViewModel = hiltViewModel(navBackStackEntry)
            LoginEntry(loginViewModel, context, navController)
        }
        composable(RouterConfig.MAINPAGE) { navBackStackEntry ->
            val mainPageViewModel: MainPageViewModel = hiltViewModel(navBackStackEntry)
            MainPage(mainPageViewModel, context, navigationViewModel)
        }
        //不应该放在这边，这是mainPage中的业务，导致逻辑混乱
//        //新增外部打开
//        composable(RouterConfig.PLAYER) { navBackStackEntry ->
//            val playerViewModel: PlayerViewModel = hiltViewModel(navBackStackEntry)
//            Log.d(TAG, "externalSong: " + externalSong?.first.toString() + " " + externalSong?.second)
//            LaunchedEffect(externalSong) {
//                externalSong?.let { (song, uri)->
//                    if (uri != null && song != null) {
//                        playerViewModel.selectSong(
//                            MusicSource.Local(-1, uri), song
//                        )
//                    }
//                    navigationViewModel.changeOpenExternalSong(null)    //消费掉
//                }
//            }
//            FullScreenPlayer(navController, context, playerViewModel)
//        }
    }
}