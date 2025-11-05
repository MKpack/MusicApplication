package com.example.musicapplication.ui.mainPage.audioPlayer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.utils.MusicProgressUtils
import kotlin.math.abs

@Composable
fun MusicProgressBar2(
    playerViewModel: PlayerViewModel
) {
    val TAG = "MusicProgressBar2"
    val thumbRadius = 25.dp
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadius.toPx() }

    val progress by playerViewModel.currentProgress.collectAsState()
    var isPressed by remember { mutableStateOf(false) }
//    var isDragging by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDowningMinutes by interactionSource.collectIsPressedAsState()

    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val isDragging by playerViewModel.isDragging.collectAsState()

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFBBBABA))
                //点击跳转位置
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        playerViewModel.updateProgress(newProgress)
                        playerViewModel.seekTo()
                    }
                }
                //拖动跳转位置
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset->
                            isPressed = true
                            val thumbCenterX = progress * size.width
                            val touchedAllow = abs(offset.x - thumbCenterX) <= thumbRadiusPx
                            if (!touchedAllow) return@detectHorizontalDragGestures
//                            isDragging = true
                            playerViewModel.changeDraggingStatus(true)
                        },
                        onHorizontalDrag = { change, _ ->
                            if (!isDragging)  return@detectHorizontalDragGestures
                            val deltaX = change.positionChange().x
                            val newProgress = (progress + deltaX / size.width).coerceIn(0f, 1f)
                            playerViewModel.updateProgress(newProgress)
                        },
                        onDragEnd = {
                            isPressed = false
                            if (!isDragging)  return@detectHorizontalDragGestures
                            playerViewModel.seekTo()
//                            isDragging = false
                            playerViewModel.changeDraggingStatus(false)
                        },
                        onDragCancel = {
                            isPressed = false
//                            isDragging = false
                            playerViewModel.changeDraggingStatus(false)
                        }
                    )
                }

        ){
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isPressed) Color.White else Color(0xFFDADADA))
            )
        }
        Spacer(
            modifier = Modifier.fillMaxWidth()
                .height(5.dp)
                .background(Color.Transparent)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource,
                    indication = null,
                    onClick = {}
                ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = MusicProgressUtils.convertSecondsToMinutes(currentPosition),
                fontSize = 12.sp,
                color = if (isPressed || isDowningMinutes) Color.White else Color(0xFFBBBABA)
            )
            Text(
                text = "-" + MusicProgressUtils.convertSecondsToMinutes(duration - currentPosition),
                fontSize = 11.sp,
                color = if (isPressed || isDowningMinutes) Color.White else Color(0xFFBBBABA)
            )
        }
    }
}