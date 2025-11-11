package com.example.musicapplication.ui.mainPage

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.ui.appNaviagtion.NavigationViewModel
import com.example.musicapplication.ui.component.CustomBottomBar
import com.example.musicapplication.ui.mainPage.audioPlayer.FullScreenPlayer
import com.example.musicapplication.ui.mainPage.audioPlayer.MiniPlayer
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerViewModel
import com.example.musicapplication.ui.mainPage.home.HomeScreen
import com.example.musicapplication.ui.mainPage.profile.ProfileScreen


//枚举式伴生写法
sealed class BarItem(
    val router: String,
    @StringRes val des: Int,
    val fill: ImageVector,
) {
    object Home: BarItem(RouterConfig.HOME, R.string.home, Icons.Filled.Home)
    object Profile: BarItem(RouterConfig.PROFILE, R.string.profile, Icons.Filled.Person)
}

@Composable
fun MainPage(
    mainPageViewModel: MainPageViewModel,
    context: Context,
    navigationViewModel: NavigationViewModel
) {
    val TAG = "MainPage"
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val externalSong by navigationViewModel.openExternalSong.collectAsState()

    LaunchedEffect(externalSong) {
        Log.d(TAG, "launchEffect")
        if (externalSong != null) {
            externalSong?.let { (song, uri) ->
                if (uri != null && song != null) {
                    playerViewModel.selectSong(
                        MusicSource.Local(-1, uri),
                        song
                    )
                }
                //消费掉
                navigationViewModel.changeOpenExternalSong(null)
            }
            navController.navigate(RouterConfig.PLAYER)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != RouterConfig.PLAYER) {
                Column {
                    MiniPlayer(navController, context, playerViewModel)
                    CustomBottomBar(navController, items)
                }
            }
        }
    ) { innerPadding->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = RouterConfig.HOME,
                modifier = Modifier.weight(1f)
            ) {
                composable(RouterConfig.HOME) { HomeScreen(modifier = Modifier.padding(innerPadding)) }
                composable(RouterConfig.PROFILE) { ProfileScreen() }
                composable(RouterConfig.PLAYER) {
                    FullScreenPlayer(navController, context, playerViewModel)
                }
            }
        }

    }
}
