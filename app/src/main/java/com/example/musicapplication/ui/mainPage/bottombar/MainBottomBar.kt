package com.example.musicapplication.ui.mainPage.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.component.Backdrop
import com.example.musicapplication.ui.component.GlassIconButton
import com.example.musicapplication.ui.component.glassShadow
import com.example.musicapplication.ui.component.liquidGlass
import com.example.musicapplication.ui.mainPage.audioPlayer.smoothProgress
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.lerp as lerpDp

// 高光滑到新选中项：带一点回弹
private val HighlightSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 380f)
// 刚开始拖动时高光跳到手指下面：快速跟上
private val FollowSpring = spring<Float>(stiffness = Spring.StiffnessHigh)

/**
 * 底部的悬浮液态玻璃栏：左边是 Tab 胶囊，右边是单独的搜索圆按钮。
 * 收起时 Tab 胶囊缩成只显示当前页图标的小圆，搜索按钮也变小，中间空出来的位置给迷你播放器
 * （迷你播放器由 PlayerSheet 画，位置见 [MainBottomBarState.miniPlayerAnchor]）。
 *
 * @param playerProgress 播放器展开的进度，播放器展开时底栏跟着下移、淡出
 */
@Composable
fun MainBottomBar(
    state: MainBottomBarState,
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onSearchClick: () -> Unit,
    backdrop: Backdrop,
    playerProgress: () -> Float,
    modifier: Modifier = Modifier,
    tabs: List<MainTab> = MainTab.entries,
) {
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 收起进度在动画里每帧都变，只在布局、绘制阶段读：动画时只重新排版和绘制，底栏不用每帧重组
    val collapse = remember(state) { { state.collapseFraction.coerceIn(0f, 1f) } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navigationBarInset + BottomBarMetrics.BottomGap)
            .height(BottomBarMetrics.TabBarHeight)
            // 进入二级页面时整体滑出屏幕；播放器展开时跟着下移、淡出
            .offset {
                val hideDistance = BottomBarMetrics.TabBarHeight + BottomBarMetrics.BottomGap + navigationBarInset + 24.dp
                val hidden = (1f - state.visibility) * hideDistance.toPx()
                val player = 28.dp.toPx() * smoothProgress(playerProgress(), 0f, 0.45f)
                IntOffset(0, (hidden + player).roundToInt())
            }
            .graphicsLayer { alpha = (state.visibility * (1f - playerProgress() * 2.2f)).coerceIn(0f, 1f) }
            .padding(horizontal = BottomBarMetrics.SideMargin)
    ) {
        val expandedTabWidth = maxWidth - BottomBarMetrics.TabBarHeight - BottomBarMetrics.Gap
        val collapsedSize = DpSize(BottomBarMetrics.CollapsedSize, BottomBarMetrics.CollapsedSize)

        TabCapsule(
            tabs = tabs,
            selectedTab = selectedTab,
            collapse = collapse,
            expandedWidth = expandedTabWidth,
            onTabSelected = onTabSelected,
            onExpand = state::expand,
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .collapsingSize(collapse, DpSize(expandedTabWidth, BottomBarMetrics.TabBarHeight), collapsedSize)
        )
        GlassIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = "搜索",
            onClick = onSearchClick,
            backdrop = backdrop,
            iconSize = 26.dp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .collapsingSize(collapse, DpSize(BottomBarMetrics.TabBarHeight, BottomBarMetrics.TabBarHeight), collapsedSize)
        )
    }
}

/** 按收起进度在展开、收起两个尺寸之间插值。进度在测量阶段读，动画时只重新测量，不触发重组 */
private fun Modifier.collapsingSize(collapse: () -> Float, expanded: DpSize, collapsed: DpSize): Modifier =
    layout { measurable, constraints ->
        val fraction = collapse()
        val width = constraints.constrainWidth(lerpDp(expanded.width, collapsed.width, fraction).roundToPx())
        val height = constraints.constrainHeight(lerpDp(expanded.height, collapsed.height, fraction).roundToPx())
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(width, height) { placeable.place(0, 0) }
    }

/**
 * Tab 胶囊。展开时是一排 Tab 加一块可以拖动的高光；收起时缩成小圆，只显示当前页的图标，点一下恢复展开。
 * 两种内容在收起过程中交叉淡入淡出。
 */
@Composable
private fun TabCapsule(
    tabs: List<MainTab>,
    selectedTab: MainTab,
    collapse: () -> Float,
    expandedWidth: Dp,
    onTabSelected: (MainTab) -> Unit,
    onExpand: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicThemeColors.current
    val neutral = colors.neutral
    // 收起到一半时 Tab 刚好淡完、小圆开始淡入，只在这时重组一次；透明度本身在绘制阶段读
    val showItems by remember(collapse) { derivedStateOf { collapse() < 0.5f } }
    val showOrb by remember(collapse) { derivedStateOf { collapse() > 0.5f } }

    Box(
        modifier = modifier
            .glassShadow(CircleShape, neutral)
            .clip(CircleShape)
            .liquidGlass(CircleShape, neutral.glass, neutral, backdrop),
        contentAlignment = Alignment.CenterStart
    ) {
        if (showItems) {
            TabItems(
                tabs = tabs,
                selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                onTabSelected = onTabSelected,
                modifier = Modifier
                    // 收起过程中仍按展开时的大小排版，超出的部分被胶囊裁掉：图标不会被挤扁，
                    // 尺寸不变，里面的 BoxWithConstraints 也不用每帧重新组合
                    .wrapContentWidth(Alignment.Start, unbounded = true)
                    .requiredWidth(expandedWidth)
                    .requiredHeight(BottomBarMetrics.TabBarHeight)
                    .graphicsLayer { alpha = 1f - smoothProgress(collapse(), 0f, 0.5f) }
            )
        }
        if (showOrb) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = smoothProgress(collapse(), 0.5f, 1f) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "展开底栏",
                        onClick = onExpand
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = selectedTab.icon,
                    contentDescription = stringResource(selectedTab.label),
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/** 一排 Tab 和下面的灰色高光。点击切换；按住左右拖动时高光跟着手指走，松手吸附到最近的一项 */
@Composable
private fun TabItems(
    tabs: List<MainTab>,
    selectedIndex: Int,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicThemeColors.current
    val scope = rememberCoroutineScope()
    // 高光的位置，单位是“第几项”，拖动时可以是小数
    val highlight = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    LaunchedEffect(selectedIndex) {
        if (!dragging) highlight.animateTo(selectedIndex.toFloat(), HighlightSpring)
    }

    BoxWithConstraints(modifier = modifier.padding(4.dp)) {
        val itemWidth = maxWidth / tabs.size
        val itemWidthPx = constraints.maxWidth.toFloat() / tabs.size
        val lastIndex = tabs.lastIndex.toFloat()

        Box(
            modifier = Modifier
                .offset { IntOffset((highlight.value * itemWidthPx).roundToInt(), 0) }
                .size(width = itemWidth, height = maxHeight)
                .background(colors.neutral.fill, CircleShape)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tabs.size, itemWidthPx) {
                    // 手指的位置换算成“第几项”，让高光的中心对准手指
                    fun indexAt(x: Float) = (x / itemWidthPx - 0.5f).coerceIn(0f, lastIndex)
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            dragging = true
                            scope.launch { highlight.animateTo(indexAt(start.x), FollowSpring) }
                        },
                        onDragEnd = {
                            val target = highlight.value.roundToInt()
                            dragging = false
                            scope.launch { highlight.animateTo(target.toFloat(), HighlightSpring) }
                            currentOnTabSelected(tabs[target])
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { highlight.animateTo(currentSelectedIndex.toFloat(), HighlightSpring) }
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scope.launch { highlight.snapTo(indexAt(change.position.x)) }
                        }
                    )
                }
        ) {
            tabs.forEachIndexed { index, tab ->
                // 离高光越近颜色越接近主题色，拖动时颜色跟着高光走
                val emphasis = (1f - abs(highlight.value - index)).coerceIn(0f, 1f)
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    color = lerp(colors.neutral.label, colors.primary, emphasis),
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: MainTab,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Tab,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = stringResource(tab.label),
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
