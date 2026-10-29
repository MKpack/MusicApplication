package com.example.musicapplication.ui.mainPage.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.musicapplication.R
import com.example.musicapplication.ui.component.GlassIconButton
import com.example.musicapplication.ui.component.GlassTopBarDefaults
import com.example.musicapplication.ui.component.GlassTopBarScaffold
import com.example.musicapplication.ui.component.MusicPullToRefreshBox
import com.example.musicapplication.ui.mainPage.bottombar.LocalMainBottomInset
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.ui.theme.MusicThemePreset
import com.example.musicapplication.ui.theme.ThemeViewModel
import com.example.musicapplication.utils.LocalAudioMetaDataReader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onClickAccount: () -> Unit,
    onClickDownload: () -> Unit,
    onClickFavorite: () -> Unit,
    onClickHistory: () -> Unit,
    onClickSetting: () -> Unit,
    onClickAbout: () -> Unit,
    onClickLogout: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isThemeExpanded by remember { mutableStateOf(false) }
    var isBackgroundExpanded by remember { mutableStateOf(false) }
//    var selectedThemeKey by remember { mutableStateOf("green") }
    val themePreset by themeViewModel.themePreset.collectAsStateWithLifecycle()
    val backgroundImage by themeViewModel.backgroundImage.collectAsStateWithLifecycle()
    val isSavingBackground by themeViewModel.isSavingBackground.collectAsStateWithLifecycle()
    // 系统的照片选择器，不需要存储权限
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) themeViewModel.setBackgroundImage(uri)
    }

    LaunchedEffect(themeViewModel) {
        themeViewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        profileViewModel.consumeErrorMessage()
    }

    GlassTopBarScaffold(
        listState = listState,
        modifier = modifier,
        title = "我的"
    ) {
        MusicPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                profileViewModel.refreshProfile()
            },
            modifier = Modifier.fillMaxSize(),
            refreshError = uiState.refreshError
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    // 列表从屏幕最上面开始画（状态栏是透明的），大标题往下让出状态栏的高度
                    top = GlassTopBarDefaults.statusBarHeight + 12.dp,
                    // 内容可以滑到悬浮底栏下面，底部留出底栏和迷你播放器的高度
                    bottom = LocalMainBottomInset.current
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ProfileHeader(
                        onClickSetting = onClickSetting
                    )
                }

                item { ProfileSummary(uiState) }

                item {
                    ProfileGroup(
                        items = listOf(
                            ProfileAction("账号资料", Icons.Default.Person, onClick = onClickAccount),
                            ProfileAction("下载管理", Icons.Default.Download, onClick = onClickDownload),
                            ProfileAction("喜欢的歌曲", Icons.Default.Favorite, onClick = onClickFavorite),
                            ProfileAction("最近播放", Icons.Default.History, onClick = onClickHistory)
                        )
                    )
                }

                item {
                    ProfileGroup(
                        items = listOf(
                            ProfileAction("主题颜色", Icons.Default.Palette, isExpanded = isThemeExpanded) {
                                isThemeExpanded = !isThemeExpanded
                            },
                            ProfileAction("背景图片", Icons.Default.Wallpaper, isExpanded = isBackgroundExpanded) {
                                isBackgroundExpanded = !isBackgroundExpanded
                            },
                            ProfileAction("设置", Icons.Default.Settings, onClick = onClickSetting),
                            ProfileAction("关于音乐", Icons.Default.Info, onClick = onClickAbout),
                            ProfileAction("退出登录", Icons.Default.Logout, onClick = { onClickLogout() })
                        ),
                        expandedContent = mapOf(
                            0 to {
                                ThemeColorChooser(
                                    selectedKey = themePreset,
                                    onSelected = { option ->
                                        themeViewModel.saveThemePreset(option)
                                        Toast.makeText(context, "已选择${option.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            1 to {
                                BackgroundChooser(
                                    imagePath = backgroundImage,
                                    isSaving = isSavingBackground,
                                    onPick = {
                                        pickBackground.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onReset = { themeViewModel.resetBackground() }
                                )
                            }
                        ),
                        expandedIndexes = buildSet {
                            if (isThemeExpanded) add(0)
                            if (isBackgroundExpanded) add(1)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    onClickSetting: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // 往上滚走以后，顶栏正中会出现同样的小标题
            Text(
                text = "我的",
                color = LocalMusicThemeColors.current.textPrimary,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "管理你的音乐与偏好",
                color = LocalMusicThemeColors.current.textSecondary,
                fontSize = 14.sp
            )
        }

        GlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "设置",
            onClick = onClickSetting
        )
    }
}


@Composable
private fun ProfileSummary(
    uiState: ProfileUIState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        AvatarImage(cover = uiState.avatarUrl)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = uiState.nickName,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = uiState.email,
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("歌单", "0", Modifier.weight(1f))
            StatCard("收藏", formatCount(uiState.favoriteCount), Modifier.weight(1f))
            StatCard("播放", formatCount(uiState.playCount), Modifier.weight(1f))
        }
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 10_000 -> String.format("%.1fw", count / 10_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }
}

@Composable
private fun AvatarImage(
    cover: String? = null
) {

//    val imageUrl = buildMediaUrl(cover)
//    Log.d("AvatarImage", "cover: $imageUrl")
    Image(
        painter = rememberAsyncImagePainter(
            model = LocalAudioMetaDataReader.buildMediaUrl(cover),
            placeholder = painterResource(R.drawable.default_cover),
            error = painterResource(R.drawable.default_cover)
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(
                width = 3.dp,
                color = LocalMusicThemeColors.current.surface,
                shape = CircleShape
            )
    )
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(LocalMusicThemeColors.current.cardSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = LocalMusicThemeColors.current.textSecondary,
            fontSize = 12.sp
        )
    }
}


private data class ProfileAction(
    val title: String,
    val icon: ImageVector,
    val isExpanded: Boolean = false,
    val onClick: () -> Unit = {}
)

@Composable
private fun ProfileGroup(
    items: List<ProfileAction>,
    expandedContent: Map<Int, @Composable () -> Unit> = emptyMap(),
    expandedIndexes: Set<Int> = emptySet()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMusicThemeColors.current.surface)
            .border(1.dp, LocalMusicThemeColors.current.border, RoundedCornerShape(22.dp))
            .padding(vertical = 6.dp)

    ) {
        items.forEachIndexed { index, item ->
            ProfileRow(item)

            if (index in expandedIndexes) {
                expandedContent[index]?.invoke()
            }

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

@Composable
private fun ThemeColorChooser(
    selectedKey: MusicThemePreset,
    onSelected: (MusicThemePreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicThemePreset.entries.forEach { preset ->
                val selected = preset == selectedKey

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(preset.primary)
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = LocalMusicThemeColors.current.textPrimary,
                            shape = CircleShape
                        )
                        .clickable { onSelected(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LocalMusicThemeColors.current.surface)
                        )
                    }
                }
            }
        }

    }
}


@Composable
private fun ProfileRow(
    item: ProfileAction,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                // TODO
                onClick = item.onClick
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

        Text(
            text = item.title,
            color = LocalMusicThemeColors.current.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Icon(
            imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = LocalMusicThemeColors.current.iconMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** 背景图片：预览当前的背景，可以从相册换一张（只存在本机），也可以恢复默认的纯白（深色模式纯黑） */
@Composable
private fun BackgroundChooser(
    imagePath: String?,
    isSaving: Boolean,
    onPick: () -> Unit,
    onReset: () -> Unit
) {
    val colors = LocalMusicThemeColors.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 预览，和手机屏幕差不多的比例
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.background)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imagePath != null) {
                val request = remember(imagePath) {
                    ImageRequest.Builder(context)
                        .data(File(imagePath))
                        .size(200)
                        .build()
                }
                Image(
                    painter = rememberAsyncImagePainter(request),
                    contentDescription = "当前背景",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Text(
                    text = "默认",
                    color = colors.textHint,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (imagePath != null) "自定义图片，只保存在这台手机上" else "默认：纯白，深色模式下纯黑",
                color = colors.textSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BackgroundChooserButton(
                    text = if (isSaving) "正在保存…" else "从相册选择",
                    enabled = !isSaving,
                    emphasized = true,
                    onClick = onPick
                )
                if (imagePath != null) {
                    BackgroundChooserButton(
                        text = "恢复默认",
                        enabled = !isSaving,
                        emphasized = false,
                        onClick = onReset
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundChooserButton(
    text: String,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMusicThemeColors.current
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .then(
                if (emphasized) {
                    Modifier.background(colors.primarySoft)
                } else {
                    Modifier.border(1.dp, colors.border, CircleShape)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (emphasized) colors.primary else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
