package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors


@Composable
fun MusicCollectionScaffold(
    title: String,
    tag: String,
    emptyTitle: String,
    emptyDescription: String,
    icon: ImageVector,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    playAllEnabled: Boolean = false,
    onPlayAllClick: () -> Unit = {},
    content: (LazyListScope.(listAreaModifier: Modifier) -> Unit)? = null
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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

        LazyColumn(
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
                MusicCollectionTopBar(
                    title = title,
                    onBack = onBack
                )
            }

            item {
                MusicCollectionTag(
                    text = tag,
                    icon = icon
                )
            }

            item {
                PlayAllButton(
                    enabled = playAllEnabled,
                    onClick = onPlayAllClick
                )
            }

            if (content == null) {
                item {
                    EmptyListState(
                        icon = icon,
                        title = emptyTitle,
                        description = emptyDescription,
                        modifier = Modifier.height(listMinHeight)
                    )
                }
            } else {
                content(Modifier.height(listMinHeight))
            }
        }
    }
}

@Composable
private fun MusicCollectionTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.textPrimary,
                modifier = Modifier.size(25.dp)
            )
        }

        Text(
            text = title,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun MusicCollectionTag(
    text: String,
    icon: ImageVector
) {
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
                imageVector = icon,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

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
private fun PlayAllButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) LocalMusicThemeColors.current.primary else LocalMusicThemeColors.current.cardSoft)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(18.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (enabled) Color.White else LocalMusicThemeColors.current.iconMuted,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = "播放全部",
            color = if (enabled) Color.White else LocalMusicThemeColors.current.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyListState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = description,
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}
