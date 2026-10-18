package com.example.musicapplication.ui.mainPage.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 迷你播放器的位置和大小（相对整个屏幕，y 是上边缘），由底栏决定，播放器从这里展开 */
@Immutable
data class MiniPlayerAnchor(val x: Dp, val y: Dp, val width: Dp, val height: Dp)

/** 列表底部要留出的空间，让最后一项能滚到底栏和迷你播放器上方。由 MainPage 提供 */
val LocalMainBottomInset = compositionLocalOf { 0.dp }

/** 底栏的尺寸，取自设计稿（UI_REDESIGN_PLAN.md 3.4 节） */
internal object BottomBarMetrics {
    val SideMargin = 16.dp              // 左右边距
    val BottomGap = 8.dp                // 底栏和系统导航栏之间
    val TabBarHeight = 62.dp            // Tab 胶囊、搜索圆按钮
    val CollapsedSize = 54.dp           // 收起后一行里三个元素的高度
    val Gap = 10.dp                     // 胶囊和搜索圆之间、迷你播放器和底栏之间
    val MiniPlayerHeight = 52.dp
    val HiddenMiniPlayerBottom = 14.dp  // 二级页面没有底栏时，迷你播放器离导航栏的距离
}

/**
 * 底栏的状态：展开 / 收起、显示 / 隐藏，以及据此算出的迷你播放器位置和列表留白。
 * 底栏、迷你播放器、列表都从这里取尺寸，不用互相测量，所以不会晚一帧才对齐。
 */
@Stable
class MainBottomBarState internal constructor(
    private val scope: CoroutineScope,
    density: Density,
) {
    private val collapseAnimation = Animatable(0f)
    private val visibilityAnimation = Animatable(1f)

    // 目标状态。动画进行中也以它为准，避免同一个方向重复启动动画
    private var collapsed = false
    private var visible = true

    /** 0 = 展开，1 = 收起成一行 */
    val collapseFraction: Float get() = collapseAnimation.value

    /** 1 = 显示，0 = 隐藏（进入搜索、我喜欢等二级页面时） */
    val visibility: Float get() = visibilityAnimation.value

    fun collapse() = animateCollapse(true)

    fun expand() = animateCollapse(false)

    fun setVisible(visible: Boolean) {
        if (this.visible == visible) return
        this.visible = visible
        scope.launch {
            visibilityAnimation.animateTo(if (visible) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    private fun animateCollapse(collapse: Boolean) {
        if (collapsed == collapse) return
        collapsed = collapse
        scope.launch {
            collapseAnimation.animateTo(if (collapse) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    /**
     * 列表往下翻一段就收起底栏，往上翻一段就展开。
     * 挂在所有页面的外层，任何列表的滚动都会传上来，各页面的滚动代码不用改。
     */
    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        private val thresholdPx = with(density) { 32.dp.toPx() }

        // 同一个方向上累计滚动的距离，换方向就重新计
        private var travel = 0f

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val dy = consumed.y
            if (dy != 0f) {
                if ((dy < 0f) != (travel < 0f)) travel = 0f
                travel += dy
                when {
                    travel < -thresholdPx -> collapse()   // 内容往上走 = 往下翻
                    travel > thresholdPx -> expand()
                }
            }
            return Offset.Zero
        }
    }

    /** 列表底部的留白：导航栏 + 底栏 + 迷你播放器，再多留一点 */
    fun contentInset(navigationBarInset: Dp): Dp = with(BottomBarMetrics) {
        navigationBarInset + BottomGap + TabBarHeight + Gap + MiniPlayerHeight + 16.dp
    }

    /**
     * 迷你播放器现在应该在哪：
     * 展开时整行宽、在 Tab 胶囊上方；收起时夹在两个小圆中间、和它们同一行；没有底栏时贴近屏幕底部。
     * 两种状态之间按动画进度插值，所以迷你播放器会跟着底栏一起连续变化。
     */
    fun miniPlayerAnchor(screenWidth: Dp, screenHeight: Dp, navigationBarInset: Dp): MiniPlayerAnchor =
        with(BottomBarMetrics) {
            val expandedBottom = navigationBarInset + BottomGap + TabBarHeight + Gap
            val collapsedInset = SideMargin + CollapsedSize + Gap
            val collapsedBottom = navigationBarInset + BottomGap + (TabBarHeight - CollapsedSize) / 2
            val hiddenBottom = navigationBarInset + HiddenMiniPlayerBottom

            val collapse = collapseFraction.coerceIn(0f, 1f)
            val barInset = lerp(SideMargin, collapsedInset, collapse)
            val barHeight = lerp(MiniPlayerHeight, CollapsedSize, collapse)
            val barBottom = lerp(expandedBottom, collapsedBottom, collapse)

            val shown = visibility.coerceIn(0f, 1f)
            val inset = lerp(SideMargin, barInset, shown)
            val height = lerp(MiniPlayerHeight, barHeight, shown)
            val bottom = lerp(hiddenBottom, barBottom, shown)

            MiniPlayerAnchor(
                x = inset,
                y = screenHeight - bottom - height,
                width = screenWidth - inset * 2,
                height = height
            )
        }
}

@Composable
fun rememberMainBottomBarState(): MainBottomBarState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(scope, density) { MainBottomBarState(scope, density) }
}
