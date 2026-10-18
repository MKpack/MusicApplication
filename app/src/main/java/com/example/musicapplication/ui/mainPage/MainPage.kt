package com.example.musicapplication.ui.mainPage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.domain.model.MusicSource
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.component.backdropSource
import com.example.musicapplication.ui.component.rememberBackdrop
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerSheet
import com.example.musicapplication.ui.mainPage.audioPlayer.PlayerViewModel
import com.example.musicapplication.ui.mainPage.bottombar.BottomFade
import com.example.musicapplication.ui.mainPage.bottombar.LocalMainBottomInset
import com.example.musicapplication.ui.mainPage.bottombar.MainBottomBar
import com.example.musicapplication.ui.mainPage.bottombar.MainTab
import com.example.musicapplication.ui.mainPage.bottombar.rememberMainBottomBarState
import com.example.musicapplication.ui.mainPage.home.HomeScreen
import com.example.musicapplication.ui.mainPage.profile.ProfileRoute
import com.example.musicapplication.ui.mainPage.profile.ProfileScreen
import com.example.musicapplication.ui.mainPage.profile.about.AboutMusicScreen
import com.example.musicapplication.ui.mainPage.profile.account.ProfileAccountScreen
import com.example.musicapplication.ui.mainPage.profile.download.DownloadMusicScreen
import com.example.musicapplication.ui.mainPage.profile.favorite.FavoriteMusicScreen
import com.example.musicapplication.ui.mainPage.profile.recent.RecentMusicScreen
import com.example.musicapplication.ui.mainPage.profile.setting.ProfileSettingScreen
import com.example.musicapplication.ui.mainPage.search.SearchScreen
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.utils.LocalAudioMetaDataReader

/**
 * 登录后的主页面，从下往上叠四层：
 * 1. 页面内容（NavHost），铺满全屏，同时把画面记录到 backdrop，供上面的玻璃模糊；
 * 2. 底部渐隐；
 * 3. 悬浮的玻璃底栏（Tab 胶囊 + 搜索按钮），只在 Tab 页显示；
 * 4. 播放器：迷你玻璃胶囊 ↔ 全屏，迷你状态的位置由底栏状态决定。
 */
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // 第一帧返回栈还是空的，这时按起始页（发现）算
    val currentRoute = navBackStackEntry?.destination?.route ?: RouterConfig.HOME
    val currentTab = MainTab.fromRoute(currentRoute)

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

    val backdrop = rememberBackdrop()
    val barState = rememberMainBottomBarState()
    // 进入二级页面时底栏还要显示上一个 Tab 的样子（它正在滑出屏幕）
    var lastTab by remember { mutableStateOf(MainTab.HOME) }
    LaunchedEffect(currentTab) {
        barState.setVisible(currentTab != null)
        if (currentTab != null) {
            lastTab = currentTab
            // 换了 Tab 或者从二级页面回来，底栏恢复展开
            barState.expand()
        }
    }
    val barHidden by remember { derivedStateOf { barState.visibility <= 0f } }

    // 播放器展开的进度（0 = 迷你，1 = 全屏）。拖动时每一帧都在变，只在布局、绘制阶段读，整页不用跟着重组
    val playerProgress = remember { mutableFloatStateOf(0f) }
    var collapsePlayerRequest by remember { mutableIntStateOf(0) }
    val isPlayerExpanded by remember { derivedStateOf { playerProgress.floatValue > 0.03f } }
    val isPlayerFullScreen by remember { derivedStateOf { playerProgress.floatValue > 0.98f } }

    val pageBackground = LocalMusicThemeColors.current.bgBottom
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalMainBottomInset provides barState.contentInset(navigationBarInset)) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = RouterConfig.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    // 任何页面的列表滚动都会传到这里：往下翻收起底栏，往上翻展开
                    .nestedScroll(barState.nestedScrollConnection)
                    .backdropSource(backdrop)
                    // 记录下来的画面要是不透明的，否则玻璃会透出更下层的东西
                    .background(pageBackground)
            ) {
                composable(RouterConfig.HOME) {
                    HomeScreen(
                        modifier = Modifier.statusBarsPadding(),
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
                        modifier = Modifier.statusBarsPadding(),
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

            // 播放器全屏时底栏完全被盖住，不用再画
            if (!barHidden && !isPlayerFullScreen) {
                val barAlpha = { (barState.visibility * (1f - playerProgress.floatValue * 2.2f)).coerceIn(0f, 1f) }
                BottomFade(
                    backdrop = backdrop,
                    color = pageBackground,
                    alpha = barAlpha,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                MainBottomBar(
                    state = barState,
                    selectedTab = currentTab ?: lastTab,
                    onTabSelected = { tab ->
                        if (tab == currentTab) barState.expand() else navController.navigateToTab(tab)
                    },
                    onSearchClick = {
                        navController.navigate(RouterConfig.SEARCH)
                    },
                    backdrop = backdrop,
                    playerProgress = { playerProgress.floatValue },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            PlayerSheet(
                context = context,
                playerViewModel = playerViewModel,
                miniAnchor = { screenWidth, screenHeight ->
                    barState.miniPlayerAnchor(screenWidth, screenHeight, navigationBarInset)
                },
                backdrop = backdrop,
                collapseRequest = collapsePlayerRequest,
                onProgressChange = { progress ->
                    playerProgress.floatValue = progress
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            )
        }
    }
}

/** 切换 Tab：每个 Tab 保留自己的返回栈和滚动位置，切回来时恢复 */
private fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
    }
}
