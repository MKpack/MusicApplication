package com.example.musicapplication.ui.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.musicapplication.ui.component.GlassIconButton
import com.example.musicapplication.ui.component.PrimaryCapsuleButton
import com.example.musicapplication.ui.component.StatusBarIconsEffect
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 登录、注册、找回密码共用的页面外壳：分组背景色 + 顶栏（左上角玻璃返回按钮、中间步骤进度条）。
 * 顶栏固定不动，分步流程里切换步骤时只有 [content] 左右滑动。
 *
 * @param onBack 为 null 时不显示返回按钮（比如登录过期直接打开的登录页、注册完成页）
 * @param step 当前第几步（1..[STEP_COUNT]），为 0 时不显示进度条
 */
@Composable
fun AuthScaffold(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    step: Int = 0,
    content: @Composable () -> Unit,
) {
    val neutral = LocalMusicThemeColors.current.neutral
    // 背景是浅灰（深色模式是黑色），状态栏图标跟着切换
    StatusBarIconsEffect(darkIcons = !neutral.isDark)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(neutral.groupedBackground)
    ) {
        AuthTopBar(onBack = onBack, step = step)
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun AuthTopBar(onBack: (() -> Unit)?, step: Int) {
    Box(
        modifier = Modifier
            // 画在下面内容的上层，返回按钮的阴影才不会被下面的页面盖住
            .zIndex(1f)
            .statusBarsPadding()
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp)
    ) {
        AnimatedVisibility(
            visible = onBack != null,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GlassIconButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "返回",
                // 淡出动画期间 onBack 已经是 null，这时点击什么都不做
                onClick = { onBack?.invoke() }
            )
        }
        AnimatedVisibility(
            visible = step > 0,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StepIndicator(current = step.coerceAtLeast(1))
        }
    }
}

/**
 * 一页的内容：中间可以滚动，[bottomBar]（主按钮 + 链接）固定在底部。
 * 键盘弹出时整页抬到键盘上方，主按钮紧贴键盘；内容放不下时可以滚动。
 */
@Composable
fun AuthPage(
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
        if (bottomBar != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                bottomBar()
            }
        }
    }
}

/**
 * 底部的主按钮 + 下面一行链接。没有链接的页面也留出同样的高度，
 * 这样每一页的主按钮都在同一个位置，切换步骤时按钮不会上下跳。
 */
@Composable
fun AuthBottomActions(
    buttonText: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    link: (@Composable () -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PrimaryCapsuleButton(
            text = buttonText,
            onClick = onClick,
            enabled = enabled,
            loading = loading
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            link?.invoke()
        }
    }
}
