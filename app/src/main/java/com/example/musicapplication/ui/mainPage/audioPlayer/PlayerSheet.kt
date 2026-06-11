package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.musicapplication.R
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.ui.component.AppleMusicBackground
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class PlayerFullPanel {
    Artwork,
    Lyrics,
    Queue
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerSheet(
    context: Context,
    playerViewModel: PlayerViewModel,
    bottomBarPadding: Dp,
    collapseRequest: Int,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val song by playerViewModel.songUiState.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val colors by playerViewModel.dominantColors.collectAsStateWithLifecycle()
    // mini ----> full 进度p, change -> compose again
    val progress = remember { Animatable(0f) }
    val p = progress.value.coerceIn(0f, 1f)


    LaunchedEffect(song.cover) {
        playerViewModel.updateColorsFromSongCover()
    }

    SideEffect {
        onProgressChange(p)
    }

    // 扩展方法
    fun expand() {
        scope.launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 360f
                )
            )
        }
    }

    // 收起方法
    fun collapse() {
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = 360f
                )
            )
        }
    }

    LaunchedEffect(collapseRequest) {
        if (collapseRequest > 0) {
            collapse()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        // 把上述dp转成px
        val screenHeightPx = with(density) { screenHeight.toPx() }
        // mini播放器高度
        val miniHeightPx = with(density) { 70.dp.toPx() }
        // 底部预留高度
        val bottomPaddingPx = with(density) { bottomBarPadding.toPx() }
        // mini播放器与底部栏的间距
        val bottomGapPx = with(density) { 10.dp.toPx() }
        // mini拖到full一共要拖动多少px
        val dragRangePx = (screenHeightPx - miniHeightPx - bottomPaddingPx - bottomGapPx)
            .coerceAtLeast(1f)

        fun settle(velocity: Float) {
            when {
                // 迅速往上甩
                velocity < -1200f -> expand()
                // 迅速往下甩
                velocity > 900f -> collapse()
                // 速度不明确，看滑动范围
                progress.value > 0.68f -> expand()
                else -> collapse()
            }
        }

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                val next = (progress.value - delta / dragRangePx).coerceIn(0f, 1f)
                progress.snapTo(next)
            }
        }

        AppleMusicPlayerMorph(
            song = song,
            isPlaying = isPlaying,
            progress = p,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bottomBarPadding = bottomBarPadding,
            dominantColor = colors.firstOrNull() ?: Color.DarkGray,
            secondaryColor = colors.getOrNull(1) ?: Color.Black,
            onClick = { expand() },
            onPlayPauseClick = {
                if (isPlaying) {
                    playerViewModel.pause()
                } else {
                    playerViewModel.resume()
                }
            },
            modifier = Modifier.fillMaxSize(),
            dragModifier = Modifier
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        settle(velocity)
                    }
                ),
            playerViewModel = playerViewModel
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun AppleMusicPlayerMorph(
    song: SongUiState,
    isPlaying: Boolean,
    progress: Float,
    screenWidth: Dp,
    screenHeight: Dp,
    bottomBarPadding: Dp,
    dominantColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel
) {
    val p = progress.coerceIn(0f, 1f)

    val miniHorizontalPadding = 18.dp
    val miniHeight = 70.dp
    val miniBottom = bottomBarPadding + 10.dp

    // 上方圆角, 18.dp -> 0.dp
    val cardRadius = when {
        p < 0.92f -> 18.dp
        else -> lerpDp(18.dp, 0.dp, smoothProgress(p, 0.92f, 1f))
    }

    val cardWidth = lerpDp(
        screenWidth - miniHorizontalPadding * 2,
        screenWidth,
        p
    )

    val cardHeight = lerpDp(
        miniHeight,
        screenHeight,
        p
    )
    // card x 方向上的坐标
    val cardX = (screenWidth - cardWidth) / 2

    // y 方向
    val cardY = lerpDp(
        screenHeight - miniBottom - miniHeight,
        0.dp,
        p
    )

    val fullBackgroundAlpha = smoothProgress(p, 0.9f, 0.98f)
    val outsideScrimAlpha = smoothProgress(p, 0.01f, 0.32f) *
            (1f - smoothProgress(p, 0.86f, 1f)).coerceIn(0f, 1f)
    val cardBackgroundAlpha = smoothProgress(p, 0f, 0.08f)
    val whiteSurfaceAlpha = (1f - smoothProgress(p, 0f, 0.16f)).coerceIn(0f, 1f)
    val fullContentAlpha = smoothProgress(p, 0.35f, 0.9f)
    val miniContentAlpha = (1f - smoothProgress(p, 0.05f, 0.35f)).coerceIn(0f, 1f)
    val clickInteractionSource = remember { MutableInteractionSource() }

    // Image图片模糊解决方法
    val density = LocalDensity.current
    val context = LocalContext.current

    val maxCoverSizePx = with(density) {
        300.dp.roundToPx()
    }

    val coverRequest = remember(song.cover, maxCoverSizePx) {
        ImageRequest.Builder(context)
            .data(song.cover)
            .size(maxCoverSizePx, maxCoverSizePx)
            .build()
    }
    var fullPanel by remember { mutableStateOf(PlayerFullPanel.Artwork) }
    var showMoreActions by remember { mutableStateOf(false) }
    val panelTransition by animateFloatAsState(
        targetValue = if (fullPanel == PlayerFullPanel.Artwork) 0f else 1f,
        animationSpec = tween(durationMillis = 170),
        label = "playerPanelTransition"
    )
    val compactArtworkProgress = if (fullPanel != PlayerFullPanel.Artwork && p < 0.98f) {
        1f
    } else {
        panelTransition
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(fullBackgroundAlpha)
        ) {
            AppleMusicBackground(
                cover = song.cover,
                dominantColor = dominantColor,
                secondaryColor = secondaryColor
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.45f * outsideScrimAlpha))
        )

        val cardModifier = Modifier
            .offset(x = cardX, y = cardY)
            .width(cardWidth)
            .height(cardHeight)
            .then(dragModifier)
            .then(
                if (p < 0.97f) {
                    Modifier.clip(RoundedCornerShape(cardRadius))
                } else {
                    Modifier
                }
            )
            .background(Color.Transparent)
            .then(
                if (p < 0.12f) {
                    Modifier.border(
                        width = 1.dp,
                        color = LocalMusicThemeColors.current.border,
                        shape = RoundedCornerShape(cardRadius)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = p < 0.08f,
                interactionSource = clickInteractionSource,
                indication = null,
                onClick = onClick
            )

        Box(
            modifier = cardModifier
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(cardBackgroundAlpha)
            ) {
                AppleMusicBackground(
                    cover = song.cover,
                    dominantColor = dominantColor,
                    secondaryColor = secondaryColor
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        LocalMusicThemeColors.current.playerSurface.copy(alpha = whiteSurfaceAlpha)
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                Image(
                    painter = rememberAsyncImagePainter(
                        model = coverRequest,
                        placeholder = painterResource(R.drawable.default_cover),
                        error = painterResource(R.drawable.default_cover)
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(
                            x = lerpDp(
                                lerpDp(12.dp, 42.dp, p),
                                lerpDp(12.dp, 32.dp, p),
                                compactArtworkProgress
                            ),
                            y = lerpDp(
                                lerpDp(10.dp, 145.dp, p),
                                lerpDp(10.dp, 62.dp, p),
                                compactArtworkProgress
                            )
                        )
                        .size(
                            lerpDp(
                                lerpDp(50.dp, 300.dp, p),
                                lerpDp(50.dp, 64.dp, p),
                                compactArtworkProgress
                            )
                        )
                        .clip(RoundedCornerShape(lerpDp(14.dp, 18.dp, p)))
                )

                if (miniContentAlpha > 0.01f) {
                    MiniContent(
                        song = song,
                        isPlaying = isPlaying,
                        alpha = miniContentAlpha,
                        onPlayPauseClick = onPlayPauseClick,
                        onPlayNextClick = {
                            playerViewModel.playNext()
                        }
                    )
                }

                if (fullContentAlpha > 0.01f) {
                    FullContent(
                        song = song,
                        isPlaying = isPlaying,
                        alpha = fullContentAlpha,
                        onPlayPauseClick = onPlayPauseClick,
                        selectedPanel = fullPanel,
                        onPanelChange = { fullPanel = it },
                        onMoreClick = { showMoreActions = true },
                        playerViewModel = playerViewModel
                    )
                }
            }
        }

        SongMoreActionSheet(
            visible = showMoreActions,
            onDismiss = { showMoreActions = false }
        )
    }
}

@Composable
private fun MiniContent(
    song: SongUiState,
    isPlaying: Boolean,
    alpha: Float,
    onPlayPauseClick: () -> Unit,
    onPlayNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .alpha(alpha)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(62.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.songTitle,
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 16.sp,
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

        IconButton(onClick = onPlayPauseClick) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.ic_pause_mini else R.drawable.ic_play_mini
                ),
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(30.dp)
            )
        }

        IconButton(onClick = {
            onPlayNextClick()
        }) {
            Icon(
                painter = painterResource(R.drawable.ic_next_mini),
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun FullContent(
    song: SongUiState,
    isPlaying: Boolean,
    alpha: Float,
    onPlayPauseClick: () -> Unit,
    selectedPanel: PlayerFullPanel,
    onPanelChange: (PlayerFullPanel) -> Unit,
    onMoreClick: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val queue by playerViewModel.songQueue.collectAsState()
    val queueKeys by playerViewModel.queueKeys.collectAsState()
    val currentIndex by playerViewModel.currentIndex.collectAsState()
    val playMode by playerViewModel.playMode.collectAsState()
    val panelTextAlpha by animateFloatAsState(
        targetValue = if (selectedPanel == PlayerFullPanel.Artwork) 0f else 1f,
        animationSpec = tween(durationMillis = 520),
        label = "playerPanelTextAlpha"
    )

    val lyricsUiState by playerViewModel.lyricsUiState.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
        ) {
            if (panelTextAlpha < 0.99f) {
                ArtworkTopContent(
                    song = song,
                    onFavoriteSong = { playerViewModel.doFavoriteEvent() },
                    onMoreClick = onMoreClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 34.dp)
                        .alpha(1f - panelTextAlpha)
                )
            }

            if (panelTextAlpha > 0.01f) {
                PanelTopContent(
                    selectedPanel = selectedPanel,
                    song = song,
                    queue = queue,
                    queueKeys = queueKeys,
                    currentIndex = currentIndex,
                    playMode = playMode,
                    onShuffleClick = {
                        playerViewModel.changePlayMode(PlayMode.Shuffle)
                    },
                    onRepeatClick = {
                        playerViewModel.changePlayMode(
                            if (playMode == PlayMode.Repeat) {
                                PlayMode.RepeatOne
                            } else {
                                PlayMode.Repeat
                            }
                        )
                    },
                    onSequenceClick = {
                        playerViewModel.changePlayMode(PlayMode.Sequence)
                    },
                    onMoreClick = onMoreClick,
                    onFavoriteSong = {
                        playerViewModel.doFavoriteEvent()
                    },
                    onSongClick = { index ->
                        playerViewModel.playQueueIndex(index)
                    },
                    onMoveSong = { from, to ->
                        playerViewModel.moveQueueSong(from, to)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(panelTextAlpha),
                    lyricsUiState = lyricsUiState,
                    currentPosition = currentPosition
                )
            }
        }

        PlaybackFooter(
            isPlaying = isPlaying,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = { playerViewModel.playPrevious() },
            onNextClick = { playerViewModel.playNext() },
            selectedPanel = selectedPanel,
            onPanelChange = onPanelChange,
            playerViewModel = playerViewModel
        )
    }
}

@Composable
private fun ArtworkTopContent(
    song: SongUiState,
    onFavoriteSong: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.songTitle,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = song.singer,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            enabled = !song.isFavoriteLoading,
            onClick = {
                onFavoriteSong()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.heart_solid_full),
                contentDescription = null,
                tint = if (song.isLoved) LocalMusicThemeColors.current.primary else Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                painter = painterResource(R.drawable.ellipsis_vertical_solid_full),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun PanelTopContent(
    selectedPanel: PlayerFullPanel,
    song: SongUiState,
    queue: List<Song>,
    queueKeys: List<String>,
    currentIndex: Int,
    playMode: PlayMode,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSequenceClick: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteSong: () -> Unit,
    onSongClick: (Int) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    lyricsUiState: LyricsUiState,
    currentPosition: Int
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(58.dp))

        QueueNowPlayingHeader(
            song = song,
            onFavoriteSong = onFavoriteSong,
            onMoreClick = onMoreClick
        )

        when (selectedPanel) {
            PlayerFullPanel.Lyrics -> {
                Spacer(modifier = Modifier.height(42.dp))

                LyricsPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(355.dp),
                    lyricsUiState = lyricsUiState,
                    currentPosition = currentPosition
                )
            }

            PlayerFullPanel.Queue -> {
                Spacer(modifier = Modifier.height(18.dp))

                PlaybackModeRow(
                    playMode = playMode,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSequenceClick = onSequenceClick
                )

                Spacer(modifier = Modifier.height(18.dp))

                QueuePanel(
                    queue = queue,
                    queueKeys = queueKeys,
                    currentIndex = currentIndex,
                    onSongClick = onSongClick,
                    onMoveSong = { from, to ->
                        onMoveSong(from, to)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
            }

            PlayerFullPanel.Artwork -> Spacer(modifier = Modifier.height(355.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PlaybackFooter(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    selectedPanel: PlayerFullPanel,
    onPanelChange: (PlayerFullPanel) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val queue by playerViewModel.songQueue.collectAsState()
    val currentIndex by playerViewModel.currentIndex.collectAsState()
    val playMode by playerViewModel.playMode.collectAsState()
    val hasValidCurrent = queue.isNotEmpty() && currentIndex in queue.indices
    val canPlayPrevious = hasValidCurrent && when (playMode) {
        PlayMode.Sequence,
        PlayMode.Shuffle -> currentIndex > 0
        PlayMode.Repeat,
        PlayMode.RepeatOne -> true
    }
    val canPlayNext = hasValidCurrent && when (playMode) {
        PlayMode.Sequence,
        PlayMode.Shuffle -> currentIndex < queue.lastIndex
        PlayMode.Repeat,
        PlayMode.RepeatOne -> true
    }

    PlayerProgressBar(playerViewModel)

    Spacer(modifier = Modifier.height(46.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousClick,
            enabled = canPlayPrevious,
            modifier = Modifier.size(86.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.backward_solid_full),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (canPlayPrevious) 1f else 0.28f),
                modifier = Modifier.size(52.dp)
            )
        }

        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(86.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause_icon else R.drawable.play_icon
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(68.dp)
            )
        }

        IconButton(
            onClick = onNextClick,
            enabled = canPlayNext,
            modifier = Modifier.size(86.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.forward_solid_full),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (canPlayNext) 1f else 0.28f),
                modifier = Modifier.size(52.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    PlayerPanelSwitcher(
        selectedPanel = selectedPanel,
        onPanelChange = onPanelChange
    )
}

@Composable
private fun QueueNowPlayingHeader(
    song: SongUiState,
    onFavoriteSong: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(80.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.songTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = song.singer,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            enabled = !song.isFavoriteLoading && song.canFavorite,
            onClick = onFavoriteSong
        ) {
            Icon(
                painter = painterResource(
                    if (song.isLoved) R.drawable.heart_solid_full
                    else R.drawable.heart_regular_full
                ),
                contentDescription = null,
                tint = if (song.isLoved) LocalMusicThemeColors.current.primary else Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                painter = painterResource(R.drawable.ellipsis_vertical_solid_full),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SongMoreActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = visible) {
        onDismiss()
    }

    val sheetProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "songMoreActionSheet"
    )

    if (!visible && sheetProgress <= 0.01f) return

    val overlayInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f * sheetProgress))
            .clickable(
                interactionSource = overlayInteractionSource,
                indication = null,
                onClick = onDismiss
            )
            .alpha(sheetProgress)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 22.dp, vertical = 28.dp)
                .offset(y = 40.dp * (1f - sheetProgress))
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = LocalMusicThemeColors.current.border,
                    shape = RoundedCornerShape(28.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = "更多操作",
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            SongMoreActionRow(
                icon = R.drawable.ic_download_sheet,
                title = "下载",
                subtitle = "保存到本地离线播放",
                iconBackground = LocalMusicThemeColors.current.primarySoft,
                iconTint = LocalMusicThemeColors.current.primary,
                titleColor = LocalMusicThemeColors.current.textPrimary,
                subtitleColor = LocalMusicThemeColors.current.textSecondary
            )

            SongMoreActionRow(
                icon = R.drawable.ic_sleep_timer_sheet,
                title = "定时睡眠",
                subtitle = "设置自动停止播放",
                iconBackground = LocalMusicThemeColors.current.primarySoft,
                iconTint = LocalMusicThemeColors.current.primary,
                titleColor = LocalMusicThemeColors.current.textPrimary,
                subtitleColor = LocalMusicThemeColors.current.textSecondary
            )

            SongMoreActionRow(
                icon = R.drawable.ic_share_sheet,
                title = "分享歌曲",
                subtitle = "发送给好友或复制链接",
                iconBackground = LocalMusicThemeColors.current.primarySoft,
                iconTint = LocalMusicThemeColors.current.primary,
                titleColor = LocalMusicThemeColors.current.textPrimary,
                subtitleColor = LocalMusicThemeColors.current.textSecondary
            )
        }
    }
}

@Composable
private fun SongMoreActionRow(
    icon: Int,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconTint: Color,
    titleColor: Color,
    subtitleColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackModeRow(
    playMode: PlayMode,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSequenceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlaybackModeButton(
            icon = R.drawable.ic_shuffle_full,
            selected = playMode == PlayMode.Shuffle,
            onClick = onShuffleClick,
            modifier = Modifier.weight(1f)
        )
        PlaybackModeButton(
            icon = if (playMode == PlayMode.RepeatOne) {
                R.drawable.ic_repeat_one_full
            } else {
                R.drawable.ic_repeat_full
            },
            selected = playMode == PlayMode.Repeat || playMode == PlayMode.RepeatOne,
            onClick = onRepeatClick,
            modifier = Modifier.weight(1f)
        )
        PlaybackModeButton(
            icon = R.drawable.ic_sequence_full,
            selected = playMode == PlayMode.Sequence,
            onClick = onSequenceClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaybackModeButton(
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(
                if (selected) {
                    Color.White.copy(alpha = 0.72f)
                } else {
                    Color.White.copy(alpha = 0.13f)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) Color(0xFF255D68) else Color.White,
            modifier = Modifier.size(25.dp)
        )
    }
}

/**
 * 歌词板
 */
@Composable
private fun LyricsPanel(
    lyricsUiState: LyricsUiState,
    modifier: Modifier = Modifier,
    currentPosition: Int
) {
    val lines = lyricsUiState.lines

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        when {
            lyricsUiState.isLoading -> {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                        .size(28.dp)
                )
            }

            lines.isEmpty() -> {
                Text(
                    text = "暂无歌词",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val currentIndex = lines.indexOfLast {
                        it.timeSeconds <= currentPosition
                    }.coerceAtLeast(0)
                    val lazyListState = rememberLazyListState()
                    val density = LocalDensity.current
                    val centerOffset = with(density) { (maxHeight / 2).roundToPx() }

                    LaunchedEffect(currentIndex, lines.size) {
                        lazyListState.animateScrollToItem(
                            index = currentIndex,
                            scrollOffset = 0
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState,
                        contentPadding = PaddingValues(vertical = maxHeight / 2),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val selected = index == currentIndex

                            Text(
                                text = line.text,
                                color = if (selected) {
                                    Color.White
                                } else {
                                    Color.White.copy(alpha = 0.45f)
                                },
                                fontSize = if (selected) 28.sp else 23.sp,
                                lineHeight = if (selected) 35.sp else 30.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<Song>,
    queueKeys: List<String>,
    currentIndex: Int,
    onSongClick: (Int) -> Unit,
    onMoveSong: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveSong(from.index, to.index)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        Text(
            text = "继续播放",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "来自 喜爱歌曲",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无播放队列",
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue,
                    key = { index, song -> queueKeys.getOrNull(index) ?: "${song.songId}-${song.source}-$index" }) { index, song ->
                    val itemKey = queueKeys.getOrNull(index) ?: "${song.songId}-${song.source}-$index"
                    ReorderableItem(
                        reorderableLazyListState,
                        key = itemKey
                    ) {
                        QueueSongRow(
                            song = song,
                            isCurrent = index == currentIndex,
                            onClick = { onSongClick(index) },
                            dragHandleModifier = Modifier.draggableHandle()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) Color.White.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.songTitle,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = song.singer,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.list_ul_solid_full),
            contentDescription = null,
            tint = Color.White.copy(alpha = if (isCurrent) 0.78f else 0.36f),
            modifier = Modifier.size(24.dp)
                .size(24.dp)
                .then(dragHandleModifier)
        )
    }
}

@Composable
private fun PlayerPanelSwitcher(
    selectedPanel: PlayerFullPanel,
    onPanelChange: (PlayerFullPanel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 58.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerPanelButton(
            icon = R.drawable.words_of_song,
            selected = selectedPanel == PlayerFullPanel.Lyrics,
            onClick = {
                onPanelChange(
                    if (selectedPanel == PlayerFullPanel.Lyrics) {
                        PlayerFullPanel.Artwork
                    } else {
                        PlayerFullPanel.Lyrics
                    }
                )
            },
            modifier = Modifier.weight(1f)
        )

        PlayerPanelButton(
            icon = R.drawable.list_ul_solid_full,
            selected = selectedPanel == PlayerFullPanel.Queue,
            onClick = {
                onPanelChange(
                    if (selectedPanel == PlayerFullPanel.Queue) {
                        PlayerFullPanel.Artwork
                    } else {
                        PlayerFullPanel.Queue
                    }
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlayerPanelButton(
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "playerPanelButtonSelected"
    )

    Box(
        modifier = modifier
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(
                    width = 2.dp * selectedProgress,
                    color = Color.White.copy(alpha = selectedProgress),
                    shape = RoundedCornerShape(13.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.52f + 0.48f * selectedProgress),
                modifier = Modifier.size(24.dp + 4.dp * selectedProgress)
            )
        }
    }
}

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction.coerceIn(0f, 1f)
}

fun smoothProgress(
    value: Float,
    start: Float,
    end: Float
): Float {
    if (value <= start) return 0f
    if (value >= end) return 1f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}
