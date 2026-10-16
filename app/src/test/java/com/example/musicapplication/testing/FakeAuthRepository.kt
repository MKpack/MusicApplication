package com.example.musicapplication.testing

import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.AuthRepository
import kotlinx.coroutines.delay

/** 不走网络的 AuthRepository：返回结果可以随时改，记录每次调用的参数 */
class FakeAuthRepository : AuthRepository {
    var loginResult: RepositoryWorkResult<Unit> = RepositoryWorkResult.Success(Unit)
    var sendCodeResult: RepositoryWorkResult<Unit> = RepositoryWorkResult.Success(Unit)
    var registerResult: RepositoryWorkResult<Unit> = RepositoryWorkResult.Success(Unit)
    var resetPasswordResult: RepositoryWorkResult<Unit> = RepositoryWorkResult.Success(Unit)

    // 模拟网络耗时，用来测试请求进行中的连点
    var latencyMillis = 0L

    val calls = mutableListOf<String>()

    override suspend fun login(email: String, password: String): RepositoryWorkResult<Unit> {
        calls += "login($email, $password)"
        delay(latencyMillis)
        return loginResult
    }

    override suspend fun sendCode(email: String): RepositoryWorkResult<Unit> {
        calls += "sendCode($email)"
        delay(latencyMillis)
        return sendCodeResult
    }

    override suspend fun register(email: String, authCode: String, password: String): RepositoryWorkResult<Unit> {
        calls += "register($email, $authCode, $password)"
        delay(latencyMillis)
        return registerResult
    }

    override suspend fun resetPassword(email: String, authCode: String, newPassword: String): RepositoryWorkResult<Unit> {
        calls += "resetPassword($email, $authCode, $newPassword)"
        delay(latencyMillis)
        return resetPasswordResult
    }
}
