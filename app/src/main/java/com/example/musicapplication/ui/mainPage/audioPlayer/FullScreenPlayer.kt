package com.example.musicapplication.ui.mainPage.audioPlayer

import com.example.musicapplication.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.musicapplication.ui.component.AppleMusicBackground
import com.example.musicapplication.ui.component.MusicProgressBar

fun getStaticBitmap(context: Context, resId: Int): Bitmap {
    val drawable = context.getDrawable(resId) as BitmapDrawable
    return drawable.bitmap
}
@Composable
fun FullScreenPlayer(
    navController: NavController,
    context: Context,
    playerViewModel: PlayerViewModel,
) {
    val colors by playerViewModel.dominantColors.collectAsState()
    val songTitle by playerViewModel.songTitle.collectAsState()
    val songSinger by playerViewModel.songSinger.collectAsState()
    LaunchedEffect(Unit) {
        val bitmap = getStaticBitmap(context = context, R.drawable.static_cover);
        playerViewModel.updateColorsFromBitmap(bitmap)
    }

    AppleMusicBackground(
        cover = R.drawable.static_cover,
        dominantColor = colors.firstOrNull() ?: Color.DarkGray,
        secondaryColor = colors.getOrNull(1) ?: Color.Black
    )
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 150.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.static_cover),
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        songTitle,
                        fontWeight = FontWeight.W600,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        songSinger,
                        fontWeight = FontWeight.W300,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.heart_solid_full),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ellipsis_vertical_solid_full),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 25.dp)
                .padding(horizontal = 30.dp),
        ) {
//            MusicProgressBar(currentProgress, onSeek = { playerViewModel.onSeek(it) })
            MusicProgressBar2(playerViewModel)
        }
    }
}