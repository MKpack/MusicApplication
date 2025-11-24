package com.example.musicapplication.ui.component

import com.example.musicapplication.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.data.remote.model.Song
import org.w3c.dom.Text

@Composable
fun SongSelectItem(
    song: Song
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(10.dp)
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.default_cover),
            contentDescription = null,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier.fillMaxHeight().padding(start = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.songTitle,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = song.singer,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {},
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.heart_regular_full),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Preview
@Composable
fun showSongSelectItem() {
    Column {
        SongSelectItem(Song(0, "想你的夜", "关喆", true, "uri"))
    }
}