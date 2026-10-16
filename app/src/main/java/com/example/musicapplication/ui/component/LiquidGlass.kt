package com.example.musicapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp
import com.example.musicapplication.ui.theme.NeutralColors

/**
 * 液态玻璃的外观：半透明底色 + 顶部的柔和光泽 + 左上亮、右下暗的渐变描边。
 * 背景模糊不在这里做：放在纯色背景上看不出模糊效果，只有欢迎页这种背后有图的地方才另外加。
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
