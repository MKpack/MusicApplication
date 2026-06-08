package com.example.musicapplication.data.repository

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.local.userstat.UserStatStore
import com.example.musicapplication.data.remote.api.UserStatApi
import com.example.musicapplication.data.remote.mapper.toUserStat
import com.example.musicapplication.domain.model.UserStat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class UserStatRepositoryImpl @Inject constructor(
    private val userStatApi: UserStatApi,
    private val userStatStore: UserStatStore
) : UserStatRepository {

    override val statFlow: Flow<UserStat?> = userStatStore.userStatFlow

    override suspend fun getUserStat(): RepositoryWorkResult<Unit> {
        try {
            val result = userStatApi.getUserStat()
            if (result.code == 200 && result.data != null) {
                userStatStore.saveUserStat(result.data.toUserStat())
                return RepositoryWorkResult.Success(Unit)
            }
            return RepositoryWorkResult.Failure("获取UserStat失败")
        } catch (e : Exception) {
            return RepositoryWorkResult.Failure("连接服务器错误", throwable = e)
        }
    }

    override suspend fun increaseUserStatPlayCount(): RepositoryWorkResult<Unit> {
        try {
            val result = userStatApi.increasePlayStat()
            if (result.code == 200 && result.data != null) {
                userStatStore.saveUserStat(result.data.toUserStat())
                return RepositoryWorkResult.Success(Unit)
            }
            return RepositoryWorkResult.Failure("增加UserStat计数错误")
        } catch (e : Exception) {
            return RepositoryWorkResult.Failure("连接服务器错误", throwable = e)
        }
    }


}