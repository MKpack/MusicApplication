package com.example.musicapplication.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 双色Icons，双层Icon进行只变内部的颜色，不改变轮廓颜色
 */
@Composable
fun TwoColorIcon(
    fill: ImageVector,
    outlined: ImageVector,
    fillColor: Color
) {
    Box(
        modifier = Modifier.size(24.dp)
    ){
        Icon(fill, contentDescription = null, tint = fillColor)
        Icon(outlined, contentDescription = null, tint = Color.Black)
    }
}