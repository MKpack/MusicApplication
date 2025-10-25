package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.ui.mainPage.BarItem

@Composable
fun CustomBottomBar(
    navController: NavController,
    items: List<BarItem>
) {
    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() // 得到底部 Insets 大小（Dp）
    Column {
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Color.Black)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomInset)
                .height(56.dp)
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val selected = navBackStackEntry?.destination?.hierarchy?.any {it.route == item.router} == true
                val tint = if (selected) Color.Red else Color.Gray

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            navController.navigate(item.router) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                            }
                        }
                ) {
                    ColorIcon(item.fill, fillColor = tint)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = stringResource(item.des), color = tint, fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview
@Composable
fun showCurrentBottomBar() {
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)
    CustomBottomBar(navController, items)
}