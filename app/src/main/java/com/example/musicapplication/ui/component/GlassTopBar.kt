package com.example.musicapplication.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlinx.coroutines.launch

/** 玻璃顶栏的尺寸 */
object GlassTopBarDefaults {
    /** 状态栏下面那一行的高度：返回按钮、操作按钮、小标题、搜索框都在这一行 */
    val RowHeight = 52.dp

    /** 左右边距，和页面列表的左右边距一样 */
    val HorizontalPadding = 18.dp

    /** 内容里的标题要给左上角的返回按钮让出的宽度：按钮 44dp + 间距 10dp */
    val BackButtonInset = 54.dp

    /** 磨砂材质下沿渐隐的高度 */
    internal val FadeHeight = 16.dp

    /** 内容滑进顶栏这么多以后，磨砂材质完全显示 */
    internal val RevealDistance = 24.dp

    /** 状态栏的高度（系统给的） */
    val statusBarHeight: Dp
        @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
}

/** 顶栏里按钮的作用域：把 [backdrop] 传给 [GlassIconButton]，按钮就能透出后面模糊的内容 */
interface GlassTopBarScope : RowScope {
    val backdrop: Backdrop
}

private class GlassTopBarScopeImpl(
    rowScope: RowScope,
    override val backdrop: Backdrop,
) : GlassTopBarScope, RowScope by rowScope

/**
 * 带玻璃顶栏的页面，仿 iOS 的滚动边缘效果。页面内容铺满整个屏幕，从透明的状态栏下面滑过：
 * - 停在顶部时顶栏什么都不画，状态栏完全透明；
 * - 内容一滑进顶栏，淡入一层磨砂材质（模糊 + 页面底色，下沿渐隐），状态栏图标始终看得清；
 * - 有标题的页面，内容里的大标题滑走后，顶栏正中淡入小标题，点它回到顶部；
 * - 返回按钮、操作按钮、搜索框固定在顶栏里，不跟着内容滚走。
 *
 * 页面背景（纯白 / 纯黑，或者自定义的背景图，见 [PageBackground]）由这里画，[content] 不用再画。
 * 列表顶部要自己留出空间：[GlassTopBarDefaults.statusBarHeight] 加上标题等内容的位置。
 *
 * @param listState 页面列表的滚动状态，用来判断内容有没有滑进顶栏、大标题有没有滑走
 * @param title 大标题滑走后显示在顶栏正中的小标题；为 null 时不显示
 * @param onBack 不为 null 时，左上角固定一个玻璃返回按钮
 * @param actions 右上角固定的按钮
 * @param pinnedContent 固定在顶栏中间的内容（比如搜索框），有它时不显示小标题
 * @param titleCollapseOffset 列表第一项（大标题所在的那一项）往上滚过多少后显示小标题
 */
@Composable
fun GlassTopBarScaffold(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable GlassTopBarScope.() -> Unit = {},
    pinnedContent: (@Composable GlassTopBarScope.() -> Unit)? = null,
    titleCollapseOffset: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    // 顶栏的磨砂模糊的是这一层：页面背景 + 内容。顶栏要和它是兄弟、画在它上面，不能放进内容里
    val backdrop = rememberBackdrop()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .backdropSource(backdrop)
        ) {
            PageBackground(modifier = Modifier.matchParentSize())
            content()
        }
        GlassTopBar(
            listState = listState,
            backdrop = backdrop,
            title = if (pinnedContent == null) title else null,
            onBack = onBack,
            actions = actions,
            pinnedContent = pinnedContent,
            titleCollapseOffset = titleCollapseOffset
        )
    }
}

/**
 * 二级页面列表第一项的标题：和左上角固定的返回按钮在同一行，往上滚走以后顶栏正中会出现同样的小标题。
 * 按列表左右边距是 [GlassTopBarDefaults.HorizontalPadding] 来让出返回按钮的位置。
 */
@Composable
fun PageTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassTopBarDefaults.RowHeight)
            .padding(start = GlassTopBarDefaults.BackButtonInset),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GlassTopBar(
    listState: LazyListState,
    backdrop: Backdrop,
    title: String?,
    onBack: (() -> Unit)?,
    actions: @Composable GlassTopBarScope.() -> Unit,
    pinnedContent: (@Composable GlassTopBarScope.() -> Unit)?,
    titleCollapseOffset: Dp,
) {
    val colors = LocalMusicThemeColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusBarHeight = GlassTopBarDefaults.statusBarHeight
    val revealPx = with(density) { GlassTopBarDefaults.RevealDistance.toPx() }
    val collapseOffsetPx = with(density) { titleCollapseOffset.roundToPx() }

    // 大标题滑走了没有。滚动时每帧都在变的只有偏移量，这里只在跨过的那一帧重组
    val titleCollapsed by remember(listState, collapseOffsetPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseOffsetPx
        }
    }
    val showTitle = title != null && titleCollapsed
    val titleAlpha by animateFloatAsState(
        targetValue = if (showTitle) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "topBarTitle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusBarHeight + GlassTopBarDefaults.RowHeight + GlassTopBarDefaults.FadeHeight)
    ) {
        TopBarMaterial(
            backdrop = backdrop,
            color = colors.background,
            solidHeight = statusBarHeight + GlassTopBarDefaults.RowHeight,
            // 内容滑进来多少，材质就显示多少。只在绘制阶段读滚动位置，滚动时不重组
            alpha = {
                if (listState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    (listState.firstVisibleItemScrollOffset / revealPx).coerceIn(0f, 1f)
                }
            },
            modifier = Modifier.matchParentSize()
        )

        if (title != null) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight, start = 72.dp, end = 72.dp)
                    .height(GlassTopBarDefaults.RowHeight)
                    .wrapContentHeight()
                    .graphicsLayer {
                        alpha = titleAlpha
                        translationY = (1f - titleAlpha) * 4.dp.toPx()
                    }
                    // 隐藏时不接收点击，下面的内容照常能点
                    .then(
                        if (showTitle) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "回到顶部"
                            ) {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        Row(
            modifier = Modifier
                .padding(top = statusBarHeight)
                .fillMaxWidth()
                .height(GlassTopBarDefaults.RowHeight)
                .padding(horizontal = GlassTopBarDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val barScope = remember(this, backdrop) { GlassTopBarScopeImpl(this, backdrop) }
            if (onBack != null) {
                GlassIconButton(
                    icon = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "返回",
                    onClick = onBack,
                    backdrop = backdrop
                )
            }
            if (pinnedContent != null) {
                barScope.pinnedContent()
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            barScope.actions()
        }
    }
}

/**
 * 顶栏的磨砂材质：模糊后的页面 + 82% 的页面底色，下沿 [GlassTopBarDefaults.FadeHeight] 渐隐到透明，
 * 内容不会在顶栏下沿被一刀切断。
 */
@Composable
private fun TopBarMaterial(
    backdrop: Backdrop,
    color: Color,
    solidHeight: Dp,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha()
                // 下面用 DstIn 给整层做下沿渐隐，需要单独的离屏图层
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(color.copy(alpha = 0.82f))
                val solid = (solidHeight.toPx() / size.height).coerceIn(0f, 1f)
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, solid to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
            // 饱和度只稍微提一点：红色的「播放全部」按钮滑到下面时，顶栏不会被染成一片粉红
            .backdropBlur(backdrop, RectangleShape, sigma = 10.dp, saturation = 1.3f)
    )
}
