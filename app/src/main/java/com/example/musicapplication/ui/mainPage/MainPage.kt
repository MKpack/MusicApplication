package com.example.musicapplication.ui.mainPage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.ui.component.CustomBottomBar
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerSheet
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerViewModel
import com.example.musicapplication.ui.mainPage.audioPlayer.smoothProgress
import com.example.musicapplication.ui.mainPage.home.HomeScreen
import com.example.musicapplication.ui.mainPage.profile.ProfileRoute
import com.example.musicapplication.ui.mainPage.profile.ProfileScreen
import com.example.musicapplication.ui.mainPage.profile.about.AboutMusicScreen
import com.example.musicapplication.ui.mainPage.profile.download.DownloadMusicScreen
import com.example.musicapplication.ui.mainPage.profile.favorite.FavoriteMusicScreen
import com.example.musicapplication.ui.mainPage.profile.recent.RecentMusicScreen
import com.example.musicapplication.ui.mainPage.profile.account.ProfileAccountScreen
import com.example.musicapplication.ui.mainPage.profile.setting.ProfileSettingScreen
import com.example.musicapplication.ui.mainPage.search.SearchScreen
import com.example.musicapplication.ui.session.SessionViewModel
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.utils.LocalAudioMetaDataReader


//枚举式伴生写法
sealed class BarItem(
    val router: String,
    @StringRes val des: Int,
    val fill: ImageVector,
) {
    object Home: BarItem(RouterConfig.HOME, R.string.home, Icons.Filled.Home)
    object Profile: BarItem(RouterConfig.PROFILE, R.string.profile, Icons.Filled.Person)
}

@OptIn(UnstableApi::class)
@Composable
fun MainPage(
    mainPageViewModel: MainPageViewModel,
    context: Context,
    externalAudioUri: Uri?,
    onExternalAudioConsumed: () -> Unit,
    onClickLogout: () -> Unit
) {
    val TAG = "MainPage"
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerViewModel: PlayerViewModel = hiltViewModel()

    /**
     * 判断外部音频
     */
    LaunchedEffect(externalAudioUri) {
        val uri = externalAudioUri ?: return@LaunchedEffect
        Log.d(TAG, "consume external audio uri: $uri")

        val song = runCatching {
            LocalAudioMetaDataReader.getStaticAudioInfo(uri, context)
        }.getOrElse {
            Song(
                songId = -1,
                songTitle = uri.lastPathSegment ?: "未知歌曲",
                singer = "未知歌手",
                isLoved = false,
                cover = null,
                source = MusicSource.Local(-1, uri)
            )
        }

        playerViewModel.playSong(
            song
        )
        onExternalAudioConsumed()
    }

    var playerProgress by remember { mutableFloatStateOf(0f) }
    var collapsePlayerRequest by remember { mutableIntStateOf(0) }

    // 1. 定义判断状态
    val isPlayerExpanded = playerProgress > 0.03f // 播放器是否展开

    val navigationBarPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val bottomBarHeight = 64.dp + navigationBarPadding


    val bottomRoutes = setOf(
        RouterConfig.HOME,
        RouterConfig.PROFILE
    )
    val shouldShowBottomBar = currentRoute in bottomRoutes

    val playerBottomPadding = if (shouldShowBottomBar) {
        bottomBarHeight
    } else {
        navigationBarPadding + 14.dp
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            bottomBar = {
                if (shouldShowBottomBar && playerProgress < 0.98f) {
                    val bottomBarAlpha = (1f - playerProgress * 2.2f).coerceIn(0f, 1f)
                    val bottomBarOffset = 28.dp * smoothProgress(playerProgress, 0f, 0.45f)

                    Box(
                        modifier = Modifier
                            .alpha(bottomBarAlpha)
                            .offset(y = bottomBarOffset)
                    ) {
                        CustomBottomBar(navController, items)
                    }
                }
            },
            containerColor = LocalMusicThemeColors.current.bgBottom
        ) { innerPadding ->

            NavHost(
                navController = navController,
                startDestination = RouterConfig.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(RouterConfig.HOME) {
                    HomeScreen(
                        modifier = Modifier.padding(
                            innerPadding
                        ),
                        context = context,
                        onSongClick = { listKey, songs, index ->
                            playerViewModel.playQueueSong(listKey, songs, index)
                        },
                        onSearchClick = {
                            navController.navigate(RouterConfig.SEARCH)
                        }
                    )
                }
                composable(RouterConfig.SEARCH) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { listKey, songs, index ->
                            if (listKey == null) {
                                playerViewModel.playQueueSong(songs, index)
                            } else {
                                playerViewModel.playQueueSong(listKey, songs, index)
                            }
                        }
                    )
                }
                composable(RouterConfig.PROFILE) {
                    ProfileScreen(
                        modifier = Modifier.padding(innerPadding),
                        onClickAccount = {
                            navController.navigate(ProfileRoute.PROFILE_ACCOUNT)
                        },
                        onClickDownload = {
                            navController.navigate(ProfileRoute.PROFILE_DOWNLOAD)
                        },
                        onClickFavorite = {
                            navController.navigate(ProfileRoute.PROFILE_FAVORITE)
                        },
                        onClickHistory = {
                            navController.navigate(ProfileRoute.PROFILE_HISTORY)
                        },
                        onClickSetting = {
                            navController.navigate(ProfileRoute.PROFILE_SETTING)
                        },
                        onClickAbout = {
                            navController.navigate(ProfileRoute.PROFILE_ABOUT)
                        },
                        onClickLogout = {
                            onClickLogout()
                        }
                    )
                }
                composable(ProfileRoute.PROFILE_ACCOUNT) {
                    ProfileAccountScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(ProfileRoute.PROFILE_DOWNLOAD) {
                    DownloadMusicScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { songs, index ->
                            playerViewModel.playQueueSong(songs, index)
                        }
                    )
                }
                composable(ProfileRoute.PROFILE_FAVORITE) {
                    FavoriteMusicScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { listKey, songs, index ->
                            playerViewModel.playQueueSong(listKey, songs, index)
                        }
                    )
                }
                composable(ProfileRoute.PROFILE_HISTORY) {
                    RecentMusicScreen(
                        onBack = { navController.popBackStack() },
                        onSongClick = { songs, index ->
                            playerViewModel.playQueueSong(songs, index)
                        }
                    )
                }
                composable(ProfileRoute.PROFILE_SETTING) {
                    ProfileSettingScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = onClickLogout
                    )
                }
                composable(ProfileRoute.PROFILE_ABOUT) {
                    AboutMusicScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            // latest register, highest grade
            BackHandler(enabled = isPlayerExpanded) {
                collapsePlayerRequest++
                Log.d(TAG, "播放器处于展开状态，拦截一切页面回退，强制折叠！ -> $collapsePlayerRequest")
            }
        }
        PlayerSheet(
            context = context,
            playerViewModel = playerViewModel,
            bottomBarPadding = playerBottomPadding,
            collapseRequest = collapsePlayerRequest,
            onProgressChange = { progress ->
                playerProgress = progress
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        )
    }
}
