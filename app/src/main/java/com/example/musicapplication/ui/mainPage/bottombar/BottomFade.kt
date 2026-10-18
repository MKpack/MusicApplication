package com.example.musicapplication.ui.mainPage.bottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.musicapplication.ui.component.Backdrop
import com.example.musicapplication.ui.component.backdropBlur

/**
 * 底部渐隐：内容滑到底栏下面时逐渐变淡、变模糊，Tab 文字更清楚。
 * 从屏幕底部往上 180dp：页面底色从 88% 过渡到 55%（45% 高度处）再到 0；模糊也是越往下越明显。
 */
@Composable
fun BottomFade(
    backdrop: Backdrop,
    color: Color,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer {
                this.alpha = alpha()
                // 下面用 DstIn 给模糊层做渐变遮罩，需要单独的离屏图层
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black), blendMode = BlendMode.DstIn)
                drawRect(
                    Brush.verticalGradient(
                        0f to color.copy(alpha = 0f),
                        0.55f to color.copy(alpha = 0.55f),
                        1f to color.copy(alpha = 0.88f)
                    )
                )
            }
            .backdropBlur(backdrop, RectangleShape, sigma = 4.dp, saturation = 1f)
    )
}
