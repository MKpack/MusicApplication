package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.domain.model.UserStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserStatRepository {

    val statFlow: Flow<UserStat?>

    suspend fun getUserStat() : RepositoryWorkResult<Unit>

    suspend fun increaseUserStatPlayCount() : RepositoryWorkResult<Unit>
}