package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig

@Composable
fun MiniPlayer(
    navController: NavController,
    context: Context,
    playerViewModel: PlayerViewModel
) {
    val isPlaying = remember { mutableStateOf(true) }
    val songTitle by playerViewModel.songTitle.collectAsState()
    val songBitmap by playerViewModel.songBitmap.collectAsState()
    Column(
        modifier = Modifier
            .clickable {
                navController.navigate(RouterConfig.PLAYER) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
    ) {
        HorizontalDivider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(Color.White)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //封面
                Image(
                    painter = painterResource(songBitmap),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(shape = RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                //歌曲标题
                Text(
                    text = songTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = { isPlaying.value = !isPlaying.value},
                    modifier = Modifier.size(30.dp)) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying.value)
                                R.drawable.pause_icon
                            else
                                R.drawable.play_icon
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.forward_solid_full),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
