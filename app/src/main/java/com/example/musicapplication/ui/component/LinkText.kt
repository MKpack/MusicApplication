package com.example.musicapplication.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 「灰色前缀 + 主题色链接」，例如「还没有账号？创建账号」。只有链接部分可以点，按下时变淡。
 * 链接上下各留 10dp 的点击区域，文字小也容易点中。
 */
@Composable
fun LinkText(
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    enabled: Boolean = true,
    fontSize: TextUnit = 15.sp,
) {
    val colors = LocalMusicThemeColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (prefix != null) {
            Text(text = prefix, color = colors.neutral.labelSecondary, fontSize = fontSize)
        }
        Text(
            text = link,
            color = if (enabled) colors.primary else colors.neutral.labelTertiary,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .graphicsLayer { alpha = if (pressed) 0.5f else 1f }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(vertical = 10.dp)
        )
    }
}
