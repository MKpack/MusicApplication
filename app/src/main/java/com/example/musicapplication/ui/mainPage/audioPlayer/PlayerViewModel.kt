package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(

): ViewModel() {
    private val TAG = "PlayerViewModel"

    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }
}