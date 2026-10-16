package com.example.musicapplication.ui.auth.welcome

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.component.BrandLogo
import com.example.musicapplication.ui.component.PrimaryCapsuleButton
import com.example.musicapplication.ui.component.SecondaryCapsuleButton
import com.example.musicapplication.ui.component.StatusBarIconsEffect
import com.example.musicapplication.ui.component.liquidGlass
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import com.example.musicapplication.ui.theme.MusicThemePreset
import kotlin.math.roundToInt

private val PanelShape = RoundedCornerShape(34.dp)

/** 未登录时的起始页：上面是缓慢漂移的封面墙，下面是液态玻璃面板，「创建账号」和「登录」两个入口分开 */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
) {
    // 封面墙顶部是暗角，状态栏用白色图标
    StatusBarIconsEffect(darkIcons = false)
    WelcomeContent(onCreateAccount = onCreateAccount, onLogin = onLogin)
}

@Composable
private fun WelcomeContent(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
) {
    val colors = LocalMusicThemeColors.current
    val drift = rememberCoverWallDrift()
    // 背景和玻璃面板里的模糊副本共用同一组图片
    val painters = rememberCoverPainters()
    // 玻璃面板相对整页的位置，用来让面板里的模糊副本和背景对齐
    var container by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }
    // 模糊效果第一次绘制前要做不少准备，会明显拖慢第一帧，系统启动页也就退得晚。
    // 所以等这一页真正画出第一帧之后再加上模糊副本，并且淡入，避免面板突然变样
    val view = LocalView.current
    var firstFrameDrawn by remember { mutableStateOf(false) }
    val backdropAlpha by animateFloatAsState(if (firstFrameDrawn) 1f else 0f, tween(250), label = "glassBackdropAlpha")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.neutral.groupedBackground)
            .onGloballyPositioned { container = it }
            .drawBehind {
                if (!firstFrameDrawn) view.post { firstFrameDrawn = true }
            }
    ) {
        val pageSize = DpSize(maxWidth, maxHeight)

        CoverWall(
            painters = painters,
            drift = drift,
            modifier = Modifier.fillMaxSize()
        )

        // 顶部暗角：白色状态栏图标在彩色封面上也看得清
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 10.dp, end = 10.dp, bottom = 6.dp)
                .fillMaxWidth()
                .onGloballyPositioned { panel ->
                    container?.let { panelOffset = it.localPositionOf(panel, Offset.Zero) }
                }
                .dropShadow(PanelShape, Shadow(radius = 24.dp, color = Color.Black.copy(alpha = 0.18f), offset = DpOffset(0.dp, 10.dp)))
                .clip(PanelShape)
                // 模糊副本出现之前，先用页面底色挡住面板后面清晰的封面墙
                .background(colors.neutral.groupedBackground)
        ) {
            if (firstFrameDrawn) {
                GlassBackdrop(
                    painters = painters,
                    drift = drift,
                    pageSize = pageSize,
                    panelOffset = panelOffset,
                    alpha = backdropAlpha,
                    modifier = Modifier.matchParentSize()
                )
            }
            GlassSurface(modifier = Modifier.matchParentSize())
            PanelContent(onCreateAccount = onCreateAccount, onLogin = onLogin)
        }
    }
}

/**
 * 玻璃面板的背景：在面板里再画一份封面墙，按面板的位置反向偏移，和背景严丝合缝，
 * 再整体模糊、提高饱和度。背景是我们自己画的封面墙，所以不需要额外的模糊库。
 */
@Composable
private fun GlassBackdrop(
    painters: List<Painter>,
    drift: CoverWallDrift,
    pageSize: DpSize,
    panelOffset: Offset,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val backdropEffect = remember(density) { glassBackdropEffect(sigmaPx = with(density) { 22.dp.toPx() }) }

    Box(
        modifier = modifier
            .graphicsLayer {
                renderEffect = backdropEffect
                clip = true
                this.alpha = alpha
            }
            // 副本要铺上和页面一样的底色：封面之间的缝隙如果是透明的，模糊后会变成半透明，
            // 背景上那份清晰的封面墙就会从这些地方透出来
            .background(LocalMusicThemeColors.current.neutral.groupedBackground)
    ) {
        CoverWall(
            painters = painters,
            drift = drift,
            modifier = Modifier
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .requiredSize(pageSize)
                .offset { IntOffset(-panelOffset.x.roundToInt(), -panelOffset.y.roundToInt()) }
        )
    }
}

// 背景模糊 + 饱和度提高到 1.7 倍，接近 iOS 液态玻璃的通透感（RenderEffect，Android 12+）
private fun glassBackdropEffect(sigmaPx: Float): RenderEffect {
    // RenderEffect 的模糊半径和高斯模糊的 sigma 之间要换算一下
    val radius = (sigmaPx - 0.5f) / 0.57735f
    val saturation = ColorMatrix().apply { setSaturation(1.7f) }
    return android.graphics.RenderEffect.createChainEffect(
        android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturation)),
        android.graphics.RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
    ).asComposeRenderEffect()
}

/** 玻璃面板的底色、顶部光泽和渐变描边 */
@Composable
private fun GlassSurface(modifier: Modifier = Modifier) {
    val neutral = LocalMusicThemeColors.current.neutral
    Box(modifier = modifier.liquidGlass(PanelShape, neutral.glassStrong, neutral))
}

@Composable
private fun PanelContent(onCreateAccount: () -> Unit, onLogin: () -> Unit) {
    val neutral = LocalMusicThemeColors.current.neutral
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandLogo(size = 72.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Shared Music",
            color = neutral.label,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "好音乐，一起听。",
            color = neutral.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "在线听歌、收藏喜欢、下载离线听",
            color = neutral.labelSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryCapsuleButton(text = "创建账号", onClick = onCreateAccount)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryCapsuleButton(text = "登录", onClick = onLogin)
    }
}

@Preview(name = "欢迎页", showSystemUi = true)
@Composable
private fun WelcomePreview() {
    MusicApplicationTheme(themePreset = MusicThemePreset.Red) {
        WelcomeContent(onCreateAccount = {}, onLogin = {})
    }
}

@Preview(name = "欢迎页 · 深色", showSystemUi = true)
@Composable
private fun WelcomeDarkPreview() {
    MusicApplicationTheme(darkTheme = true, themePreset = MusicThemePreset.Red) {
        WelcomeContent(onCreateAccount = {}, onLogin = {})
    }
}
