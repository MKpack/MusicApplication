package com.example.musicapplication.ui.mainPage.home

import android.util.Log
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
    val total: Long = 0,
    val size: Long = 0,
    val current: Long = 0,
    val pages: Long = 0,
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val songRepository: SongRepository
): ViewModel() {


    private val TAG = "HomeScreenViewModel"

    private val _songListUiState = MutableStateFlow(SongListUiState())
    val songListUiState = _songListUiState.asStateFlow()

    init {
        observeHotSongs()
        refreshHotSongs()
    }

    private fun observeHotSongs() {
        viewModelScope.launch {
            songRepository.observeSongs(SongListKey.Hot).collect { songs ->
                _songListUiState.value = _songListUiState.value.copy(
                    songs = songs
                )
            }
        }
    }

    fun refreshHotSongs(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            val state = _songListUiState.value
            if (state.isLoading) return@launch
            if (state.isRefreshing) return@launch

            _songListUiState.value = state.copy(
                isLoading = !isPullToRefresh,
                isRefreshing = isPullToRefresh,
                errorMsg = null
            )

            val result = songRepository.refreshSongs(SongListKey.Hot)

            when(result) {
                is RepositoryWorkResult.Success -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false,
                        isRefreshing = false
                    )
                    Log.d(TAG, "getHotSongs: ${result.data}")
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

    fun loadMoreHotSongs() {
        viewModelScope.launch {
            val state = _songListUiState.value
            if (state.isLoading) return@launch
            _songListUiState.value = state.copy(
                isLoading = true,
                errorMsg = null
            )
            val result = songRepository.loadMoreSongs(SongListKey.Hot)
            when(result) {
                is RepositoryWorkResult.Success -> {
                    _songListUiState.value = _songListUiState.value.copy(
                        isLoading = false
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


    fun consumeErrorMsg() {
        _songListUiState.value = _songListUiState.value.copy(
            errorMsg = null
        )
    }

//    fun getHotSongs() {
//        viewModelScope.launch {
//            val state = _songListUiState.value
//            if (state.isLoading) return@launch
//            _songListUiState.value = state.copy(isLoading = true)
//            val nextPage = state.current + 1
//            val result = songRepository.getHotSongs(nextPage, 20)
//
//            when(result) {
//                is RepositoryWorkResult.Success -> {
//                    _songListUiState.value = _songListUiState.value.copy(
//                        isLoading = false,
//                        songs = state.songs + result.data.records,
//                        total = result.data.total,
//                        size = result.data.size,
//                        current = result.data.current,
//                        pages = result.data.pages,
//                        errorMsg = null
//                    )
//                    Log.d(TAG, "getHotSongs: ${result.data}")
//                }
//                is RepositoryWorkResult.Failure -> {
//                    _songListUiState.value = _songListUiState.value.copy(
//                        isLoading = false,
//                        errorMsg = result.message
//                    )
//                }
//            }
//        }
//    }
}
