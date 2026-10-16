package com.example.musicapplication.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 在当前页面显示期间切换状态栏图标的颜色。[darkIcons] 为 false 时是白色图标，用在深色或彩色的背景上。
 *
 * 页面切换时新旧两页会同时存在一小段时间（转场动画），旧页面离开得比新页面进入晚，
 * 所以离开时不能简单地“恢复成进入前的值”，否则会把新页面刚设好的颜色改掉。
 * 这里按进入顺序记下每个页面的要求，离开时交给最后进入的那个页面；都离开了就恢复默认（跟随深色模式）。
 */
@Composable
fun StatusBarIconsEffect(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val defaultDarkIcons = !isSystemInDarkTheme()

    DisposableEffect(darkIcons, defaultDarkIcons) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val request = StatusBarRequest(darkIcons)
            activeRequests += request
            controller.isAppearanceLightStatusBars = darkIcons
            onDispose {
                activeRequests -= request
                controller.isAppearanceLightStatusBars = activeRequests.lastOrNull()?.darkIcons ?: defaultDarkIcons
            }
        }
    }
}

// 用对象本身区分每个页面的请求（不是 data class），两个页面要求相同颜色时也不会互相删掉
private class StatusBarRequest(val darkIcons: Boolean)

// 只在主线程（组合和副作用里）读写
private val activeRequests = mutableListOf<StatusBarRequest>()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
