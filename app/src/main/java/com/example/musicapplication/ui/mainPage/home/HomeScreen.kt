package com.example.musicapplication.ui.mainPage.home

import android.content.Context
import android.widget.Toast
import com.example.musicapplication.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter


private data class PlaylistUi(
    val title: String,
    val desc: String,
    val cover: String?
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier,
    onSongClick: (listKey: SongListKey, songs: List<Song>, index: Int) -> Unit,
    onSearchClick: () -> Unit,
    context: Context,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val songList by homeScreenViewModel.songListUiState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    // 可以获取lazyColumn的快照看是否存在item
    val listState = rememberLazyListState()

    LaunchedEffect(songList.errorMsg) {
        val message = songList.errorMsg ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        homeScreenViewModel.consumeErrorMsg()
    }

    LaunchedEffect(listState, songList.songs) {
        snapshotFlow {
            val triggerIndex = (songList.songs.lastIndex - 2).coerceAtLeast(0)
            val triggerSongId = songList.songs.getOrNull(triggerIndex)?.songId ?: return@snapshotFlow false

            val triggerKey = "hot_$triggerSongId"

            listState.layoutInfo.visibleItemsInfo.any {
                it.key == triggerKey
            }
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                homeScreenViewModel.loadMoreHotSongs()
            }
    }

    PullToRefreshBox(
        isRefreshing = songList.isRefreshing,
        onRefresh = {
            homeScreenViewModel.refreshHotSongs(isPullToRefresh = true)
        },
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            DiscoverRefreshIndicator(
                state = pullToRefreshState,
                isRefreshing = songList.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
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
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 150.dp
            ),
//            verticalArrangement = Arrangement.spacedBy(22.dp),
            state = listState
        ) {
            item {
                DiscoverHeader()
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
            }

            item {
                SearchBar(onSearchClick)
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
            }

//            item {
//                // TODO
//                RandomPlayCard()
//            }

            item {
                SectionHeader(title = "推荐歌单", action = "查看全部")

                Spacer(modifier = Modifier.height(12.dp))

                Text("敬请期待")

//                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
//                    items(playlists) { playlist ->
//                        PlaylistCard(playlist = playlist)
//                    }
//                }
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
            }

            item {
                SectionHeader(title = "热门歌曲", action = "更多")
            }

            item {
                Spacer(modifier = Modifier.height(22.dp))
            }

            itemsIndexed(
                items = songList.songs,
                key = { _, song -> "hot_${song.songId}" }
            ) { index, song ->
                val shape = when {
                    songList.songs.size == 1 -> RoundedCornerShape(20.dp)
                    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    index == songList.songs.size - 1 -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(LocalMusicThemeColors.current.surface)
                ) {
                    SongRow(
                        song = song,
                        onClick = {
                            onSongClick(SongListKey.Hot, songList.songs, index)
                        },
                        onFavoriteSong = {
                            homeScreenViewModel.doFavoriteEvent(index)
                        }
                    )

                    if (index != songList.songs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = LocalMusicThemeColors.current.border,
                            thickness = 1.dp
                        )
                    }
                }
            }

            /**
             * 最下方如果在下滑还没拿到数据就转圈 --> 表示等待
             */
            if (songList.isLoading && songList.songs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = LocalMusicThemeColors.current.primary,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            } else if (songList.isEndReached && songList.songs.isNotEmpty()) {
                item {
                    Text(
                        text = "已经到底了",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        color = LocalMusicThemeColors.current.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverRefreshIndicator(
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
private fun DiscoverHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "发现",
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "今天想听什么？",
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 14.sp
            )
        }

        IconButton(
            onClick = {},
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.iconMuted,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}


@Composable
private fun SearchBar(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(
                width = 1.dp,
                color = LocalMusicThemeColors.current.border,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = interactionSource
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.search_solid_full),
            contentDescription = "搜索",
            tint = LocalMusicThemeColors.current.iconMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "搜索歌曲、歌手、歌单",
            color = LocalMusicThemeColors.current.textHint,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun RandomPlayCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LocalMusicThemeColors.current.primarySoft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            null,
            modifier = Modifier.size(88.dp),
            corner = 18
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "随机播放",
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "发现属于你的音乐惊喜",
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play_icon),
                contentDescription = "播放",
                tint = LocalMusicThemeColors.current.surface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CoverImage(
    cover: String?,
    modifier: Modifier,
    corner: Int
) {
    Image(
        painter = rememberAsyncImagePainter(
            model = cover,
            placeholder = painterResource(R.drawable.default_cover),
            error = painterResource(R.drawable.default_cover)
        ),
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(corner.dp)),
        contentScale = ContentScale.Crop
    )
}


@Composable
private fun SectionHeader(
    title: String,
    action: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = action,
            color = LocalMusicThemeColors.current.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistUi) {
    Column(
        modifier = Modifier.width(112.dp)
    ) {
        CoverImage(
            cover = playlist.cover,
            modifier = Modifier.size(112.dp),
            corner = 18
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.title,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = playlist.desc,
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onFavoriteSong: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(
            cover = song.cover,
            modifier = Modifier.size(48.dp),
            corner = 12
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.songTitle,
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = song.singer,
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = {
            onFavoriteSong()
        }) {
            Icon(
                painter = painterResource(
                    if (song.isLoved) R.drawable.heart_solid_full
                    else R.drawable.heart_regular_full
                ),
                contentDescription = null,
                tint = if (song.isLoved) LocalMusicThemeColors.current.primary else LocalMusicThemeColors.current.iconMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

