package com.example.musicapplication.ui.mainPage

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainPage(
    mainPageViewModel: MainPageViewModel,
    context: Context
) {

    val accessToken by mainPageViewModel.accessToken.collectAsState()
    val refreshToken by mainPageViewModel.refreshToken.collectAsState()
    val testMsg by mainPageViewModel.testMsg.collectAsState()
    Column(
        modifier = Modifier.systemBarsPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("这是主页")
        Button(
            onClick = {mainPageViewModel.showToken(context)}
        ) {
            Text("显示token")
        }
        Text("accessToken: $accessToken")
        Text("refreshToken: $refreshToken")
        Button(
            onClick = { mainPageViewModel.refresh() }
        ) {
            Text("刷新accessToken")
        }
        Button(
            onClick = {
                mainPageViewModel.getTextMsg()
                mainPageViewModel.getTextMsg()
            }
        ) {
            Text("测试demo")
        }

        Text(text = testMsg)
    }
}