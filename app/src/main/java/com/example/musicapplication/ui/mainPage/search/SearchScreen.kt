package com.example.musicapplication.ui.mainPage.search

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by searchViewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.errorMsg) {
        val message = uiState.errorMsg ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        searchViewModel.consumeErrorMessage()
    }

    LazyColumn(
        modifier = modifier
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
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 132.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SearchTopBar(
                keyword = uiState.keyword,
                onKeywordChange = searchViewModel::onKeywordChange,
                onBack = onBack,
                onClear = { searchViewModel.onKeywordChange("") },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        if (uiState.keyword.isBlank()) {
            if (uiState.recentSongs.isNotEmpty()) {
                item {
                    SearchSectionTitle(title = "最近搜索")
                }

                itemsIndexed(
                    items = uiState.recentSongs,
                    key = { _, song -> "recent_${song.songId}" }
                ) { index, song ->
                    SearchSongRow(
                        song = song,
                        onClick = {
                            searchViewModel.recordSearchSong(song)
                            onSongClick(uiState.recentSongs, index)
                        },
                        onFavoriteClick = {
                            searchViewModel.doFavoriteEvent(song)
                        }
                    )
                }
            }

//            item {
//                SearchSection(title = "热门搜索") {
//                    FlowRow(
//                        horizontalArrangement = Arrangement.spacedBy(10.dp),
//                        verticalArrangement = Arrangement.spacedBy(10.dp)
//                    ) {
//                        hotKeywords.forEach { text ->
//                            SearchChip(
//                                text = text,
//                                onClick = {
//                                    searchViewModel.onKeywordChange(text)
//                                    searchViewModel.searchImmediately(text)
//                                }
//                            )
//                        }
//                    }
//                }
//            }
        } else if (uiState.isLoading && uiState.songs.isEmpty()) {
            item {
                SearchLoading()
            }
        } else if (uiState.hasSearched && uiState.songs.isEmpty()) {
            item {
                SearchEmpty(keyword = uiState.keyword)
            }
        } else {
            itemsIndexed(
                items = uiState.songs,
                key = { _, song -> song.songId }
            ) { index, song ->
                SearchSongRow(
                    song = song,
                    onClick = {
                        searchViewModel.recordSearchSong(song)
                        onSongClick(uiState.songs, index)
                    },
                    onFavoriteClick = {
                        searchViewModel.doFavoriteEvent(song)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.textPrimary,
                modifier = Modifier.size(25.dp)
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        BasicTextField(
            value = keyword,
            onValueChange = onKeywordChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 15.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(LocalMusicThemeColors.current.surface)
                        .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LocalMusicThemeColors.current.iconMuted,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Box(Modifier
                        .weight(1f)
                        .height(27.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (keyword.isBlank()) {
                            Text(
                                text = "搜索歌曲、歌手、歌单",
                                color = LocalMusicThemeColors.current.textHint,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun SearchSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        SearchSectionTitle(title = title)

        content()
    }
}

@Composable
private fun SearchSectionTitle(title: String) {
    Text(
        text = title,
        color = LocalMusicThemeColors.current.textPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

@Composable
private fun SearchHistoryRow(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = LocalMusicThemeColors.current.iconMuted,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = text,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchChip(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchResultPlaceholder(
    keyword: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Text(
            text = "搜索结果",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "“$keyword” 的结果会显示在这里",
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SearchLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = LocalMusicThemeColors.current.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun SearchEmpty(
    keyword: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Text(
            text = "没有找到结果",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "换个关键词搜索“$keyword”试试。",
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SearchSongRow(
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
            contentScale = ContentScale.Crop,
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

            Spacer(modifier = Modifier.height(3.dp))

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
