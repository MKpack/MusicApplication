package com.example.musicapplication.ui.mainPage.profile.download

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.R
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

@Composable
fun DownloadMusicScreen(
    onBack: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    downloadMusicViewModel: DownloadMusicViewModel = hiltViewModel()
) {
    val uiState by downloadMusicViewModel.uiState.collectAsState()
    val songs = uiState.songs
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateSetOf<Long>() }

    fun exitSelectMode() {
        isSelectMode = false
        selectedSongIds.clear()
    }

    fun toggleSelection(songId: Long) {
        if (selectedSongIds.contains(songId)) {
            selectedSongIds.remove(songId)
            if (selectedSongIds.isEmpty()) {
                isSelectMode = false
            }
        } else {
            selectedSongIds.add(songId)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LocalMusicThemeColors.current.bgTop,
                        LocalMusicThemeColors.current.bgBottom
                    )
                )
            )
    ) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomPadding = 120.dp
        val itemSpacing = 14.dp
        val listMinHeight = (
            maxHeight -
                statusBarTop -
                8.dp -
                50.dp -
                58.dp -
                48.dp -
                itemSpacing * 3 -
                bottomPadding
            ).coerceAtLeast(320.dp)
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = statusBarTop + 8.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            item {
                DownloadTopBar(
                    isSelectMode = isSelectMode,
                    selectedCount = selectedSongIds.size,
                    onBack = {
                        if (isSelectMode) {
                            exitSelectMode()
                        } else {
                            onBack()
                        }
                    },
                    onDelete = {
                        val ids = selectedSongIds.toList()
                        if (ids.isNotEmpty()) {
                            downloadMusicViewModel.deleteDownloadSongs(ids)
                        }
                        exitSelectMode()
                    }
                )
            }

            item {
                DownloadTag()
            }

            item {
                PlayAllButton(
                    enabled = songs.isNotEmpty() && !isSelectMode,
                    onClick = {
                        if (songs.isNotEmpty()) {
                            onSongClick(songs, 0)
                        }
                    }
                )
            }

            if (songs.isEmpty()) {
                item {
                    EmptyDownloadState(modifier = Modifier.height(listMinHeight))
                }
            } else {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> "download_${song.songId}" }
                ) { index, song ->
                    DownloadSongRow(
                        song = song,
                        isSelectMode = isSelectMode,
                        isSelected = selectedSongIds.contains(song.songId),
                        onClick = {
                            if (isSelectMode) {
                                toggleSelection(song.songId)
                            } else {
                                onSongClick(songs, index)
                            }
                        },
                        onLongClick = {
                            isSelectMode = true
                            selectedSongIds.add(song.songId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadTopBar(
    isSelectMode: Boolean,
    selectedCount: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.textPrimary,
                modifier = Modifier.size(25.dp)
            )
        }

        Text(
            text = if (isSelectMode) "已选择 $selectedCount 首" else "下载管理",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )

        if (isSelectMode) {
            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (selectedCount > 0) {
                        Color(0xFFFF5148)
                    } else {
                        LocalMusicThemeColors.current.textSecondary
                    },
                    modifier = Modifier.size(25.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadTag() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "长按歌曲可进入多选删除",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlayAllButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (enabled) {
                    LocalMusicThemeColors.current.primary
                } else {
                    LocalMusicThemeColors.current.primarySoft
                }
            )
            .combinedClickable(
                enabled = enabled,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (enabled) Color.White else LocalMusicThemeColors.current.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "播放全部",
            color = if (enabled) Color.White else LocalMusicThemeColors.current.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadSongRow(
    song: Song,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    LocalMusicThemeColors.current.primarySoft
                } else {
                    LocalMusicThemeColors.current.surface
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    LocalMusicThemeColors.current.primary
                } else {
                    LocalMusicThemeColors.current.border
                },
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectMode) {
            SelectionCircle(isSelected = isSelected)
            Spacer(modifier = Modifier.width(10.dp))
        }

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
    }
}

@Composable
private fun SelectionCircle(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    LocalMusicThemeColors.current.primary
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = 1.5.dp,
                color = if (isSelected) {
                    LocalMusicThemeColors.current.primary
                } else {
                    LocalMusicThemeColors.current.textSecondary
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyDownloadState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "暂无下载音乐",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "下载后的歌曲会保存在这里，断网时也可以播放。",
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
