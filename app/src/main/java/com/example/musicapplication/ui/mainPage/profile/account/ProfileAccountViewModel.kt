package com.example.musicapplication.ui.mainPage.profile.account

import android.net.Uri
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

data class ProfileAccountUiState(
    val userId: Int? = null,
    val email: String = "",
    val nickName: String = "",
    val avatarUrl: String? = null,
    // 本地刚选择、但还没上传的头像
    val pendingAvatarUri: Uri? = null,

    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileAccountViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val TAG = "ProfileAccountViewModel"

    private val _uiState = MutableStateFlow(ProfileAccountUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeProfile()
        getProfile()
    }

    private fun getProfile() {
        viewModelScope.launch {
            when (val result = profileRepository.getUserProfile()) {
                is RepositoryWorkResult.Success -> Unit
                is RepositoryWorkResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.profileFlow.collect { profile ->
                if (profile != null) {
                    _uiState.value = profile.toAccountUiState()
                }
            }
        }
    }
    fun onAvatarPicked(uri: Uri?) {
        if (uri != null) {
            _uiState.value = _uiState.value.copy(
                pendingAvatarUri = uri,
                errorMessage = null
            )
        }
    }

    fun onNickNameChange(nickName: String) {
        _uiState.value = _uiState.value.copy(
            nickName = nickName
        )
        Log.d(TAG, "新nickName: $nickName")
    }

    fun onCropAvatarFailed(message: String?) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message ?: "头像裁剪失败"
        )
    }

    fun consumeErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }


    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            val userId = state.userId
            if (userId == null) {
                _uiState.value = state.copy(
                    errorMessage = "用户信息未加载完成"
                )
                return@launch
            }

            _uiState.value = state.copy(
                isSaving = true,
                errorMessage = null
            )

            val avatarResult = state.pendingAvatarUri?.let { uri ->
                profileRepository.updateUserAvatar(uri)
            }

            if (avatarResult is RepositoryWorkResult.Failure) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = avatarResult.message
                )
                return@launch
            }

            val latest = _uiState.value

            val profileResult = profileRepository.updateUserProfile(
                UserProfile(
                    userId = userId,
                    email = latest.email,
                    nickName = state.nickName,
                    avatarUrl = latest.avatarUrl,
                )
            )

            when (profileResult) {
                is RepositoryWorkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        pendingAvatarUri = null
                    )
                }
                is RepositoryWorkResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = profileResult.message
                    )
                }
            }

        }
    }

    private fun UserProfile.toAccountUiState(): ProfileAccountUiState {
        return ProfileAccountUiState(
            userId = userId,
            email = email,
            nickName = nickName ?: "",
            avatarUrl = avatarUrl,
            pendingAvatarUri = null,
            isSaving = false,
            errorMessage = null
        )
    }
}
