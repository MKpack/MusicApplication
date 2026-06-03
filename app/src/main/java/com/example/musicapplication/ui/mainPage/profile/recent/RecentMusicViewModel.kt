package com.example.musicapplication.ui.mainPage.profile.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentSongUiState(
    val songs: List<Song> = emptyList()
)

@HiltViewModel
class RecentMusicViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _songUiState = MutableStateFlow(RecentSongUiState())
    val songUiState = _songUiState.asStateFlow()

    init {
        observeRecentSongs()
    }

    private fun observeRecentSongs() {
        viewModelScope.launch {
            songRepository.observeRecentSongs().collect { songs ->
                _songUiState.value = _songUiState.value.copy(
                    songs = songs
                )
            }
        }
    }

    fun clearRecentPlay() {
        viewModelScope.launch {
            songRepository.clearRecentPlay()
        }
    }
}
