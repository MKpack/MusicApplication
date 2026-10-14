package com.example.musicapplication.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


//这是状态[登陆，注册，忘记密码]
enum class LoginMode {ACCOUNT, REGISTER, FORGET}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    //登陆成功：页面收到后跳转主页，再调用consumeLoginSuccess
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess = _loginSuccess.asStateFlow()
    //登陆失败的原因，显示在登录表单下方
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()
    //登录、注册、重置密码请求进行中：按钮显示转圈，且不能重复点击
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

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

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    //login网络请求
    fun login() {
        //开发阶段测试
        if (_email.value == "root1" && _password.value == "1") {
            _loginSuccess.value = true
            return
        }
        submit {
            when (val result = authRepository.login(_email.value, _password.value)) {
                is RepositoryWorkResult.Success -> {
                    _loginError.value = null
                    _loginSuccess.value = true
                }
                is RepositoryWorkResult.Failure -> {
                    _loginError.value = result.message
                }
            }
        }
    }

    fun sendCode() {
        viewModelScope.launch {
            _message.value = when (val result = authRepository.sendCode(_email.value)) {
                is RepositoryWorkResult.Success -> "验证码已发送"
                is RepositoryWorkResult.Failure -> result.message
            }
        }
    }

    fun register() {
        if (_password.value != _passwordAgain.value) {
            _message.value = "两次输入密码不同"
            return
        }
        submit {
            when (val result = authRepository.register(_email.value, _authCode.value, _password.value)) {
                is RepositoryWorkResult.Success -> {
                    backToLogin()
                    _message.value = "注册成功，请登录"
                }
                is RepositoryWorkResult.Failure -> {
                    _message.value = result.message
                }
            }
        }
    }

    fun reset() {
        submit {
            when (val result = authRepository.resetPassword(_email.value, _authCode.value, _password.value)) {
                is RepositoryWorkResult.Success -> {
                    backToLogin()
                    _message.value = "密码已重置，请重新登录"
                }
                is RepositoryWorkResult.Failure -> {
                    _message.value = result.message
                }
            }
        }
    }

    //同一时间只允许一个登录/注册/重置请求，防止连点重复提交
    private fun submit(request: suspend () -> Unit) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                request()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    //注册或重置密码成功后回到登录页，保留邮箱方便直接登录
    private fun backToLogin() {
        val email = _email.value
        changeMode(LoginMode.ACCOUNT)
        _email.value = email
    }


    fun isEmailValid(email: String) =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun consumeLoginSuccess() {
        _loginSuccess.value = false
    }
    //改变状态方法,即切换组件
    fun changeMode(mode: LoginMode): Unit {
        _mode.value = mode
        _loginError.value = null
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