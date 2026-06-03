package com.example.musicapplication.ui.mainPage.profile

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.musicapplication.R
import com.example.musicapplication.ui.theme.MusicBgBottom
import com.example.musicapplication.ui.theme.MusicBgTop
import com.example.musicapplication.ui.theme.MusicBorder
import com.example.musicapplication.ui.theme.MusicCardSoft
import com.example.musicapplication.ui.theme.MusicDivider
import com.example.musicapplication.ui.theme.MusicIconMuted
import com.example.musicapplication.ui.theme.MusicPrimary
import com.example.musicapplication.ui.theme.MusicPrimarySoft
import com.example.musicapplication.ui.theme.MusicSurface
import com.example.musicapplication.ui.theme.MusicTextPrimary
import com.example.musicapplication.ui.theme.MusicTextSecondary
import com.example.musicapplication.utils.LocalAudioMetaDataReader

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onClickAccount: () -> Unit,
    onClickDownload: () -> Unit,
    onClickFavorite: () -> Unit,
    onClickHistory: () -> Unit,
    onClickSetting: () -> Unit,
    onClickAbout: () -> Unit,
    onClickLogout: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isThemeExpanded by remember { mutableStateOf(false) }
    var selectedThemeKey by remember { mutableStateOf("green") }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        profileViewModel.consumeErrorMessage()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MusicBgTop, MusicBgBottom)
                )
            ),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 150.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ProfileHeader()
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
                    ProfileAction("设置", Icons.Default.Settings, onClick = onClickSetting),
                    ProfileAction("关于音乐", Icons.Default.Info, onClick = onClickAbout),
                    ProfileAction("退出登录", Icons.Default.Logout, onClick = { onClickLogout() })
                ),
                expandedContent = mapOf(
                    0 to {
                        ThemeColorChooser(
                            selectedKey = selectedThemeKey,
                            onSelected = { option ->
                                selectedThemeKey = option.key
                                Toast.makeText(context, "已选择${option.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ),
                expandedIndexes = if (isThemeExpanded) setOf(0) else emptySet()
            )
        }
    }
}



@Composable
private fun ProfileHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "我的",
                color = MusicTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "管理你的音乐与偏好",
                color = MusicTextSecondary,
                fontSize = 14.sp
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MusicIconMuted,
                modifier = Modifier.size(21.dp)
            )
        }
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
            .background(MusicSurface)
            .border(1.dp, MusicBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        AvatarImage(cover = uiState.avatarUrl)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = uiState.nickName,
            color = MusicTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = uiState.email,
            color = MusicTextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("歌单", "12", Modifier.weight(1f))
            StatCard("收藏", "248", Modifier.weight(1f))
            StatCard("播放", "1.6k", Modifier.weight(1f))
        }
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
                color = MusicSurface,
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
            .background(MusicCardSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            color = MusicTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = MusicTextSecondary,
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

private data class ThemeColorOption(
    val key: String,
    val name: String,
    val color: Color
)

private val themeColorOptions = listOf(
    ThemeColorOption("green", "松石绿", Color(0xFF1DB954)),
    ThemeColorOption("blue", "湖蓝", Color(0xFF0EA5E9)),
    ThemeColorOption("rose", "玫瑰", Color(0xFFF43F5E)),
    ThemeColorOption("amber", "琥珀", Color(0xFFF59E0B)),
    ThemeColorOption("violet", "紫罗兰", Color(0xFF8B5CF6)),
    ThemeColorOption("slate", "石墨", Color(0xFF64748B))
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
            .background(MusicSurface)
            .border(1.dp, MusicBorder, RoundedCornerShape(22.dp))
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
                    color = MusicDivider,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun ThemeColorChooser(
    selectedKey: String,
    onSelected: (ThemeColorOption) -> Unit
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
            themeColorOptions.forEach { option ->
                val selected = option.key == selectedKey

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = MusicTextPrimary,
                            shape = CircleShape
                        )
                        .clickable { onSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MusicSurface)
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
                .background(MusicPrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MusicPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = item.title,
            color = MusicTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Icon(
            imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MusicIconMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}
