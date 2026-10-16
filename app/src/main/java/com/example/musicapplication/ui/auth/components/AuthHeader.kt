package com.example.musicapplication.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/** 注册、找回密码都是 3 步：邮箱 → 验证码 → 密码（完成页不算） */
const val STEP_COUNT = 3

/** 顶部的步骤进度条：3 段小横条，走过的步骤是主题色 */
@Composable
fun StepIndicator(
    current: Int,
    modifier: Modifier = Modifier,
    count: Int = STEP_COUNT,
) {
    val colors = LocalMusicThemeColors.current
    Row(
        modifier = modifier.semantics { contentDescription = "第 $current 步，共 $count 步" },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(count) { index ->
            val color by animateColorAsState(
                targetValue = if (index < current) colors.primary else colors.neutral.fill,
                label = "stepColor"
            )
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 5.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

/** 每一步顶部的符号圆：76dp 的主题色浅底圆 + 40dp 的图标 */
@Composable
fun SymbolBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMusicThemeColors.current
    Box(
        modifier = modifier
            .size(76.dp)
            .background(colors.primaryTint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** 页面大标题 + 下面的灰色说明。说明里可以有加粗的邮箱、可点的链接，所以用 AnnotatedString */
@Composable
fun AuthHeadline(
    title: String,
    subtitle: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    val neutral = LocalMusicThemeColors.current.neutral
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = neutral.label,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = neutral.labelSecondary,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** 符号圆 + 标题 + 说明，注册和找回密码每一步的页头 */
@Composable
fun AuthHero(
    icon: ImageVector,
    title: String,
    subtitle: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SymbolBadge(icon = icon)
        Spacer(modifier = Modifier.height(18.dp))
        AuthHeadline(title = title, subtitle = subtitle)
    }
}
