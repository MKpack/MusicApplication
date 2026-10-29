package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.musicapplication.ui.theme.NeutralColors
import kotlin.math.ceil

/**
 * 液态玻璃的外观：半透明底色 + 顶部的柔和光泽 + 左上亮、右下暗的渐变描边。
 * 这个版本不带背景模糊：放在纯色背景上看不出模糊效果。背后有内容时用带 [Backdrop] 的版本。
 */
fun Modifier.liquidGlass(shape: Shape, fill: Color, neutral: NeutralColors): Modifier {
    val rimAlphas = if (neutral.isDark) floatArrayOf(0.55f, 0.16f, 0.03f, 0.3f) else floatArrayOf(1f, 0.45f, 0.06f, 0.7f)
    val sheenAlpha = if (neutral.isDark) 0.14f else 0.42f
    return this
        .background(fill, shape)
        .drawWithCache {
            val sheen = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = sheenAlpha), Color.Transparent),
                center = Offset(size.width / 2, -size.height * 0.3f),
                radius = size.width * 0.75f
            )
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind { drawOutline(outline, sheen) }
        }
        .border(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                0f to Color.White.copy(alpha = rimAlphas[0]),
                0.26f to Color.White.copy(alpha = rimAlphas[1]),
                0.55f to Color.White.copy(alpha = rimAlphas[2]),
                1f to Color.White.copy(alpha = rimAlphas[3])
            ),
            shape = shape
        )
}

/** 带背景模糊的液态玻璃：先画模糊后的 [backdrop]，再叠半透明底色、光泽和描边 */
fun Modifier.liquidGlass(
    shape: Shape,
    fill: Color,
    neutral: NeutralColors,
    backdrop: Backdrop,
    sigma: Dp = 10.dp,
): Modifier = backdropBlur(backdrop, shape, sigma).liquidGlass(shape, fill, neutral)

/** 玻璃控件下面柔和的投影，深色模式下更深一些。[alpha] 在绘制时读取，用于跟着动画淡出 */
fun Modifier.glassShadow(shape: Shape, neutral: NeutralColors, alpha: () -> Float = { 1f }): Modifier = softShadow(
    shape = shape,
    color = Color.Black.copy(alpha = if (neutral.isDark) 0.5f else 0.12f),
    radius = 14.dp,
    offsetY = 6.dp,
    alpha = alpha
)

/**
 * 柔和的投影：把形状模糊后画在控件下面，效果和 Modifier.dropShadow 相同（模糊半径的换算一样）。
 *
 * 不用 dropShadow 的原因：它每遇到一个新尺寸，都要在主线程用 CPU 重新模糊生成一张阴影图并上传成新纹理，
 * 生成过的还会一直缓存着。底栏收起、迷你播放器移动的动画里尺寸每帧都在变，滑动列表时会明显掉帧。
 * 这里改成把形状录进一个 GraphicsLayer，用 RenderEffect 在 GPU 上模糊，尺寸变了只需要重录一个形状。
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color,
    radius: Dp,
    offsetY: Dp = 0.dp,
    alpha: () -> Float = { 1f },
): Modifier = drawWithCache {
    val radiusPx = radius.toPx()
    // 模糊会往外扩散大约两倍半径，图层四周多留出这么多
    val pad = ceil(radiusPx * 2).toInt()
    val outline = shape.createOutline(size, layoutDirection, this)
    val layer = obtainGraphicsLayer()
    layer.renderEffect = if (radiusPx > 0f) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null
    layer.topLeft = IntOffset(-pad, -pad + offsetY.roundToPx())
    layer.record(size = IntSize(ceil(size.width).toInt() + pad * 2, ceil(size.height).toInt() + pad * 2)) {
        translate(pad.toFloat(), pad.toFloat()) {
            drawOutline(outline, color)
        }
    }
    onDrawWithContent {
        layer.alpha = alpha().coerceIn(0f, 1f)
        drawLayer(layer)
        drawContent()
    }
}
