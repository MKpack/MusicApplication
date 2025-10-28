package com.example.musicapplication.ui.mainPage.audioPlayer

import android.graphics.Bitmap
import android.util.Log
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

    //song Title
    private val _songTitle = MutableStateFlow("方圆几里")
    val songTitle: StateFlow<String> = _songTitle

    //song singer
    private val _songSinger = MutableStateFlow("薛之谦")
    val songSinger: StateFlow<String> = _songSinger

    //indictor progress 进度
    private val _currentProgress = MutableStateFlow(0.5f)
    val currentProgress: StateFlow<Float> = _currentProgress

    //left_time, right_time
    private val _leftTime = MutableStateFlow("0:00")
    val leftTime: StateFlow<String> = _leftTime
    private val _rightTime = MutableStateFlow("-4:22")
    val rightTime: StateFlow<String> = _rightTime

    fun updateColorsFromBitmap(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val color1 = palette?.dominantSwatch?.rgb ?: 0xFF000000.toInt()
            val color2 = palette?.vibrantSwatch?.rgb ?: color1

            _dominantColors.value = listOf(Color(color1), Color(color2))
        }
    }

    fun updateProgress(tmp: Float) {
        _currentProgress.value = tmp
        Log.d(TAG, "progress: " + tmp)
    }

}