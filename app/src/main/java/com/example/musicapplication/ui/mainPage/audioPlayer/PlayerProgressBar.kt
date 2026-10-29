package com.example.musicapplication.ui.mainPage.audioPlayer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

private val TrackColor = Color(0xFFBBBABA)
private val FillColor = Color(0xFFDADADA)
private val LabelColor = Color(0xFFBBBABA)

/**
 * 播放进度条：点任意位置跳过去，也可以从任意位置按住左右拖，松手后跳转。
 * 触摸区域比进度条本身高，拖动时进度条变粗变亮。
 */
@Composable
fun PlayerProgressBar(
    positionMs: () -> Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    // 正在拖动时手指的位置（0~1），没在拖时是 null
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // 松手后要跳到的位置：播放器真正跳过去之前先显示它，进度条不会闪回原来的位置
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }

    val playerPositionMs = positionMs()
    // 播放器跳到位了就显示真实进度
    LaunchedEffect(playerPositionMs) {
        val target = pendingSeekMs ?: return@LaunchedEffect
        if (abs(playerPositionMs - target) < SEEK_SETTLE_TOLERANCE_MS) pendingSeekMs = null
    }
    // 跳转一直没完成（比如还在缓冲）也不能一直挡着真实进度
    LaunchedEffect(pendingSeekMs) {
        if (pendingSeekMs != null) {
            delay(SEEK_SETTLE_TIMEOUT_MS)
            pendingSeekMs = null
        }
    }

    val canSeek = durationMs > 0
    val shownMs = when {
        dragFraction != null -> (dragFraction!! * durationMs).toLong()
        pendingSeekMs != null -> pendingSeekMs!!
        else -> playerPositionMs
    }.coerceIn(0L, durationMs.coerceAtLeast(0L))
    val fraction = if (canSeek) shownMs.toFloat() / durationMs else 0f
    val isDragging = dragFraction != null
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 8.dp,
        label = "progressTrackHeight"
    )

    fun seekToFraction(value: Float) {
        val target = (value.coerceIn(0f, 1f) * durationMs).toLong()
        pendingSeekMs = target
        currentOnSeek(target)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(canSeek, durationMs) {
                    if (!canSeek) return@pointerInput
                    detectTapGestures { offset ->
                        seekToFraction(offset.x / size.width)
                    }
                }
                .pointerInput(canSeek, durationMs) {
                    if (!canSeek) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragFraction?.let { seekToFraction(it) }
                            dragFraction = null
                        },
                        onDragCancel = {
                            dragFraction = null
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(50))
                    .background(TrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(if (isDragging) Color.White else FillColor)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(shownMs),
                fontSize = 12.sp,
                color = if (isDragging) Color.White else LabelColor
            )
            Text(
                text = "-" + formatPlaybackTime((durationMs - shownMs).coerceAtLeast(0L)),
                fontSize = 11.sp,
                color = if (isDragging) Color.White else LabelColor
            )
        }
    }
}

/** 毫秒转成「分:秒」，比如 83500 -> 1:23 */
internal fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private const val SEEK_SETTLE_TOLERANCE_MS = 1_000L
private const val SEEK_SETTLE_TIMEOUT_MS = 1_500L
