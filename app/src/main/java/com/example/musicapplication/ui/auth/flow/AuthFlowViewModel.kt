package com.example.musicapplication.ui.auth.flow

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.data.repository.AuthRepository
import com.example.musicapplication.ui.auth.validation.AuthValidators
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthFlowUiState(
    val step: AuthStep = AuthStep.EMAIL,
    // 验证码发到了哪个邮箱，后面注册 / 重置密码都用它（第一步的输入框之后还可能被改）
    val sentTo: String = "",
    // 发送验证码、注册、重置密码的请求进行中：按钮转圈，且不能重复提交
    val isSubmitting: Boolean = false,
    // 显示在当前步骤里的错误提示，切换步骤时清掉
    val error: String? = null,
    // 还要等几秒才能重新发送验证码，0 表示现在就可以
    val resendSeconds: Int = 0,
)

/**
 * 注册 / 找回密码的分步流程。
 * 步骤推进：发送验证码成功 → CODE；验证码输满 5 位 → PASSWORD；提交成功 → DONE。
 * 后端到最后提交时才校验验证码，验证码不对就退回 CODE 重新输入，已经填好的密码保留。
 */
@HiltViewModel
class AuthFlowViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val flow = AuthFlow.fromRouteArg(savedStateHandle.get<String>(RouterConfig.AUTH_FLOW_ARG))

    // 输入框的内容直接放在 TextFieldState 里，页面和这里读写的是同一份
    val email = TextFieldState(savedStateHandle.get<String>(RouterConfig.EMAIL_ARG).orEmpty())
    val code = TextFieldState()
    val password = TextFieldState()
    val confirmPassword = TextFieldState()

    private val _uiState = MutableStateFlow(AuthFlowUiState())
    val uiState: StateFlow<AuthFlowUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    fun canSendCode(): Boolean = AuthValidators.isEmailValid(email.text.toString())

    // 不限制密码强度，填了并且两次一致就行
    fun canSubmit(): Boolean =
        password.text.isNotEmpty() && password.text.toString() == confirmPassword.text.toString()

    fun sendCode() {
        val state = _uiState.value
        if (state.isSubmitting || state.step != AuthStep.EMAIL || !canSendCode()) return
        val address = email.text.toString().trim()
        // 刚给这个邮箱发过、还在倒计时：不再重复发送，直接回到输入验证码
        if (address == state.sentTo && state.resendSeconds > 0) {
            _uiState.update { it.copy(step = AuthStep.CODE, error = null) }
            return
        }
        launchRequest {
            when (val result = authRepository.sendCode(address)) {
                is RepositoryWorkResult.Success -> {
                    code.clearText()
                    _uiState.update { it.copy(step = AuthStep.CODE, sentTo = address, error = null) }
                    startCountdown()
                }
                is RepositoryWorkResult.Failure -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (state.isSubmitting || state.step != AuthStep.CODE || state.resendSeconds > 0) return
        launchRequest {
            when (val result = authRepository.sendCode(state.sentTo)) {
                is RepositoryWorkResult.Success -> {
                    code.clearText()
                    _uiState.update { it.copy(error = null) }
                    startCountdown()
                }
                is RepositoryWorkResult.Failure -> _uiState.update { it.copy(error = result.message) }
            }
        }
    }

    /** 验证码输满 5 位后由页面调用，进入设置密码 */
    fun onCodeFilled() {
        if (_uiState.value.step != AuthStep.CODE || code.text.length != AuthValidators.CODE_LENGTH) return
        _uiState.update { it.copy(step = AuthStep.PASSWORD, error = null) }
    }

    /** 验证码页的「修改」：回到第一步改邮箱 */
    fun editEmail() {
        if (_uiState.value.step != AuthStep.CODE) return
        _uiState.update { it.copy(step = AuthStep.EMAIL, error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting || state.step != AuthStep.PASSWORD || !canSubmit()) return
        val authCode = code.text.toString()
        val newPassword = password.text.toString()
        launchRequest {
            val result = when (flow) {
                AuthFlow.REGISTER -> authRepository.register(state.sentTo, authCode, newPassword)
                AuthFlow.RESET -> authRepository.resetPassword(state.sentTo, authCode, newPassword)
            }
            when (result) {
                is RepositoryWorkResult.Success -> {
                    countdown?.cancel()
                    _uiState.update { it.copy(step = AuthStep.DONE, error = null, resendSeconds = 0) }
                }
                is RepositoryWorkResult.Failure -> onSubmitFailed(result)
            }
        }
    }

    /**
     * 返回上一步。在第一步（或完成页）时返回 false，由页面退出整个流程。
     * 从设置密码退回验证码时清空验证码，否则输满 5 位又会马上跳回来。
     */
    fun back(): Boolean {
        val previous = when (_uiState.value.step) {
            AuthStep.EMAIL, AuthStep.DONE -> return false
            AuthStep.CODE -> AuthStep.EMAIL
            AuthStep.PASSWORD -> {
                code.clearText()
                AuthStep.CODE
            }
        }
        _uiState.update { it.copy(step = previous, error = null) }
        return true
    }

    private fun onSubmitFailed(failure: RepositoryWorkResult.Failure) {
        when (failure.code) {
            CODE_INVALID -> {
                code.clearText()
                _uiState.update { it.copy(step = AuthStep.CODE, error = "验证码错误或已过期，请重新输入") }
            }
            // 注册时邮箱已经被注册过：回到第一步换一个邮箱
            EMAIL_ALREADY_REGISTERED -> _uiState.update { it.copy(step = AuthStep.EMAIL, error = failure.message) }
            else -> _uiState.update { it.copy(error = failure.message) }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (seconds in RESEND_SECONDS downTo 1) {
                _uiState.update { it.copy(resendSeconds = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(resendSeconds = 0) }
        }
    }

    // 同一时间只允许一个请求，防止连点重复提交
    private fun launchRequest(request: suspend () -> Unit) {
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                request()
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    companion object {
        const val RESEND_SECONDS = 60

        // 后端 ResultCode：验证码无效、邮箱已注册
        private const val CODE_INVALID = 1002
        private const val EMAIL_ALREADY_REGISTERED = 1013
    }
}
