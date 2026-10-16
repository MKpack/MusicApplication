package com.example.musicapplication.ui.auth.validation

/** 登录注册的校验规则。纯 Kotlin，不依赖 Android（android.util.Patterns 在单元测试里是空的） */
object AuthValidators {
    // 后端发的验证码是 5 位数字，10 分钟内有效
    const val CODE_LENGTH = 5

    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun isEmailValid(email: String): Boolean = EMAIL.matches(email.trim())
}
