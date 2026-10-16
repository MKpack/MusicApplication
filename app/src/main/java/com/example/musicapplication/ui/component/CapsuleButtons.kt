package com.example.musicapplication.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 主按钮：54dp 高的胶囊，主题色渐变 + 顶部高光 + 同色阴影。
 * 请求进行中（[loading]）保持主题色并转圈，看起来是“处理中”而不是“不可用”，同时不能重复点击。
 */
@Composable
fun PrimaryCapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalMusicThemeColors.current
    val primary = colors.primary
    val highlighted = enabled || loading
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "primaryButtonScale")

    val decorated = if (highlighted) {
        Modifier
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 16.dp, color = primary.copy(alpha = 0.45f), spread = (-4).dp, offset = DpOffset(0.dp, 8.dp))
            )
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(lerp(primary, Color.White, 0.12f), primary, lerp(primary, Color.Black, 0.05f))))
            // 上半部分的柔和光泽 + 顶边 1dp 的白色内高光
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.18f), 0.55f to Color.Transparent))
            .innerShadow(CircleShape, Shadow(radius = 0.dp, color = Color.White.copy(alpha = 0.35f), offset = DpOffset(0.dp, 1.dp)))
    } else {
        Modifier
            .clip(CircleShape)
            .background(colors.neutral.fill)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(decorated)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else colors.neutral.labelTertiary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** 次按钮：白底胶囊 + 主题色文字，放在玻璃或彩色背景上；深色模式下换成半透明的白底 */
@Composable
fun SecondaryCapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicThemeColors.current
    val dark = colors.neutral.isDark
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "secondaryButtonScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(radius = 12.dp, color = Color.Black.copy(alpha = if (dark) 0f else 0.12f), offset = DpOffset(0.dp, 6.dp))
            )
            .clip(CircleShape)
            .background(if (dark) Color.White.copy(alpha = 0.14f) else Color.White)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
