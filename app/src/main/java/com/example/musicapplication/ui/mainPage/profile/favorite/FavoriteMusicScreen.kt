package com.example.musicapplication.ui.mainPage.profile.favorite

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.musicapplication.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.component.MusicCollectionScaffold
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteMusicScreen(
    onBack: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    favoriteMusicViewModel: FavoriteMusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val songListUiState by favoriteMusicViewModel.songListUiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(songListUiState.errorMsg) {
        val message = songListUiState.errorMsg ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        favoriteMusicViewModel.consumeErrorMessage()
    }

    PullToRefreshBox(
        isRefreshing = songListUiState.isRefreshing,
        onRefresh = {
            favoriteMusicViewModel.refreshLovedSongs()
        },
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            FavoriteRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = songListUiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        MusicCollectionScaffold(
            title = "喜欢的歌曲",
            tag = "喜欢的音乐会显示在这里",
            emptyTitle = "还没有喜欢的歌曲",
            emptyDescription = "在播放页点击喜欢后，歌曲会出现在这里。",
            icon = Icons.Default.Favorite,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
            playAllEnabled = songListUiState.songs.isNotEmpty(),
            onPlayAllClick = {
                if (songListUiState.songs.isNotEmpty()) {
                    onSongClick(songListUiState.songs, 0)
                }
            },
            content = if (songListUiState.songs.isEmpty()) {
                null
            } else {
                { _ ->
                    items(
                        items = songListUiState.songs,
                        key = { it.songId }
                    ) { song ->
                        FavoriteSongRow(
                            song = song,
                            onClick = {
                                onSongClick(songListUiState.songs, songListUiState.songs.indexOf(song))
                            },
                            onFavoriteClick = {
                                favoriteMusicViewModel.doFavoriteEvent(songListUiState.songs.indexOf(song))
                                favoriteMusicViewModel.refreshLovedSongs()
                            }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val offsetY = with(density) {
        val hiddenY = -(statusBarTop + 42.dp).toPx()
        val shownY = (statusBarTop + 18.dp).toPx()
        val progress = state.distanceFraction.coerceIn(0f, 1f)

        if (isRefreshing) {
            shownY
        } else {
            hiddenY + (shownY - hiddenY) * progress
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY
                alpha = if (isRefreshing || state.distanceFraction > 0f) 1f else 0f
            }
            .size(36.dp)
            .clip(CircleShape)
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = LocalMusicThemeColors.current.primary,
                strokeWidth = 2.5.dp
            )
        } else {
            CircularProgressIndicator(
                progress = { state.distanceFraction.coerceIn(0f, 1f) },
                color = LocalMusicThemeColors.current.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FavoriteSongRow(
    song: Song,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = song.cover,
                placeholder = painterResource(R.drawable.default_cover),
                error = painterResource(R.drawable.default_cover)
            ),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.songTitle,
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.singer,
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(
                    if (song.isLoved) {
                        R.drawable.heart_solid_full
                    } else {
                        R.drawable.heart_regular_full
                    }
                ),
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
