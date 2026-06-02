package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.ui.mainPage.BarItem
import com.example.musicapplication.ui.theme.MusicBorder
import com.example.musicapplication.ui.theme.MusicIconMuted
import com.example.musicapplication.ui.theme.MusicPrimary
import com.example.musicapplication.ui.theme.MusicPrimarySoft
import com.example.musicapplication.ui.theme.MusicSurface

@Composable
fun CustomBottomBar(
    navController: NavController,
    items: List<BarItem>
) {
    val bottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding() // 得到底部 Insets 大小（Dp）
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MusicSurface)
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = MusicBorder
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp + bottomInset)
                .padding(bottom = bottomInset)
                .background(MusicSurface),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val selected = navBackStackEntry?.destination?.
                    hierarchy?.any {it.route == item.router} == true
//                val tint = if (selected) Color.Red else Color.Gray

                BottomBarItem(
                    item = item,
                    selected = selected,
                    onClick = {
                        navController.navigate(item.router) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    }
}




@Composable
private fun BottomBarItem(
    item: BarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val itemColor = if (selected) MusicPrimary else MusicIconMuted
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 22.dp)
    ) {

        ColorIcon(item.fill, fillColor = itemColor)

        Spacer(modifier = Modifier.height(2.dp))

        Text(text = stringResource(item.des),
            color = itemColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}


@Preview
@Composable
fun showCurrentBottomBar() {
    val navController = rememberNavController()
    val items = listOf( BarItem.Home, BarItem.Profile)
    Column(modifier = Modifier.fillMaxHeight()) {
        CustomBottomBar(navController, items)
    }
}