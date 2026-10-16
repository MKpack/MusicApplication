package com.example.musicapplication.ui.auth.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.AuthRepository
import com.example.musicapplication.ui.auth.validation.AuthValidators
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    // 登录请求进行中：按钮转圈，且不能重复点击
    val isSubmitting: Boolean = false,
    // 登录失败的原因，显示在输入框下方
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // 注册成功、重置密码后回到登录页时带着邮箱，帮用户填好
    val email = TextFieldState(savedStateHandle.get<String>(RouterConfig.EMAIL_ARG).orEmpty())
    val password = TextFieldState()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // 登录成功只通知一次，页面收到后跳转主页
    private val _loggedIn = Channel<Unit>(Channel.BUFFERED)
    val loggedIn: Flow<Unit> = _loggedIn.receiveAsFlow()

    fun canLogin(): Boolean = email.text.isNotBlank() && password.text.isNotEmpty()

    fun login() {
        if (_uiState.value.isSubmitting || !canLogin()) return
        val address = email.text.toString().trim()
        if (!AuthValidators.isEmailValid(address)) {
            _uiState.update { it.copy(error = "邮箱格式不正确") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = authRepository.login(address, password.text.toString())) {
                    is RepositoryWorkResult.Success -> _loggedIn.send(Unit)
                    is RepositoryWorkResult.Failure -> _uiState.update { it.copy(error = result.message) }
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
