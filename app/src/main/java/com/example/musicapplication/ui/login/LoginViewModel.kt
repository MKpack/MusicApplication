package com.example.musicapplication.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.data.repository.LoginRepositoryImpi
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.text.isEmpty


//这是状态[登陆，注册，忘记密码]
enum class LoginMode {ACCOUNT, REGISTER, FORGET}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepositoryImpi: LoginRepositoryImpi
) : ViewModel() {
    //登陆状态
    //登陆：标识是否登陆成功以及失败
    private val _loginStatus = MutableStateFlow("")
    val loginStatus: StateFlow<String> = _loginStatus
    //注册：标识数值的错误性
    private val _registerStatus = MutableStateFlow("")
    val registerStatus: StateFlow<String> = _registerStatus
    //重置密码
    private val _resetStatus = MutableStateFlow("")
    val resetStatus: StateFlow<String> = _resetStatus

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

    private var message = ""

    //login网络请求
    fun login() {
        viewModelScope.launch {
            val msg = loginRepositoryImpi.login(_email.value, _password.value)
            if (msg != "success")
                _loginStatus.value = "fail"
            else
                _loginStatus.value = "success"
        }
    }

    fun sendCode() {
        viewModelScope.launch {
            val msg = loginRepositoryImpi.getAuthCode(_email.value)
            message = msg
        }
    }

    fun register(): String {
        viewModelScope.launch {
            val msg = loginRepositoryImpi.register(
                _email.value, _authCode.value, _password.value, _passwordAgain.value
            )
            if (msg == "success") {
                _mode.value = LoginMode.ACCOUNT
            }

            message = msg
        }
        return message
    }


    fun isEmailValid(email: String) =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun changeLoginStatus() {
        _loginStatus.value = ""
    }
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

    fun ifCanRegister(): Boolean {
        return !_email.value.isEmpty() && !_authCode.value.isEmpty() &&
                !_password.value.isEmpty() && !_passwordAgain.value.isEmpty()
                && _password.value == _passwordAgain.value && isEmailValid(_email.value)
    }

    fun ifCanReset(): Boolean {
        return !_email.value.isEmpty() && !_authCode.value.isEmpty() &&
                !_password.value.isEmpty()
    }

    fun clearAllValue() {
        _email.value = ""
        _authCode.value = ""
        _password.value = ""
        _passwordAgain.value = ""
    }
}