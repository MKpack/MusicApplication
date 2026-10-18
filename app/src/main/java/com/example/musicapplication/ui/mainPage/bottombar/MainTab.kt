package com.example.musicapplication.ui.mainPage.bottombar

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.musicapplication.R
import com.example.musicapplication.config.RouterConfig

/** 底部 Tab。阶段 3 加「排行榜」「资料库」时，在这里加两项并配上对应的页面 */
enum class MainTab(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(RouterConfig.HOME, R.string.home, Icons.Rounded.Home),
    PROFILE(RouterConfig.PROFILE, R.string.profile, Icons.Rounded.Person);

    companion object {
        /** 当前页面是哪个 Tab；搜索、我喜欢等二级页面返回 null */
        fun fromRoute(route: String?): MainTab? = entries.firstOrNull { it.route == route }
    }
}
