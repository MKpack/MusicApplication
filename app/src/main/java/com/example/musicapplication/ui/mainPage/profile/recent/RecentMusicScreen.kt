package com.example.musicapplication.ui.mainPage.profile.recent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.musicapplication.ui.component.MusicCollectionScaffold

@Composable
fun RecentMusicScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MusicCollectionScaffold(
        title = "最近播放",
        tag = "最近播放记录会显示在这里",
        emptyTitle = "暂无播放记录",
        emptyDescription = "播放过的歌曲会自动记录到这里。",
        icon = Icons.Default.History,
        onBack = onBack,
        modifier = modifier
    )
}
