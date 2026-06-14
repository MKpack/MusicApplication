package com.example.musicapplication.ui.mainPage.profile.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadMusicUiState(
    val songs: List<Song> = emptyList()
)

@HiltViewModel
class DownloadMusicViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    val uiState = songRepository.observeDownloadSongs()
        .map { DownloadMusicUiState(songs = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadMusicUiState()
        )


    fun deleteDownloadSongs(songIds : List<Long>) {
        viewModelScope.launch {
            songRepository.deleteDownloadSongs(songIds)
        }
    }
}