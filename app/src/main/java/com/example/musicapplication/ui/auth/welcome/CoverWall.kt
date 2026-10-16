package com.example.musicapplication.ui.auth.welcome

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.musicapplication.R
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlin.math.floor

private const val COLUMN_COUNT = 4
// 每列一组放 8 张封面，往下无限重复；漂移满一组的高度就无缝回到起点
private const val TILES_PER_SET = 8
private val TILE_GAP = 10.dp
private val TILE_CORNER = 14.dp
// 每列起始位置错开（单位：一个方块的边长），和设计稿一致
private val START_OFFSETS = floatArrayOf(-0.29f, -0.79f, -0.5f, -1.02f)
// 每列漂移一组所用的时间，速度在每秒 10dp 左右，各列略有不同
private val COLUMN_LOOP_MILLIS = intArrayOf(100_000, 120_000, 110_000, 130_000)

/** 封面墙每一列的漂移进度（0 到 1 循环）。背景和玻璃面板里的模糊副本共用同一份，两者才能对齐 */
@Stable
class CoverWallDrift internal constructor(internal val progress: List<State<Float>>)

@Composable
fun rememberCoverWallDrift(): CoverWallDrift {
    // 系统开了「移除动画」或者在预览里时，封面墙保持静止
    val animate = remember { ValueAnimator.areAnimatorsEnabled() } && !LocalInspectionMode.current
    val transition = rememberInfiniteTransition(label = "coverWall")
    val progress = COLUMN_LOOP_MILLIS.map { millis ->
        if (animate) {
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(millis, easing = LinearEasing)),
                label = "coverWallColumn"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }
    }
    return CoverWallDrift(progress)
}

/** 欢迎页封面墙用的 16 张封面，打包在 res/drawable-nodpi 里，每张 360px */
val WelcomeCovers = listOf(
    R.drawable.welcome_cover_01, R.drawable.welcome_cover_02, R.drawable.welcome_cover_03, R.drawable.welcome_cover_04,
    R.drawable.welcome_cover_05, R.drawable.welcome_cover_06, R.drawable.welcome_cover_07, R.drawable.welcome_cover_08,
    R.drawable.welcome_cover_09, R.drawable.welcome_cover_10, R.drawable.welcome_cover_11, R.drawable.welcome_cover_12,
    R.drawable.welcome_cover_13, R.drawable.welcome_cover_14, R.drawable.welcome_cover_15, R.drawable.welcome_cover_16,
)

/**
 * 每张封面一个图片对象，背景和模糊副本共用。
 * 封面是打包在 App 里的小图，直接同步读取：打开欢迎页时封面墙就是完整的，不会先出现灰色占位块再一张张冒出来
 */
@Composable
fun rememberCoverPainters(covers: List<Int> = WelcomeCovers): List<Painter> =
    covers.map { painterResource(it) }

/**
 * 封面墙：4 列方块错落排列，1、3 列往上、2、4 列往下缓慢漂移。4 列总宽是屏幕宽的 1.2 倍，左右各露出半列。
 *
 * 整面墙用一块画布绘制，每帧只画屏幕里看得见的方块；漂移进度只在绘制阶段读取，不会触发重组和重新布局。
 */
@Composable
fun CoverWall(
    painters: List<Painter>,
    drift: CoverWallDrift,
    modifier: Modifier = Modifier,
) {
    val placeholder = LocalMusicThemeColors.current.neutral.fill
    val columns = remember(painters) { coverColumns(painters) }

    Spacer(
        modifier = modifier
            .clipToBounds()
            .drawWithCache {
                val gap = TILE_GAP.toPx()
                val tile = (size.width * 1.2f - gap * (COLUMN_COUNT - 1)) / COLUMN_COUNT
                val step = tile + gap
                val setHeight = step * TILES_PER_SET
                val left = (size.width - (tile * COLUMN_COUNT + gap * (COLUMN_COUNT - 1))) / 2
                val tileSize = Size(tile, tile)
                val tileShape = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, tile, tile, CornerRadius(TILE_CORNER.toPx())))
                }

                onDrawBehind {
                    columns.forEachIndexed { col, column ->
                        if (column.isEmpty()) return@forEachIndexed
                        val progress = drift.progress[col].value
                        // 第 0 个方块的位置：起始错位 + 漂移。往上走的列从 0 移到 -一组，往下走的列从 -一组移到 0
                        val shift = if (col % 2 == 0) -progress * setHeight else (progress - 1f) * setHeight
                        val firstTop = START_OFFSETS[col] * tile + shift
                        val x = left + col * step
                        var index = floor(-firstTop / step).toInt().coerceAtLeast(0)
                        while (true) {
                            val y = firstTop + index * step
                            if (y > size.height) break
                            translate(x, y) {
                                clipPath(tileShape) {
                                    drawRect(placeholder, size = tileSize)
                                    with(column[index % column.size]) {
                                        draw(tileSize)
                                    }
                                }
                            }
                            index++
                        }
                    }
                }
            }
    )
}

/**
 * 把封面分到每一列，每列 [perColumn] 张：
 * 单数列用第 0、2、4… 张，双数列用第 1、3、5… 张，相邻两列不会出现同一张；
 * 第 3、4 列再把顺序错开半组，和第 1、2 列区分开。
 */
internal fun <T> coverColumns(covers: List<T>, columns: Int = COLUMN_COUNT, perColumn: Int = TILES_PER_SET): List<List<T>> {
    if (covers.isEmpty()) return List(columns) { emptyList() }
    return List(columns) { col ->
        val pool = covers.filterIndexed { i, _ -> i % 2 == col % 2 }.ifEmpty { covers }
        val shift = (col / 2) * (perColumn / 2)
        List(perColumn) { row -> pool[(row + shift) % pool.size] }
    }
}
