package com.example.musicapplication.ui.component

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlinx.coroutines.delay

// 拉过这么多就松手刷新；刷新时内容也停在这个位置
private val RefreshThreshold = 64.dp

// 指示器：四根竖条，每根 4dp 宽、间隔 4dp，最高 22dp
private val IndicatorHeight = 30.dp
private val BarWidth = 4.dp
private val BarGap = 4.dp
private val BarMinHeight = 4.dp
private val BarMaxHeight = 22.dp
private const val BarCount = 4

// 刷新结果在原处停留多久再弹回去：失败的提示多停一会儿，来得及看清
private const val UpdatedHoldMillis = 800L
private const val FailedHoldMillis = 1300L

// 松手后等这么久调用方还没开始刷新（这次下拉被忽略了），就把内容弹回去
private const val RefreshStartGraceMillis = 150L

// 一次下拉刷新至少跳这么久，见 withMinRefreshDuration
private const val MinRefreshingMillis = 600L

// 刷新时四根竖条各自的节奏（毫秒）和起始相位，看起来像随着音乐跳动
private val DanceDurations = intArrayOf(450, 310, 390, 280)
private val DanceOffsets = intArrayOf(0, 200, 100, 250)

private sealed interface RefreshResult {
    data object Updated : RefreshResult
    data class Failed(val message: String) : RefreshResult
}

/**
 * 下拉刷新，指示器是四根均衡器竖条。
 * - 整页跟着手指往下走，过了阈值越拉越费力；露出来的空隙里竖条跟着长高，不压在任何内容上；
 * - 拉过 [RefreshThreshold] 竖条变成主题色并轻震一下，松手开始刷新；
 * - 刷新时内容停在阈值的位置，竖条随节奏跳动；
 * - 刷新结束后在原处显示「已更新」或失败原因，停一下再弹回去，不弹 Toast；
 * - 调用方正忙、没有真的开始刷新时（比如首屏还在加载，忽略了这次下拉），内容自己弹回去。
 *
 * @param refreshError 这次刷新失败的原因。刷新结束（[isRefreshing] 变回 false）时不为 null 就显示失败
 * @param enabled 为 false 时不能下拉
 * @param indicatorTop 内容让出来的空隙从哪里开始，一般是状态栏下沿
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshError: String? = null,
    enabled: Boolean = true,
    indicatorTop: Dp = GlassTopBarDefaults.statusBarHeight,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val haptic = LocalHapticFeedback.current
    val thresholdPx = with(LocalDensity.current) { RefreshThreshold.toPx() }

    // 刷新结束后，在原处停一下显示结果
    var result by remember { mutableStateOf<RefreshResult?>(null) }
    var wasRefreshing by remember { mutableStateOf(false) }
    val latestError by rememberUpdatedState(refreshError)
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            wasRefreshing = true
            result = null
            return@LaunchedEffect
        }
        if (!wasRefreshing) return@LaunchedEffect
        wasRefreshing = false
        val error = latestError
        result = if (error != null) RefreshResult.Failed(error) else RefreshResult.Updated
        delay(if (error != null) FailedHoldMillis else UpdatedHoldMillis)
        result = null
    }
    // Material 松手后先把内容停到阈值再调 onRefresh，之后只在 isRefreshing 变化时才移动内容。
    // 调用方忽略了这次下拉时 isRefreshing 不会变，内容会一直停在那里：所以记一下刚请求过刷新，等一会儿还没开始就弹回去
    var awaitingStart by remember { mutableStateOf(false) }
    LaunchedEffect(awaitingStart) {
        if (awaitingStart) {
            delay(RefreshStartGraceMillis)
            awaitingStart = false
        }
    }
    // 刷新中、显示结果时都让内容停在阈值的位置
    val holding = isRefreshing || result != null || awaitingStart

    // 手指拉过阈值的那一下轻震；刷新开始后内容自己停到阈值位置时不震
    val pastThreshold by remember { derivedStateOf { state.distanceFraction >= 1f } }
    LaunchedEffect(pastThreshold) {
        if (pastThreshold && !holding) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }

    Box(
        modifier = modifier.pullToRefresh(
            isRefreshing = holding,
            state = state,
            enabled = enabled,
            threshold = RefreshThreshold,
            onRefresh = {
                awaitingStart = true
                onRefresh()
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 拉动的距离只在这里读，下拉时只移动图层，页面不重组
                .graphicsLayer { translationY = pullOffset(state.distanceFraction) * thresholdPx }
        ) {
            content()
        }

        RefreshIndicator(
            distanceFraction = { state.distanceFraction },
            offsetPx = { pullOffset(state.distanceFraction) * thresholdPx },
            armed = pastThreshold,
            isRefreshing = isRefreshing,
            result = result,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = indicatorTop)
        )
    }
}

/**
 * ViewModel 处理下拉刷新时用它包住请求：请求再快，也至少过 [MinRefreshingMillis] 才返回。
 * 没网时请求几毫秒就失败，isRefreshing 在同一帧里从 true 变回 false，[MusicPullToRefreshBox] 根本看不到刷新开始过：
 * 竖条不会跳，失败原因也不会显示，内容直接弹回去。
 */
suspend fun <T> withMinRefreshDuration(block: suspend () -> T): T {
    val start = SystemClock.uptimeMillis()
    val result = block()
    val remaining = MinRefreshingMillis - (SystemClock.uptimeMillis() - start)
    if (remaining > 0) delay(remaining)
    return result
}

/**
 * 拉动的进度（1 = 阈值）换算成内容往下移动的距离（单位也是阈值）：
 * 阈值以内跟手（Material 已经把手指的距离打了五折），过了阈值越拉越费力，最多到阈值的两倍。
 */
private fun pullOffset(distanceFraction: Float): Float {
    if (distanceFraction <= 1f) return distanceFraction
    val overshoot = (distanceFraction - 1f).coerceAtMost(2f)
    return 1f + overshoot - overshoot * overshoot / 4f
}

@Composable
private fun RefreshIndicator(
    distanceFraction: () -> Float,
    offsetPx: () -> Float,
    armed: Boolean,
    isRefreshing: Boolean,
    result: RefreshResult?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicThemeColors.current
    val heightPx = with(LocalDensity.current) { IndicatorHeight.toPx() }
    val showing = isRefreshing || result != null

    Box(
        modifier = modifier
            .height(IndicatorHeight)
            .graphicsLayer {
                // 放在内容让出来的空隙正中
                translationY = offsetPx() / 2f - heightPx / 2f
                alpha = if (showing) 1f else (distanceFraction() * 1.6f).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            result != null -> RefreshResultText(result)
            // 只有刷新时才跑无限动画，平时不占用每一帧
            isRefreshing -> DancingBars(color = colors.primary)
            else -> PullingBars(distanceFraction = distanceFraction, armed = armed)
        }
    }
}

/** 拉动时的竖条：跟着拉动的距离依次长高；拉过阈值变成主题色，并弹一下 */
@Composable
private fun PullingBars(distanceFraction: () -> Float, armed: Boolean) {
    val colors = LocalMusicThemeColors.current
    val color by animateColorAsState(
        targetValue = if (armed) colors.primary else colors.textHint,
        label = "refreshBarColor"
    )
    val bump = remember { Animatable(1f) }
    LaunchedEffect(armed) {
        if (armed) {
            bump.animateTo(1.22f, tween(durationMillis = 90))
            bump.animateTo(1f, tween(durationMillis = 130))
        }
    }

    Canvas(
        modifier = Modifier
            .size(width = BarWidth * BarCount + BarGap * (BarCount - 1), height = BarMaxHeight)
            .graphicsLayer {
                scaleX = bump.value
                scaleY = bump.value
            }
    ) {
        val progress = distanceFraction()
        drawBars(color) { index ->
            ((progress - index * 0.12f) / 0.64f).coerceIn(0f, 1f)
        }
    }
}

/** 刷新中的竖条：四根各按自己的节奏上下跳 */
@Composable
private fun DancingBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "refreshBars")
    val levels = List(BarCount) { index ->
        transition.animateFloat(
            initialValue = 0.05f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = DanceDurations[index], easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(DanceOffsets[index])
            ),
            label = "refreshBar$index"
        )
    }

    Canvas(modifier = Modifier.size(width = BarWidth * BarCount + BarGap * (BarCount - 1), height = BarMaxHeight)) {
        drawBars(color) { index -> levels[index].value }
    }
}

/** 从底边往上画四根圆角竖条，[level] 是每根的高度（0 = 最矮，1 = 最高） */
private inline fun DrawScope.drawBars(color: Color, level: (Int) -> Float) {
    val barWidth = BarWidth.toPx()
    val gap = BarGap.toPx()
    val minHeight = BarMinHeight.toPx()
    val maxHeight = size.height
    repeat(BarCount) { index ->
        val height = minHeight + (maxHeight - minHeight) * level(index)
        drawRoundRect(
            color = color,
            topLeft = Offset(index * (barWidth + gap), maxHeight - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2f)
        )
    }
}

@Composable
private fun RefreshResultText(result: RefreshResult) {
    val colors = LocalMusicThemeColors.current
    val (text, color) = when (result) {
        RefreshResult.Updated -> "已更新" to colors.textSecondary
        is RefreshResult.Failed -> "刷新失败：${result.message}" to colors.neutral.danger
    }
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
}
