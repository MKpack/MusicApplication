package com.example.musicapplication.ui.auth.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.common.RepositoryWorkResult
import com.example.musicapplication.testing.FakeAuthRepository
import com.example.musicapplication.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private fun createViewModel(email: String? = null) = LoginViewModel(
        authRepository = repository,
        savedStateHandle = SavedStateHandle(mapOf(RouterConfig.EMAIL_ARG to email))
    )

    @Test
    fun emailFromNavArgumentIsPrefilled() {
        val viewModel = createViewModel(email = "music@example.com")

        assertEquals("music@example.com", viewModel.email.text.toString())
        assertEquals("", viewModel.password.text.toString())
    }

    @Test
    fun emptyFields_cannotLogin() = runTest {
        val viewModel = createViewModel()

        viewModel.login()
        advanceUntilIdle()

        assertFalse(viewModel.canLogin())
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun invalidEmail_showsErrorWithoutRequest() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd("music")
        viewModel.password.setTextAndPlaceCursorAtEnd("secret")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("邮箱格式不正确", viewModel.uiState.value.error)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun success_emitsLoggedIn() = runTest {
        val viewModel = createViewModel()
        viewModel.email.setTextAndPlaceCursorAtEnd(" music@example.com ")
        viewModel.password.setTextAndPlaceCursorAtEnd("secret")

        viewModel.login()
        advanceUntilIdle()

        viewModel.loggedIn.first()
        assertEquals(listOf("login(music@example.com, secret)"), repository.calls)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun failure_showsServerMessage() = runTest {
        repository.loginResult = RepositoryWorkResult.Failure("密码错误", 1004)
        val viewModel = createViewModel(email = "music@example.com")
        viewModel.password.setTextAndPlaceCursorAtEnd("wrong")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("密码错误", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun repeatedTapsWhileSubmitting_sendOneRequest() = runTest {
        repository.latencyMillis = 1_000
        val viewModel = createViewModel(email = "music@example.com")
        viewModel.password.setTextAndPlaceCursorAtEnd("secret")

        viewModel.login()
        runCurrent()
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.login()
        viewModel.login()
        advanceUntilIdle()

        assertEquals(1, repository.calls.size)
    }
}
