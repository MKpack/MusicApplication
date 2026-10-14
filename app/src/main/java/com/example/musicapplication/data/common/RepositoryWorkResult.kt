package com.example.musicapplication.data.common

import com.example.musicapplication.data.remote.dto.response.ApiResponse
import com.google.gson.Gson
import retrofit2.HttpException

sealed interface RepositoryWorkResult<out T> {
    data class Success<T> (
        val data: T
    ): RepositoryWorkResult<T>

    data class Failure(
        val message: String,
        val code: Int? = null,
        val throwable: Throwable? = null
    ): RepositoryWorkResult<Nothing>
}

/**
 * 把接口调用抛出的异常转成 Failure
 * 后端的业务错误（验证码无效、密码错误等）返回的是 HTTP 400，响应体仍然是 ApiResponse：
 * {"code":1002,"message":"验证码无效","data":null}
 * Retrofit 遇到非 2xx 会直接抛 HttpException，这里优先取出后端给的 message，取不到再用 defaultMessage
 */
fun Throwable.toRepositoryFailure(defaultMessage: String): RepositoryWorkResult.Failure {
    val apiResponse = (this as? HttpException)
        ?.response()
        ?.errorBody()
        ?.use { body ->
            runCatching { Gson().fromJson(body.charStream(), ApiResponse::class.java) }.getOrNull()
        }
    val message = apiResponse?.message

    return if (message.isNullOrBlank()) {
        RepositoryWorkResult.Failure(defaultMessage, throwable = this)
    } else {
        RepositoryWorkResult.Failure(message, apiResponse?.code, this)
    }
}
