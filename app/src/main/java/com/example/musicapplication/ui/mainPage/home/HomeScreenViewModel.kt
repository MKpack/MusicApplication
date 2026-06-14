package com.example.musicapplication.ui.mainPage.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.AppNotificationRepository
import com.example.musicapplication.data.repository.SongRepository
import com.example.musicapplication.domain.model.AppNotification
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

data class HomeNotificationUiState(
    val notification: AppNotification? = null,
    val showNotificationDialog: Boolean = false,
    val notificationMsg: String? = null
)


@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val appNotificationRepository: AppNotificationRepository
): ViewModel() {


    private val TAG = "HomeScreenViewModel"

    private val _songListUiState = MutableStateFlow(SongListUiState())
    val songListUiState = _songListUiState.asStateFlow()

    private val _notificationUiState = MutableStateFlow(HomeNotificationUiState())
    val notificationUiState = _notificationUiState.asStateFlow()

    init {
        observeHotSongs()
        refreshHotSongs()
        checkLatestNotificationOnAppOpen()
    }

    private fun checkLatestNotificationOnAppOpen() {
        viewModelScope.launch {
            when (val result = appNotificationRepository.getLatestNotification()) {
                is RepositoryWorkResult.Success -> {
                    val notification = result.data ?: return@launch
                    val lastShownId = appNotificationRepository.getLastAutoShownNotificationId()
                    if (notification.notificationId != lastShownId) {
                        _notificationUiState.value = _notificationUiState.value.copy(
                            notification = notification,
                            showNotificationDialog = true
                        )
                        appNotificationRepository.saveLastAutoShownNotificationId(notification.notificationId)
                    }
                }

                is RepositoryWorkResult.Failure -> {
                    // 自动检查通知失败时保持静默，避免每次打开 App 都打扰用户。
                }
            }
        }
    }

    fun loadLatestNotificationByClick() {
        viewModelScope.launch {
            when (val result = appNotificationRepository.getLatestNotification()) {
                is RepositoryWorkResult.Success -> {
                    val notification = result.data
                    if (notification == null) {
                        _notificationUiState.value = _notificationUiState.value.copy(
                            notificationMsg = "暂无通知"
                        )
                    } else {
                        _notificationUiState.value = _notificationUiState.value.copy(
                            notification = notification,
                            showNotificationDialog = true
                        )
                    }
                }

                is RepositoryWorkResult.Failure -> {
                    _notificationUiState.value = _notificationUiState.value.copy(
                        notificationMsg = result.message
                    )
                }
            }
        }
    }

    fun dismissNotificationDialog() {
        _notificationUiState.value = _notificationUiState.value.copy(
            showNotificationDialog = false
        )
    }

    fun consumeNotificationMsg() {
        _notificationUiState.value = _notificationUiState.value.copy(
            notificationMsg = null
        )
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
                errorMsg = null,
                isEndReached = false
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
        Log.d(TAG, "loadMoreHotSongs begin")
        viewModelScope.launch {
            val state = _songListUiState.value
            if (state.isLoading) return@launch
            if (state.isRefreshing) return@launch
            if (state.isEndReached) return@launch
            _songListUiState.value = state.copy(
                isLoading = true,
                errorMsg = null
            )
            val result = songRepository.loadMoreSongs(SongListKey.Hot)
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
                        isEndReached = false,
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
