package com.example.musicapplication.ui.auth.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.musicapplication.ui.auth.welcome.WelcomeCovers
import com.example.musicapplication.ui.auth.welcome.rememberCoverPainters
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import kotlinx.coroutines.delay

/** 完成页的大徽章：主题色渐变圆 + 白色图标 + 两圈淡淡的光环，出现时轻轻弹一下 */
@Composable
fun SuccessBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val primary = LocalMusicThemeColors.current.primary
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .size(188.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(188.dp)
                .background(primary.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(148.dp)
                .background(primary.copy(alpha = 0.09f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(radius = 24.dp, color = primary.copy(alpha = 0.4f), offset = DpOffset(0.dp, 12.dp))
                )
                .background(
                    Brush.linearGradient(listOf(lerp(primary, Color.White, 0.18f), primary, lerp(primary, Color.Black, 0.06f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

/** 注册成功页围着徽章浮动的曲库封面：位置相对中心点，依次弹出后上下轻轻浮动 */
@Composable
fun FloatingCovers(modifier: Modifier = Modifier) {
    val painters = rememberCoverPainters(FloatingCoverSpecs.map { WelcomeCovers[it.cover] })
    val transition = rememberInfiniteTransition(label = "floatingCovers")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        FloatingCoverSpecs.forEachIndexed { index, spec ->
            val bob by transition.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.periodMillis, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "coverBob$index"
            )
            val appear = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(150L + 70L * index)
                appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
            }
            val shape = RoundedCornerShape(spec.size * 0.22f)
            Image(
                painter = painters[index],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(spec.x, spec.y)
                    .graphicsLayer {
                        translationY = bob * 4.dp.toPx()
                        rotationZ = spec.rotation
                        alpha = appear.value.coerceIn(0f, 1f)
                        scaleX = 0.5f + 0.5f * appear.value
                        scaleY = 0.5f + 0.5f * appear.value
                    }
                    .size(spec.size)
                    .dropShadow(shape, Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.18f), offset = DpOffset(0.dp, 5.dp)))
                    .clip(shape)
            )
        }
    }
}

private class FloatingCoverSpec(
    val cover: Int,
    val x: Dp,
    val y: Dp,
    val size: Dp,
    val rotation: Float,
    val periodMillis: Int,
)

// 位置参考设计图：左上、上方两张、右上、左下、右下，大小和角度错开，看起来是随手摆的
private val FloatingCoverSpecs = listOf(
    FloatingCoverSpec(cover = 0, x = (-128).dp, y = (-76).dp, size = 54.dp, rotation = -8f, periodMillis = 3200),
    FloatingCoverSpec(cover = 8, x = (-36).dp, y = (-128).dp, size = 40.dp, rotation = 6f, periodMillis = 2700),
    FloatingCoverSpec(cover = 5, x = 64.dp, y = (-136).dp, size = 44.dp, rotation = -5f, periodMillis = 3600),
    FloatingCoverSpec(cover = 3, x = 130.dp, y = (-90).dp, size = 50.dp, rotation = 9f, periodMillis = 2900),
    FloatingCoverSpec(cover = 12, x = (-142).dp, y = 70.dp, size = 44.dp, rotation = -10f, periodMillis = 3400),
    FloatingCoverSpec(cover = 6, x = 136.dp, y = 68.dp, size = 58.dp, rotation = 6f, periodMillis = 3000),
)
