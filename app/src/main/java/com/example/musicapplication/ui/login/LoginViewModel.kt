package com.example.musicapplication.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.text.isEmpty

enum class LoginMode {ACCOUNT, REGISTER, FORGET}

@HiltViewModel
class LoginViewModel @Inject constructor(): ViewModel() {
    //这是状态[登陆，注册，忘记密码]
    private val _mode = MutableStateFlow(LoginMode.ACCOUNT)
    val mode: StateFlow<LoginMode> = _mode

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password
    private val _passwordAgain = MutableStateFlow("")
    val passwordAgain: StateFlow<String> = _passwordAgain

    private val _authCode = MutableStateFlow("")
    val authCode: StateFlow<String> = _authCode


    //改变状态方法,即切换组件
    fun changeMode(mode: LoginMode): Unit {
        _mode.value = mode
        clearAllValue()
    }
    //这边是改变观察值的变化
    fun emailUpdate(it: String) {
        _email.value = it
    }
    fun passwordUpdate(it: String) {
        _password.value = it
    }
    fun passwordAgainUpdate(it: String) {
        _passwordAgain.value = it
    }
    fun authCodeUpdate(it: String) {
        _authCode.value = it
    }


    fun ifCanLogin(): Boolean {
        return !_email.value.isEmpty() && !_password.value.isEmpty()
    }

    fun clearAllValue() {
        _email.value = ""
        _authCode.value = ""
        _password.value = ""
        _passwordAgain.value = ""
    }
}