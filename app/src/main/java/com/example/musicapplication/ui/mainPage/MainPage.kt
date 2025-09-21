package com.example.musicapplication.ui.mainPage

import android.content.Context
import android.graphics.drawable.Icon
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.ui.component.TwoColorIcon
import com.example.musicapplication.ui.mainPage.home.HomeScreen
import com.example.musicapplication.ui.mainPage.profile.ProfileScreen
import kotlinx.coroutines.flow.collectLatest

//枚举式伴生写法
sealed class BarItem(
    val router: String,
    @StringRes val des: Int,
    val fill: ImageVector,
    val outlined: ImageVector
) {
    object Home: BarItem(RouterConfig.HOME, R.string.home, Icons.Filled.Home, Icons.Outlined.Home)
    object Profile: BarItem(RouterConfig.PROFILE, R.string.profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainPage(
    mainPageViewModel: MainPageViewModel,
    context: Context
) {
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.router } == true
                    val tint by animateColorAsState(
                        targetValue = if (selected) Color.Black
                                        else Color.White,
                        animationSpec = tween(120)
                    )
                    NavigationBarItem(
                        icon = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy((-4).dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TwoColorIcon(item.fill, item.outlined, fillColor = tint)
                                Text(stringResource(item.des))
                            }
                               },
                        label = {  },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.router) {
                                //弹出策略,saveState=true保存状态
                                popUpTo(navController.graph.findStartDestination().id) {
                                     saveState = true
                                }
                                //单顶方式
                                launchSingleTop = true
                                //恢复状态,与前面saveState=true成对出现
                                restoreState = true
                            }
                        },
                        //选中后没有选中背景色
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding->
        NavHost(
            navController = navController,
            startDestination = RouterConfig.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RouterConfig.HOME) { HomeScreen() }
            composable(RouterConfig.PROFILE) { ProfileScreen() }
        }
    }
}
