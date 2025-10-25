package com.example.musicapplication.ui.mainPage.audioPlayer

import com.example.musicapplication.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.musicapplication.ui.component.AppleMusicBackground

fun getStaticBitmap(context: Context, resId: Int): Bitmap {
    val drawable = context.getDrawable(resId) as BitmapDrawable
    return drawable.bitmap
}
@Composable
fun FullScreenPlayer(
    navController: NavController,
    context: Context,
    playerViewModel: PlayerViewModel,
) {
    val colors by playerViewModel.dominantColors.collectAsState()
    LaunchedEffect(Unit) {
        val bitmap = getStaticBitmap(context = context, R.drawable.static_cover);
        playerViewModel.updateColorsFromBitmap(bitmap)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AppleMusicBackground(
            cover = R.drawable.static_cover,
            dominantColor = colors.firstOrNull() ?: Color.DarkGray,
            secondaryColor = colors.getOrNull(1) ?: Color.Black
        )
    }
}