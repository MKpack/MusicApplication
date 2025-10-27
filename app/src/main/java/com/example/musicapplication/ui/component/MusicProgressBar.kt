package com.example.musicapplication.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.abs

@Composable
fun MusicProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
) {
    val TAG = "MusicProgressBar"
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val thumbRadius = 20.dp
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadius.toPx() }

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
                  onSeek(newProgress)
                  dragProgress = newProgress
              }
          }
          //拖动跳转位置
          .pointerInput(Unit) {
              detectHorizontalDragGestures(
                  onDragStart = { offset->
                      isPressed = true
                      val thumbCenterX = dragProgress * size.width
                      val touchedAllow = abs(offset.x - thumbCenterX) <= thumbRadiusPx
                      if (!touchedAllow) return@detectHorizontalDragGestures
                      isDragging = true
                  },
                  onHorizontalDrag = { change, _ ->
                      if (!isDragging)  return@detectHorizontalDragGestures
                      val deltaX = change.positionChange().x
                      Log.d(TAG, "dragProgress: " + dragProgress)
                      dragProgress = (dragProgress + deltaX / size.width).coerceIn(0f, 1f)
                  },
                  onDragEnd = {
                      isPressed = false
                      if (!isDragging)  return@detectHorizontalDragGestures
                      isDragging = false
                      onSeek(dragProgress)
                  },
                  onDragCancel = {
                      isPressed = false
                      isDragging = false
                  }
              )
          }

    ){
        Box(
            Modifier.fillMaxHeight()
                .fillMaxWidth(if (isDragging) dragProgress else progress)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isPressed) Color.White else Color(0xFFDADADA))
        )
    }
}