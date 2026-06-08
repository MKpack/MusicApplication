package com.example.musicapplication.ui.mainPage.profile.setting

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

@Composable
fun ProfileSettingScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    profileSettingViewModel: ProfileSettingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var autoRecordRecent by remember { mutableStateOf(true) }
    var rememberProgress by remember { mutableStateOf(false) }

    LaunchedEffect(profileSettingViewModel) {
        profileSettingViewModel.message.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LocalMusicThemeColors.current.bgTop,
                        LocalMusicThemeColors.current.bgBottom
                    )
                )
            ),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = 140.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingTopBar(onBack = onBack)
        }

        item {
            SettingGroup(
                title = "播放设置",
                items = listOf(
                    SettingItem.SwitchItem(
                        title = "自动记录最近播放",
                        subtitle = "播放成功后写入本地最近播放",
                        icon = Icons.Default.History,
                        checked = autoRecordRecent,
                        onCheckedChange = { autoRecordRecent = it }
                    ),
                    SettingItem.SwitchItem(
                        title = "记住播放进度",
                        subtitle = "下次打开时恢复到上次播放位置",
                        icon = Icons.Default.PlayCircle,
                        checked = rememberProgress,
                        onCheckedChange = { rememberProgress = it }
                    )
                )
            )
        }

        item {
            SettingGroup(
                title = "本地数据",
                items = listOf(
                    SettingItem.ActionItem(
                        title = "清空最近播放",
                        subtitle = "删除本机保存的最近播放记录",
                        icon = Icons.Default.DeleteSweep,
                        onClick = { }
                    ),
                    SettingItem.ActionItem(
                        title = "清理图片缓存",
                        subtitle = "释放封面与头像缓存空间",
                        icon = Icons.Default.CleaningServices,
                        onClick = { profileSettingViewModel.clearArtworkCache() }
                    )
                )
            )
        }

        item {
            SettingGroup(
                title = "账号",
                items = listOf(
                    SettingItem.ActionItem(
                        title = "退出登录",
                        subtitle = "清除登录状态并返回登录页",
                        icon = Icons.Default.Logout,
                        onClick = onLogout
                    )
                )
            )
        }
    }
}

@Composable
private fun SettingTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.textPrimary,
                modifier = Modifier.size(25.dp)
            )
        }

        Text(
            text = "设置",
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private sealed class SettingItem {
    abstract val title: String
    abstract val subtitle: String
    abstract val icon: ImageVector

    data class SwitchItem(
        override val title: String,
        override val subtitle: String,
        override val icon: ImageVector,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItem()

    data class ActionItem(
        override val title: String,
        override val subtitle: String,
        override val icon: ImageVector,
        val onClick: () -> Unit
    ) : SettingItem()
}

@Composable
private fun SettingGroup(
    title: String,
    items: List<SettingItem>
) {
    Column {
        Text(
            text = title,
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(LocalMusicThemeColors.current.surface)
                .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
                .padding(vertical = 6.dp)
        ) {
            items.forEachIndexed { index, item ->
                SettingRow(item)
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp),
                        color = LocalMusicThemeColors.current.divider,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    item: SettingItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .then(
                if (item is SettingItem.ActionItem) {
                    Modifier.clickable(onClick = item.onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(LocalMusicThemeColors.current.primarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = LocalMusicThemeColors.current.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = item.title,
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.subtitle,
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (item) {
            is SettingItem.SwitchItem -> {
                Switch(
                    checked = item.checked,
                    onCheckedChange = item.onCheckedChange
                )
            }

            is SettingItem.ActionItem -> {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = LocalMusicThemeColors.current.iconMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
