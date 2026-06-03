package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.R
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 已弃用
 */
@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    context: Context,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val songValue by playerViewModel.songUiState.collectAsState()
//    val songTitle by playerViewModel.songTitle.collectAsState()
//    val songCover by playerViewModel.songCover.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
//            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .then(dragModifier)
                .clip(RoundedCornerShape(22.dp))
                .background(LocalMusicThemeColors.current.playerSurface)
                .border(
                    width = 1.dp,
                    color = LocalMusicThemeColors.current.border,
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable {
                    onClick()
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = songValue.cover,
                    placeholder = painterResource(R.drawable.default_cover),
                    error = painterResource(R.drawable.default_cover)
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = songValue.songTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    color = LocalMusicThemeColors.current.textPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = songValue.singer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = LocalMusicThemeColors.current.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            MiniPlayerIconButton(
                icon = if (isPlaying) R.drawable.ic_pause_mini else R.drawable.ic_play_mini,
                tint = LocalMusicThemeColors.current.primary,
                iconSize = 30.dp,
                onClick = {
                    if (isPlaying) {
                        playerViewModel.pause()
                    } else {
                        playerViewModel.resume()
                    }
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            MiniPlayerIconButton(
                icon = R.drawable.ic_next_mini,
                tint = LocalMusicThemeColors.current.primary,
                iconSize = 30.dp,
                onClick = { }
            )
        }
    }
}



@Composable
private fun MiniPlayerIconButton(
    @DrawableRes icon: Int,
    tint: Color,
    iconSize: Dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
