package com.example.musicapplication.ui.mainPage.profile.download

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.musicapplication.ui.component.MusicCollectionScaffold

@Composable
fun DownloadMusicScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MusicCollectionScaffold(
        title = "下载管理",
        tag = "管理已下载的离线音乐",
        emptyTitle = "暂无下载音乐",
        emptyDescription = "下载后的歌曲会保存在这里，断网时也可以播放。",
        icon = Icons.Default.Download,
        onBack = onBack,
        modifier = modifier
    )
}
