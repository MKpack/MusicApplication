package com.example.musicapplication.ui.splash

import com.example.musicapplication.R
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
        delay(800)
        withFrameNanos { }
        val hasLoginState = prefs.getString("access_token", null) != null ||
            prefs.getString("refresh_token", null) != null

        if (hasLoginState) {
            navController.navigate(RouterConfig.MAINPAGE) {
                popUpTo(RouterConfig.SPLASH) { inclusive = true}
            }
        } else {
            navController.navigate(RouterConfig.LOGIN) {
                popUpTo(RouterConfig.SPLASH) { inclusive = true}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LocalMusicThemeColors.current.bgTop,
                        LocalMusicThemeColors.current.bgBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(LocalMusicThemeColors.current.primary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_music),
                    contentDescription = "Music App",
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Music App",
                color = LocalMusicThemeColors.current.textPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Feel the music, your way",
                color = LocalMusicThemeColors.current.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(44.dp))

//            CircularProgressIndicator(
//                modifier = Modifier.size(26.dp),
//                color = Color(0xFFFF5148),
//                strokeWidth = 2.5.dp
//            )
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = LocalMusicThemeColors.current.primary,
                trackColor = LocalMusicThemeColors.current.primarySoft
            )
        }
    }
}
