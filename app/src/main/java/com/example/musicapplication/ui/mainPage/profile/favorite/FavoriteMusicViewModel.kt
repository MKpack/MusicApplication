package com.example.musicapplication.ui.mainPage.profile.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


data class SongListUiState(
    val songs: List<Song> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isEndReached: Boolean = false
)

@HiltViewModel
class FavoriteMusicViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _songListUiState = MutableStateFlow(SongListUiState())
    val songListUiState = _songListUiState.asStateFlow()

    init {
        observeLovedSongs()
        refreshLovedSongs()
    }

    fun observeLovedSongs() {
        viewModelScope.launch {
            songRepository.observeSongs(SongListKey.Loved).collect { songs ->
                _songListUiState.value = _songListUiState.value.copy(
                    songs = songs
                )
            }
        }
    }

    fun refreshLovedSongs(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            val state = _songListUiState.value
            if (state.isLoading) return@launch
            if (state.isRefreshing) return@launch
            _songListUiState.value = _songListUiState.value.copy(
                isLoading = true,
                isRefreshing = isPullToRefresh,
                errorMsg = null,
                isEndReached = false
            )

            val result = songRepository.refreshSongs(SongListKey.Loved)
            when(result) {
                is RepositoryWorkResult.Success -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                is RepositoryWorkResult.Failure -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMsg = result.message
                    )
                }
            }
        }
    }

    fun loadMoreLoveSongs() {
        viewModelScope.launch {
            val state = _songListUiState.value
            if (state.isLoading) return@launch
            if (state.isRefreshing) return@launch
            if (state.isEndReached) return@launch
            _songListUiState.value = _songListUiState.value.copy(
                isLoading = true,
                errorMsg = null
            )
            val result = songRepository.loadMoreSongs(SongListKey.Loved)

            when(result) {
                is RepositoryWorkResult.Success -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false,
                        isEndReached = result.data.isEndReached
                    )
                }
                is RepositoryWorkResult.Failure -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false,
                        errorMsg = result.message
                    )
                }
            }
        }
    }

    fun doFavoriteEvent(songIndex: Int) {
        val song = _songListUiState.value.songs.getOrNull(songIndex) ?: return

        viewModelScope.launch {
            val result = if (song.isLoved) {
                songRepository.unFavoriteSong(song.songId)
            } else {
                songRepository.favoriteSong(song.songId)
            }

            if (result is RepositoryWorkResult.Failure) {
                _songListUiState.value = _songListUiState.value.copy(
                    errorMsg = result.message
                )
            }
        }
    }


    fun consumeErrorMessage() {
        _songListUiState.value = _songListUiState.value.copy(
            errorMsg = null
        )
    }

//    private fun loadLovedSongs(
//        pageNum: Long,
//        isRefresh: Boolean
//    ) {
//        viewModelScope.launch {
//            val state = _songListUiState.value
//            if (state.isLoading) return@launch
//            _songListUiState.value = _songListUiState.value.copy(
//                isLoading = true,
//                isRefreshing = isRefresh
//            )
//            val response = songRepository.getLovedSongs(pageNum, 20)
//
//            when(response) {
//                is RepositoryWorkResult.Success -> {
//                    val newState = response.data.toSongListUiState()
//                    _songListUiState.value = if (isRefresh) {
//                        newState
//                    } else {
//                        newState.copy(
//                            songs = state.songs + newState.songs
//                        )
//                    }
//                }
//                is RepositoryWorkResult.Failure -> {
//                    _songListUiState.value = _songListUiState.value.copy(
//                        errorMsg = response.message,
//                        isLoading = false,
//                        isRefreshing = false
//                    )
//                }
//            }
//        }
//    }
}
