package com.example.musicapplication.data.local.profile

import com.example.musicapplication.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileStore {

    val profileFlow: Flow<UserProfile?>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun clearProfile()
}