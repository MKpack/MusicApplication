package com.example.musicapplication.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.ui.theme.LocalPageBackgroundImage
import java.io.File

// 盖在自定义背景图上的页面底色有多不透明：浅色模式白 60%，深色模式黑 55%
private const val LightScrimAlpha = 0.6f
private const val DarkScrimAlpha = 0.55f

/**
 * 页面背景：默认是纯白（深色模式纯黑）。
 * 设置了自定义背景图时，图铺满整个页面、不跟着列表滚动；上面再盖一层半透明的页面底色，
 * 直接写在背景上的标题、说明文字才看得清。
 */
@Composable
fun PageBackground(modifier: Modifier = Modifier) {
    val colors = LocalMusicThemeColors.current
    val image = LocalPageBackgroundImage.current

    Box(modifier = modifier.background(colors.background)) {
        if (image != null) {
            val context = LocalContext.current
            val request = remember(image) {
                ImageRequest.Builder(context)
                    .data(File(image))
                    // 保存时已经裁成屏幕大小，直接按原尺寸读；尺寸确定了，切换页面时能直接从内存缓存拿到，不会闪一下
                    .size(Size.ORIGINAL)
                    .build()
            }
            Image(
                painter = rememberAsyncImagePainter(request),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        colors.background.copy(alpha = if (colors.neutral.isDark) DarkScrimAlpha else LightScrimAlpha)
                    )
            )
        }
    }
}
