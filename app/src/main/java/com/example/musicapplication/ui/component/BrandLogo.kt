package com.example.musicapplication.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.musicapplication.R

// Logo 本身固定是樱桃红，阴影也用品牌红，不跟随主题色变化
private val BrandRed = Color(0xFFFF2D55)

/** 品牌 Logo：樱桃红圆角方块 + 白色耳机爱心，用在欢迎页、登录页等展示品牌的地方 */
@Composable
fun BrandLogo(size: Dp, modifier: Modifier = Modifier) {
    // 圆角约为边长的 23.5%，和 iOS 图标的比例一致
    val shape = RoundedCornerShape(size * 0.235f)
    Image(
        painter = painterResource(R.drawable.ic_brand_logo),
        contentDescription = "Shared Music",
        modifier = modifier
            .size(size)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = size * 0.22f,
                    color = BrandRed.copy(alpha = 0.45f),
                    spread = -(size * 0.06f),
                    offset = DpOffset(0.dp, size * 0.14f)
                )
            )
            .clip(shape)
    )
}
