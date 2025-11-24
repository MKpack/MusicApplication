package com.example.musicapplication.ui.mainPage.home

import com.example.musicapplication.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.data.remote.model.Song
import com.example.musicapplication.ui.component.SongSelectItem

@Composable
fun HomeScreen(modifier: Modifier) {
    //屏幕宽度
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val screenWidth = LocalConfiguration.current.screenWidthDp
    //静态歌曲
    val songs: List<Song> = listOf(
        Song(0, "想你的夜", "关喆", true, "uri"),
        Song(0, "想你的夜", "关喆", true, "uri"),
        Song(0, "想你的夜", "关喆", true, "uri"),
        Song(0, "想你的夜", "关喆", true, "uri"),
        Song(0, "想你的夜", "关喆", true, "uri"),
    )
    val group = songs.chunked(2)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //搜索
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0x3C727070), RoundedCornerShape(16.dp))
                .padding(10.dp)
                .height(30.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.search_solid_full),
                contentDescription = null,
            )
            Text(
                "艺人、歌曲(暂不支持歌词)",
                fontSize = 20.sp
            )
        }
        LazyColumn {
            item {
                //随心听
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height((screenHeight * 0.17).dp)
                        .padding(vertical = 20.dp)
                        .border(1.dp, Color(0x95E8A0A0), RoundedCornerShape(16.dp))
                        .background(color = Color(0x95E8A0A0), shape = RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = "",
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Row(
                        modifier = Modifier.weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            "未知歌曲",
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(start = 20.dp, end = 30.dp),
                            color = Color(0xD78D2C66),
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {}) {
                            Icon(
                                painter = painterResource(R.drawable.play_icon),
                                contentDescription = null,
                            )
                        }
                    }
                    VerticalDivider(thickness = 3.dp, color = Color(0xD78D2C66))
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        "随机听".forEach { it ->
                            Text(
                                it.toString(),
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp),
                                color = Color(0xD78D2C66),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            item {
                //最热
                Column(
                    modifier = Modifier.fillMaxWidth()
//                .padding(top = 20.dp)
                        .height((screenHeight * 0.25).dp)
                        .background(color = Color(0xDAF84DAA), RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "最热",
                        fontSize = 25.sp,
                        modifier = Modifier
                            .padding(10.dp),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = Color.Black, thickness = 2.dp)

                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(group) { it ->
                            Box(
                                modifier = Modifier.wrapContentHeight()
                                    .width(screenWidth.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    it.forEach { item ->
                                        SongSelectItem(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                //最新
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 30.dp)
                        .height((screenHeight * 0.25).dp)
                        .background(color = Color(0xD584BDF1), RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "最新",
                        fontSize = 25.sp,
                        modifier = Modifier
                            .padding(10.dp),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = Color.Black, thickness = 2.dp)

                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(group) { it ->
                            Box(
                                modifier = Modifier.wrapContentHeight()
                                    .width(screenWidth.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    it.forEach { item ->
                                        SongSelectItem(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
