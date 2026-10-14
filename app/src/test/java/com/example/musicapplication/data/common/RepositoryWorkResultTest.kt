package com.example.musicapplication.data.common

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RepositoryWorkResultTest {

    private fun httpException(code: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, responseBody))
    }

    @Test
    fun businessError_usesBackendMessage() {
        // 后端 GlobalExceptionHandler 对 BusinessException 的返回格式
        val failure = httpException(400, """{"code":1002,"message":"验证码无效","data":null}""")
            .toRepositoryFailure("网络请求失败")

        assertEquals("验证码无效", failure.message)
        assertEquals(1002, failure.code)
    }

    @Test
    fun nonJsonErrorBody_usesDefaultMessage() {
        val failure = httpException(502, "<html>Bad Gateway</html>")
            .toRepositoryFailure("网络请求失败")

        assertEquals("网络请求失败", failure.message)
    }

    @Test
    fun networkError_usesDefaultMessage() {
        val failure = IOException("timeout").toRepositoryFailure("网络请求失败")

        assertEquals("网络请求失败", failure.message)
    }
}
