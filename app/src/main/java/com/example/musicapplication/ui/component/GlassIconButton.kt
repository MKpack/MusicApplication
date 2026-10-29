package com.example.musicapplication.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 液态玻璃圆按钮，默认 44dp，用作返回、关闭；底栏的搜索按钮也用它（传入更大的尺寸）。深色模式下玻璃自动变暗。
 * 背后有内容时传入 [backdrop]，玻璃会透出模糊后的内容。
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    iconSize: Dp = 21.dp,
    tint: Color = LocalMusicThemeColors.current.neutral.label,
    enabled: Boolean = true,
) {
    val neutral = LocalMusicThemeColors.current.neutral
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "glassButtonScale")

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glassShadow(CircleShape, neutral)
            .then(
                if (backdrop != null) Modifier.liquidGlass(CircleShape, neutral.glass, neutral, backdrop)
                else Modifier.liquidGlass(CircleShape, neutral.glass, neutral)
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
