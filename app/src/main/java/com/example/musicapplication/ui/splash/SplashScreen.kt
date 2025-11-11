package com.example.musicapplication.ui.splash

import com.example.musicapplication.R
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.remote.model.Song
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    context: Context,
    externalSong: Pair<Song?, Uri?>?
) {
    val TAG = "SplashScreen"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0F805959)), // 品牌背景色
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.default_cover),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
    }
    LaunchedEffect(Unit) {
        delay(600)
        val prefs = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        //如果有登陆状态则直接splash->mainPage,无则splash->login
        if (prefs.getString("access_token", null) != null
            || prefs.getString("refresh_token", null) != null) {
            navController.navigate(RouterConfig.MAINPAGE) {
                popUpTo(RouterConfig.SPLASH) { inclusive = true}
            }
        } else {
            //无登陆状态,先登陆
            navController.navigate(RouterConfig.LOGIN) {
                popUpTo(RouterConfig.SPLASH) { inclusive = true}
            }
        }
    }
}