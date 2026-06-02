package com.example.musicapplication.data.repository

import android.net.Uri
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {

    val profileFlow: StateFlow<UserProfile?>
    suspend fun getUserProfile(): RepositoryWorkResult<UserProfile>

    suspend fun updateUserProfile(userProfile: UserProfile): RepositoryWorkResult<UserProfile>

    suspend fun updateUserAvatar(uri: Uri): RepositoryWorkResult<UserProfile>
}