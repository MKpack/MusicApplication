package com.example.musicapplication.data.local.userstat

import com.example.musicapplication.domain.model.UserStat
import kotlinx.coroutines.flow.Flow

interface UserStatStore {

    val userStatFlow: Flow<UserStat?>

    suspend fun saveUserStat(userStat: UserStat)

    suspend fun clearUserStat()
}
