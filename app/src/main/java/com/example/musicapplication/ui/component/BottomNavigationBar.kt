package com.example.musicapplication.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.ui.mainPage.BarItem

@Composable
fun BottomNavigationBar(
    navController: NavController,
    items: List<BarItem>
) {
    //获取回退栈的顶部页面
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.router } == true
            val tint by animateColorAsState(
                targetValue = if (selected) Color.Red
                else Color.Gray,
                animationSpec = tween(120)
            )
            NavigationBarItem(
                icon = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy((-4).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ColorIcon(item.fill, fillColor = tint)
                        Text(stringResource(item.des), color = tint)
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

@Preview
@Composable
fun showBottomNavigationBar() {
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)
    BottomNavigationBar(navController, items)
}