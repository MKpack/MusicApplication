package com.example.musicapplication.ui.mainPage.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.ProfileRepository
import com.example.musicapplication.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUIState(
    val isLoading: Boolean = false,
    val userId: Int? = null,
    val email: String = "music@example.com",
    val nickName: String = "未知用户",
    val avatarUrl: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val profileRepository: ProfileRepository
): ViewModel() {

    private val TAG ="ProfileViewModel"
    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState = _uiState.asStateFlow()

    init {
        observeProfile()
        getProfile()
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
                    _uiState.value = profile.toUIState()
                }
            }
        }
    }

    private fun UserProfile.toUIState(): ProfileUIState {
        return ProfileUIState(
            isLoading = false,
            userId = userId,
            email = email,
            nickName = nickName ?: "未知用户",
            avatarUrl = avatarUrl,
            errorMessage = null
        )
    }

    fun consumeErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}
