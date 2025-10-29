package com.example.musicapplication.ui.mainPage.audioPlayer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.palette.graphics.Palette
import com.example.musicapplication.R
import com.example.musicapplication.ui.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    private val TAG = "PlayerViewModel"

    private val song = Song("想你的夜", "关喆", 266, true, R.drawable.xiangnideye)
    private val _dominantColors = MutableStateFlow(listOf(Color.Black, Color.DarkGray))
    val dominantColors: StateFlow<List<Color>> = _dominantColors

    //song Title
    private val _songTitle = MutableStateFlow(song.songTitle)
    val songTitle: StateFlow<String> = _songTitle

    //song singer
    private val _songSinger = MutableStateFlow(song.singer)
    val songSinger: StateFlow<String> = _songSinger

    //song bitmap
    private val _songBitmap = MutableStateFlow(song.bitmap)
    val songBitmap: StateFlow<Int> = _songBitmap

    //song isLove
    private val _isLoved = MutableStateFlow(song.isLoved)
    val songIsLove = _isLoved

    //indictor progress 进度
    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress: StateFlow<Float> = _currentProgress

    //left_time, all_time
    private val _leftTime = MutableStateFlow(0)
    val leftTime: StateFlow<Int> = _leftTime
    private val _allTime = MutableStateFlow(song.songTimeLong)
    val allTime: StateFlow<Int> = _allTime


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
        _leftTime.value = (tmp * _allTime.value).toInt()
    }

    fun updateLoveStatus() {
        _isLoved.value = !_isLoved.value
    }
}