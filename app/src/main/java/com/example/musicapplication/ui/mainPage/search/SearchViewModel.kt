package com.example.musicapplication.ui.mainPage.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.search.SearchHistoryStore
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.Song
import com.example.musicapplication.domain.model.SongListKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val keyword: String = "",
    val songs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val hasSearched: Boolean = false,
    val isEndReached: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val searchHistoryStore: SearchHistoryStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var observeJob: Job? = null
    private var observeRecentSongsJob: Job? = null

    init {
        observeSearchHistory()
    }

    fun onKeywordChange(keyword: String) {
        _uiState.value = _uiState.value.copy(
            keyword = keyword,
            errorMsg = null
        )

        searchJob?.cancel()
        val cleanKeyword = keyword.trim()
        if (cleanKeyword.isBlank()) {
            observeJob?.cancel()
            _uiState.value = _uiState.value.copy(
                keyword = keyword,
                songs = emptyList(),
                isLoading = false,
                errorMsg = null,
                hasSearched = false,
                isEndReached = false
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            search(cleanKeyword)
        }
    }

    fun searchImmediately(keyword: String = _uiState.value.keyword) {
        searchJob?.cancel()
        val cleanKeyword = keyword.trim()
        if (cleanKeyword.isBlank()) return
        viewModelScope.launch {
            search(cleanKeyword)
        }
    }

    fun doFavoriteEvent(songIndex: Int) {
        val song = _uiState.value.songs.getOrNull(songIndex) ?: return

        doFavoriteEvent(song)
    }

    fun doFavoriteEvent(song: Song) {
        viewModelScope.launch {
            val result = if (song.isLoved) {
                songRepository.unFavoriteSong(song.songId)
            } else {
                songRepository.favoriteSong(song.songId)
            }

            if (result is RepositoryWorkResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    errorMsg = result.message
                )
            }
        }
    }

    fun recordSearchSong(song: Song) {
        viewModelScope.launch {
            searchHistoryStore.saveSong(song.songId)
        }
    }

    fun consumeErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.clearSearchHistory()
        }
    }

    fun loadMoreSearchSongs() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isBlank()) return

        viewModelScope.launch {
            val state = _uiState.value
            if (state.isLoading) return@launch
            if (state.isEndReached) return@launch

            _uiState.value = state.copy(
                isLoading = true,
                errorMsg = null
            )

            when (val result = songRepository.loadMoreSongs(SongListKey.Search(keyword))) {
                is RepositoryWorkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEndReached = result.data.isEndReached
                    )
                }

                is RepositoryWorkResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = result.message
                    )
                }
            }
        }
    }

    private suspend fun search(keyword: String) {
        val listKey = SongListKey.Search(keyword)
        observeSearchResult(listKey)

        _uiState.value = _uiState.value.copy(
            keyword = keyword,
            isLoading = true,
            errorMsg = null,
            hasSearched = true,
            isEndReached = false
        )

        when (val result = songRepository.refreshSongs(listKey)) {
            is RepositoryWorkResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }

            is RepositoryWorkResult.Failure -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = result.message
                )
            }
        }
    }

    private fun observeSearchHistory() {
        viewModelScope.launch {
            searchHistoryStore.searchHistoryFlow.collect { songIds ->
                observeRecentSongsJob?.cancel()
                observeRecentSongsJob = viewModelScope.launch {
                    songRepository.observeSongsByIds(songIds).collect { songs ->
                        _uiState.value = _uiState.value.copy(
                            recentSongs = songs
                        )
                    }
                }
            }
        }
    }

    private fun observeSearchResult(listKey: SongListKey.Search) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            songRepository.observeSongs(listKey).collect { songs ->
                _uiState.value = _uiState.value.copy(
                    songs = songs
                )
            }
        }
    }
}
