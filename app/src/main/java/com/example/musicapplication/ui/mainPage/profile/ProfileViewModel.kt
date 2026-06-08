package com.example.musicapplication.ui.mainPage.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.ProfileRepository
import com.example.musicapplication.data.repository.UserStatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userId: Int? = null,
    val email: String = "music@example.com",
    val nickName: String = "未知用户",
    val avatarUrl: String? = null,
    val playCount: Long = 0L,
    val favoriteCount: Long = 0L,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val profileRepository: ProfileRepository,
    val userStatRepository: UserStatRepository
): ViewModel() {

    private val TAG ="ProfileViewModel"
    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState = _uiState.asStateFlow()


    init {
        observeProfile()
        observeUserStat()
        getProfile()
        getUserStat()
        Log.d(TAG, "ProfileViewModel init")
    }

    fun getProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            val result = profileRepository.getUserProfile()
            when(result) {
                is RepositoryWorkResult.Success -> {
//                    _uiState.value = result.data.toUIState()
//                    有observeProfile，所以不需要再手动更新uiState
                }
                is RepositoryWorkResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                    Log.e(TAG, "getProfile failed: ${result.message}")
                }
            }
        }
    }

    fun observeProfile() {
        viewModelScope.launch {
            profileRepository.profileFlow.collect { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userId = profile.userId,
                        email = profile.email,
                        nickName = profile.nickName ?: "未知用户",
                        avatarUrl = profile.avatarUrl,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeUserStat() {
        viewModelScope.launch {
            userStatRepository.statFlow.collect { stat ->
                if (stat != null) {
                    _uiState.value = _uiState.value.copy(
                        playCount = stat.playCount,
                        favoriteCount = stat.favoriteCount
                    )
                }
            }
        }
    }

    fun getUserStat() {
        viewModelScope.launch {
            when (val result = userStatRepository.getUserStat()) {
                is RepositoryWorkResult.Success -> Unit

                is RepositoryWorkResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message
                    )
                    Log.e(TAG, "getUserStat failed: ${result.message}")
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.isLoading || state.isRefreshing) return@launch

            _uiState.value = state.copy(
                isRefreshing = true,
                errorMessage = null
            )

            val profileResult = profileRepository.getUserProfile()
            val statResult = userStatRepository.getUserStat()
            val failure = listOf(profileResult, statResult)
                .filterIsInstance<RepositoryWorkResult.Failure>()
                .firstOrNull()

            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                isLoading = false,
                errorMessage = failure?.message
            )

            failure?.let {
                Log.e(TAG, "refreshProfile failed: ${it.message}")
            }
        }
    }

    fun consumeErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}
