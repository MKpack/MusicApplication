package com.example.musicapplication.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.musicapplication.ui.mainPage.audioPlayer.CoverColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 三团柔光各自漂一圈的时间（毫秒），互相错开，看不出循环
private const val Blob1Period = 23_000L
private const val Blob2Period = 29_000L
private const val Blob3Period = 37_000L

// 切歌时颜色过渡的时长
private const val ColorChangeMillis = 700

/**
 * 播放页背景：用封面的 3 种重点颜色（见 [CoverColors]）铺成一片缓慢流动的柔光。
 * - 不模糊封面原图，也不用面积最大的颜色：白底、黑底的封面不会变成一片灰；
 * - 第一种重点色压暗后铺底，三团柔光在左上、右边、左下慢慢漂；
 * - 颜色按相对亮度压到中等偏暗，黄色、绿色这种本身很亮的颜色压得更多，白色的字在上面始终看得清；
 * - 黑白灰的封面就是一片灰；系统关掉了动画时柔光不动。
 *
 * 柔光的位置按当前帧的时间算，同一帧里画的几份背景（播放器卡片里的、全屏的）柔光位置都一样，叠在一起没有接缝。
 * 只有显示出来的时候才每帧重画，不显示时不要放进界面里。
 */
@Composable
fun CoverColorBackground(
    colors: CoverColors,
    modifier: Modifier = Modifier,
) {
    val tones = remember(colors) { BackgroundTones.from(colors) }

    val base by animateColorAsState(tones.base, tween(ColorChangeMillis), label = "backgroundBase")
    val color1 by animateColorAsState(tones.blobs[0], tween(ColorChangeMillis), label = "backgroundBlob1")
    val color2 by animateColorAsState(tones.blobs[1], tween(ColorChangeMillis), label = "backgroundBlob2")
    val color3 by animateColorAsState(tones.blobs[2], tween(ColorChangeMillis), label = "backgroundBlob3")

    // 帧时间和 System.nanoTime() 是同一个时间基准，先用它，第一帧的柔光就在对的位置
    var timeMillis by remember { mutableLongStateOf(System.nanoTime() / 1_000_000L) }
    LaunchedEffect(Unit) {
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameMillis { timeMillis = it }
        }
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                // 渐变只在尺寸、颜色变的时候重新建；柔光移动时平移画布，不用每帧新建渐变
                val baseBrush = Brush.verticalGradient(listOf(base, lerpToBlack(base, 0.35f)))
                val blob1 = Blob(color1, radius = w * 1.05f)
                val blob2 = Blob(color2, radius = w * 0.95f)
                val blob3 = Blob(color3, radius = w * 1.15f)
                // 上下再压暗一点：顶部的状态栏、底部的控制按钮更清楚
                val scrim = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.22f),
                    0.32f to Color.Transparent,
                    0.68f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.38f)
                )

                onDrawBehind {
                    // 时间只在绘制阶段读：柔光移动时只重画这一块，播放页不重组
                    val time = timeMillis
                    val a1 = angle(time, Blob1Period)
                    val a2 = angle(time, Blob2Period)
                    val a3 = angle(time, Blob3Period) + 2f

                    drawRect(baseBrush)
                    blob1.draw(this, Offset(w * (0.28f + 0.14f * cos(a1)), h * (0.2f + 0.06f * sin(a1))))
                    blob2.draw(this, Offset(w * (0.92f + 0.1f * sin(a2)), h * (0.55f + 0.08f * cos(a2))))
                    blob3.draw(this, Offset(w * (0.18f + 0.12f * cos(a3)), h * (0.88f + 0.05f * sin(a3))))
                    drawRect(scrim)
                }
            }
    )
}

/** [time] 时刻转了一圈里的多少，换成弧度 */
private fun angle(time: Long, period: Long): Float = (2.0 * PI * (time % period) / period).toFloat()

/** 一团从中心向外淡掉的柔光。渐变以 (radius, radius) 为圆心建一次，画的时候平移到实际位置 */
private class Blob(color: Color, private val radius: Float) {
    private val brush = Brush.radialGradient(
        0f to color,
        0.45f to color.copy(alpha = 0.55f),
        1f to color.copy(alpha = 0f),
        center = Offset(radius, radius),
        radius = radius
    )

    fun draw(scope: DrawScope, center: Offset) {
        scope.translate(left = center.x - radius, top = center.y - radius) {
            drawCircle(brush = brush, radius = radius, center = Offset(radius, radius))
        }
    }
}

private fun lerpToBlack(color: Color, fraction: Float): Color = Color(
    red = color.red * (1f - fraction),
    green = color.green * (1f - fraction),
    blue = color.blue * (1f - fraction),
    alpha = color.alpha
)

/** 背景上实际用的颜色：底色和三团柔光，都是从重点色调出来的 */
private class BackgroundTones(val base: Color, val blobs: List<Color>) {
    companion object {
        fun from(colors: CoverColors): BackgroundTones {
            val minSaturation = if (colors.isMonochrome) 0f else 0.3f
            val blobs = colors.keyColors.mapIndexed { index, color ->
                if (index == 0) {
                    color.tone(luminance = 0.1f..0.2f, saturation = minSaturation..0.85f)
                } else {
                    color.tone(luminance = 0.07f..0.16f, saturation = minSaturation..0.85f)
                }
            }
            val base = colors.keyColors.first().tone(luminance = 0.02f..0.045f, saturation = (minSaturation * 0.8f)..0.7f)
            return BackgroundTones(base, blobs)
        }
    }
}

/**
 * 保留色相，把鲜艳程度限制在 [saturation] 里，再调明暗，让相对亮度（人眼感觉的亮度）落在 [luminance] 里。
 * 按相对亮度而不是 HSL 的亮度来调：同样的 HSL 亮度，黄色看起来比蓝色亮得多，白字会看不清。
 */
private fun Color.tone(
    luminance: ClosedFloatingPointRange<Float>,
    saturation: ClosedFloatingPointRange<Float>,
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(saturation.start, saturation.endInclusive)
    fun luminanceAt(lightness: Float) = Color.hsl(hue, sat, lightness).luminance()

    var lightness = hsl[2]
    if (luminanceAt(lightness) > luminance.endInclusive) {
        var low = 0f
        var high = lightness
        repeat(12) {
            val mid = (low + high) / 2f
            if (luminanceAt(mid) > luminance.endInclusive) high = mid else low = mid
        }
        lightness = low
    } else if (luminanceAt(lightness) < luminance.start) {
        var low = lightness
        var high = 1f
        repeat(12) {
            val mid = (low + high) / 2f
            if (luminanceAt(mid) < luminance.start) low = mid else high = mid
        }
        lightness = high
    }
    return Color.hsl(hue, sat, lightness)
}
