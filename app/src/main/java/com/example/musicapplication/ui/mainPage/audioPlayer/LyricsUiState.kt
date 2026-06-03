package com.example.musicapplication.ui.mainPage.audioPlayer

data class LyricLine(
    val timeSeconds: Float,
    val text: String
)

data class LyricsUiState(
    val isLoading: Boolean = false,
    val lines: List<LyricLine> = emptyList(),
    val errorMessage: String? = null
)