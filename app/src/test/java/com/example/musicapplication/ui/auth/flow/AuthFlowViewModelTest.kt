package com.example.musicapplication.ui.auth.flow

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.testing.FakeAuthRepository
import com.example.musicapplication.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(flow: String? = "register", email: String? = null) = AuthFlowViewModel(
        authRepository = repository,
        savedStateHandle = SavedStateHandle(mapOf(RouterConfig.AUTH_FLOW_ARG to flow, RouterConfig.EMAIL_ARG to email))
    )

    private val AuthFlowViewModel.step get() = uiState.value.step

    /** 填邮箱、发验证码、输满验证码，停在设置密码这一步 */
    private fun TestScope.reachPasswordStep(viewModel: AuthFlowViewModel) {
        viewModel.email.setTextAndPlaceCursorAtEnd("music@example.com")
        viewModel.sendCode()
        runCurrent()
        viewModel.code.setTextAndPlaceCursorAtEnd("12345")
        viewModel.onCodeFilled()
        assertEquals(AuthStep.PASSWORD, viewModel.step)
    }

    private fun AuthFlowViewModel.typePasswords(first: String, second: String = first) {
        password.setTextAndPlaceCursorAtEnd(first)
        confirmPassword.setTextAndPlaceCursorAtEnd(second)
    }

    @Test
    fun flowAndEmailComeFromNavArguments() {
        assertEquals(AuthFlow.RESET, createViewModel(flow = "reset", email = "music@example.com").flow)
        assertEquals("music@example.com", createViewModel(flow = "reset", email = "music@example.com").email.text.toString())
        assertEquals(AuthFlow.REGISTER, createViewModel(flow = null).flow)
    }

    @Test
    fun sendCode_movesToCodeStepAndStartsCountdown() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd(" music@example.com ")

        viewModel.sendCode()
        runCurrent()

        assertEquals(AuthStep.CODE, viewModel.step)
        assertEquals("music@example.com", viewModel.uiState.value.sentTo)
        assertEquals(listOf("sendCode(music@example.com)"), repository.calls)
        assertEquals(60, viewModel.uiState.value.resendSeconds)

        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(30, viewModel.uiState.value.resendSeconds)

        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.resendSeconds)
    }

    @Test
    fun sendCode_invalidEmail_doesNothing() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music@")

        viewModel.sendCode()
        advanceUntilIdle()

        assertEquals(AuthStep.EMAIL, viewModel.step)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun sendCode_failure_staysOnEmailWithError() = runTest {
        repository.sendCodeResult = RepositoryWorkResult.Failure("网络请求失败")
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music@example.com")

        viewModel.sendCode()
        advanceUntilIdle()

        assertEquals(AuthStep.EMAIL, viewModel.step)
        assertEquals("网络请求失败", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun editEmail_sameEmailDuringCountdown_doesNotSendAgain() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music@example.com")
        viewModel.sendCode()
        runCurrent()

        viewModel.editEmail()
        assertEquals(AuthStep.EMAIL, viewModel.step)
        viewModel.sendCode()
        runCurrent()

        assertEquals(AuthStep.CODE, viewModel.step)
        assertEquals(1, repository.calls.size)

        // 换了邮箱就重新发送
        viewModel.editEmail()
        viewModel.email.setTextAndPlaceCursorAtEnd("other@example.com")
        viewModel.sendCode()
        runCurrent()
        assertEquals("sendCode(other@example.com)", repository.calls.last())
        assertEquals("other@example.com", viewModel.uiState.value.sentTo)
    }

    @Test
    fun resendCode_onlyAfterCountdownEnds() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music@example.com")
        viewModel.sendCode()
        runCurrent()

        viewModel.resendCode()
        runCurrent()
        assertEquals(1, repository.calls.size)

        advanceTimeBy(60_000)
        runCurrent()
        viewModel.resendCode()
        runCurrent()
        assertEquals(2, repository.calls.size)
        assertEquals(60, viewModel.uiState.value.resendSeconds)
    }

    @Test
    fun codeFilled_movesToPasswordOnlyWhenComplete() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music@example.com")
        viewModel.sendCode()
        runCurrent()

        viewModel.code.setTextAndPlaceCursorAtEnd("1234")
        viewModel.onCodeFilled()
        assertEquals(AuthStep.CODE, viewModel.step)

        viewModel.code.setTextAndPlaceCursorAtEnd("12345")
        viewModel.onCodeFilled()
        assertEquals(AuthStep.PASSWORD, viewModel.step)
    }

    @Test
    fun submit_register_movesToDone() = runTest {
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthStep.DONE, viewModel.step)
        assertEquals("register(music@example.com, 12345, Music2026)", repository.calls.last())
    }

    @Test
    fun submit_reset_callsResetPassword() = runTest {
        val viewModel = createViewModel(flow = "reset")
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthStep.DONE, viewModel.step)
        assertEquals("resetPassword(music@example.com, 12345, Music2026)", repository.calls.last())
    }

    @Test
    fun submit_mismatchedPasswords_doesNothing() = runTest {
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026", "Music2025")

        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.canSubmit())
        assertEquals(AuthStep.PASSWORD, viewModel.step)
        assertEquals(1, repository.calls.size)
    }

    @Test
    fun submit_invalidCode_returnsToCodeStepAndKeepsPassword() = runTest {
        repository.registerResult = RepositoryWorkResult.Failure("验证码无效", 1002)
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthStep.CODE, viewModel.step)
        assertEquals("", viewModel.code.text.toString())
        assertEquals("验证码错误或已过期，请重新输入", viewModel.uiState.value.error)
        assertEquals("Music2026", viewModel.password.text.toString())

        // 重新输入验证码后回到密码页，错误提示清掉
        viewModel.code.setTextAndPlaceCursorAtEnd("54321")
        viewModel.onCodeFilled()
        assertEquals(AuthStep.PASSWORD, viewModel.step)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun submit_emailAlreadyRegistered_returnsToEmailStep() = runTest {
        repository.registerResult = RepositoryWorkResult.Failure("邮箱已注册", 1013)
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthStep.EMAIL, viewModel.step)
        assertEquals("邮箱已注册", viewModel.uiState.value.error)
    }

    @Test
    fun submit_otherFailure_staysWithError() = runTest {
        repository.registerResult = RepositoryWorkResult.Failure("网络请求失败")
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AuthStep.PASSWORD, viewModel.step)
        assertEquals("网络请求失败", viewModel.uiState.value.error)
    }

    @Test
    fun submit_repeatedTapsWhileSubmitting_sendOneRequest() = runTest {
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")
        repository.latencyMillis = 1_000

        viewModel.submit()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, repository.calls.count { it.startsWith("register") })
    }

    @Test
    fun back_goesToPreviousStepAndExitsFromFirst() = runTest {
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)

        assertTrue(viewModel.back())
        assertEquals(AuthStep.CODE, viewModel.step)
        // 退回验证码时清空，否则又会马上自动跳到密码页
        assertEquals("", viewModel.code.text.toString())

        assertTrue(viewModel.back())
        assertEquals(AuthStep.EMAIL, viewModel.step)

        assertFalse(viewModel.back())
        assertEquals(AuthStep.EMAIL, viewModel.step)
    }

    @Test
    fun back_fromDone_isHandledByPage() = runTest {
        val viewModel = createViewModel()
        reachPasswordStep(viewModel)
        viewModel.typePasswords("Music2026")
        viewModel.submit()
        advanceUntilIdle()

        assertFalse(viewModel.back())
        assertEquals(AuthStep.DONE, viewModel.step)
    }
}
