package com.example.musicapplication.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

@Composable
fun AppleMusicBackground(
    cover: Int,
    dominantColor: Color,
    secondaryColor: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 封面作为背景模糊放大
        Image(
            painter = painterResource(cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                .blur(60.dp)
        )

        // 上下黑色透明渐变提升对比度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Apple Music 风格柔光层（主色到次色）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.25f),
                            secondaryColor.copy(alpha = 0.25f)
                        ),
                        radius = 1100f
                    )
                )
        )
    }
}
