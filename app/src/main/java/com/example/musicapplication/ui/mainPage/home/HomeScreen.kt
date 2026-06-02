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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.theme.MusicBgBottom
import com.example.musicapplication.ui.theme.MusicBgTop
import com.example.musicapplication.ui.theme.MusicBorder
import com.example.musicapplication.ui.theme.MusicIconMuted
import com.example.musicapplication.ui.theme.MusicPrimary
import com.example.musicapplication.ui.theme.MusicPrimarySoft
import com.example.musicapplication.ui.theme.MusicSurface
import com.example.musicapplication.ui.theme.MusicTextHint
import com.example.musicapplication.ui.theme.MusicTextPrimary
import com.example.musicapplication.ui.theme.MusicTextSecondary


private data class PlaylistUi(
    val title: String,
    val desc: String,
    val cover: String?
)
@Composable
fun HomeScreen(
    modifier: Modifier,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    context: Context,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val songList by homeScreenViewModel.songListUiState.collectAsState()

    LaunchedEffect(songList.errorMsg) {
        val message = songList.errorMsg ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        homeScreenViewModel.consumeErrorMsg()
    }

    val playlists = listOf(
        PlaylistUi("通勤节奏", "28 首歌曲", null),
        PlaylistUi("午后放松", "35 首歌曲", null),
        PlaylistUi("学习专注", "42 首歌曲", null)
    )


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MusicBgTop,
                        MusicBgBottom
                    )
                )
            ),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 150.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            DiscoverHeader()
        }

        item {
            // TODO 新增一个搜索页
            SearchBar({})
        }

        item {
            // TODO
            RandomPlayCard()
        }

        item {
            SectionHeader(title = "推荐歌单", action = "查看全部")

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(playlists) { playlist ->
                    PlaylistCard(playlist = playlist)
                }
            }
        }

        item {
            SectionHeader(title = "热门歌曲", action = "更多")

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MusicSurface)
                    .padding(vertical = 6.dp)
            ) {
                songList.songs.forEachIndexed { index, song ->
                    SongRow(
                        song = song,
                        onClick = {
                            onSongClick(songList.songs, index)
                        },
                        onFavoriteSong = {
                            homeScreenViewModel.doFavoriteEvent(index)
                        }
                    )

                    if (index != songList.songs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MusicBorder,
                            thickness = 1.dp
                        )
                    }
                }
            }
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
                color = MusicTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "今天想听什么？",
                color = MusicTextSecondary,
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
                tint = MusicIconMuted,
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
            .background(MusicSurface)
            .border(
                width = 1.dp,
                color = MusicBorder,
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
            tint = MusicIconMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "搜索歌曲、歌手、歌单",
            color = MusicTextHint,
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
            .background(MusicPrimarySoft)
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
                color = MusicTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "发现属于你的音乐惊喜",
                color = MusicTextSecondary,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MusicPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.play_icon),
                contentDescription = "播放",
                tint = MusicSurface,
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
            color = MusicTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = action,
            color = MusicPrimary,
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
            color = MusicTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = playlist.desc,
            color = MusicTextSecondary,
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
                color = MusicTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = song.singer,
                color = MusicTextSecondary,
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
                tint = if (song.isLoved) MusicPrimary else MusicIconMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}




