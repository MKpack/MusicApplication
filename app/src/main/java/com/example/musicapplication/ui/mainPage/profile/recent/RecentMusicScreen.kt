package com.example.musicapplication.ui.mainPage.profile.recent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.R
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.component.MusicCollectionScaffold
import com.example.musicapplication.ui.theme.MusicBorder
import com.example.musicapplication.ui.theme.MusicSurface
import com.example.musicapplication.ui.theme.MusicTextPrimary
import com.example.musicapplication.ui.theme.MusicTextSecondary

@Composable
fun RecentMusicScreen(
    onBack: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    recentMusicViewModel: RecentMusicViewModel = hiltViewModel()
) {
    val songUiState by recentMusicViewModel.songUiState.collectAsState()
    val songs = songUiState.songs

    MusicCollectionScaffold(
        title = "最近播放",
        tag = "最近播放记录会显示在这里",
        emptyTitle = "暂无播放记录",
        emptyDescription = "播放过的歌曲会自动记录到这里。",
        icon = Icons.Default.History,
        onBack = onBack,
        modifier = modifier,
        playAllEnabled = songs.isNotEmpty(),
        onPlayAllClick = {
            if (songs.isNotEmpty()) {
                onSongClick(songs, 0)
            }
        },
        content = if (songs.isEmpty()) {
            null
        } else {
            { _ ->
                itemsIndexed(
                    items = songs,
                    key = { index, song -> "${index}-${song.songId}-${song.source}" }
                ) { index, song ->
                    RecentSongRow(
                        song = song,
                        onClick = {
                            onSongClick(songs, index)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun RecentSongRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MusicSurface)
            .border(1.dp, MusicBorder, RoundedCornerShape(16.dp))
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
                color = MusicTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.singer,
                color = MusicTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
