package com.example.musicapplication.ui.auth.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapplication.ui.auth.components.AuthBottomActions
import com.example.musicapplication.ui.auth.components.AuthHeadline
import com.example.musicapplication.ui.auth.components.AuthPage
import com.example.musicapplication.ui.auth.components.AuthScaffold
import com.example.musicapplication.ui.component.BrandLogo
import com.example.musicapplication.ui.component.FormSecureField
import com.example.musicapplication.ui.component.FormTextField
import com.example.musicapplication.ui.component.GroupedCard
import com.example.musicapplication.ui.component.GroupedDivider
import com.example.musicapplication.ui.component.GroupedFooter
import com.example.musicapplication.ui.component.LinkText
import com.example.musicapplication.ui.component.PasswordVisibilityToggle
import com.example.musicapplication.ui.theme.MusicApplicationTheme

/**
 * 登录页。
 *
 * @param onBack 为 null 时不显示返回按钮（登录过期时直接打开的登录页，下面没有别的页面）
 * @param onForgotPassword 带上已经填好的邮箱进入找回密码
 */
@Composable
fun LoginRoute(
    onBack: (() -> Unit)?,
    onLoggedIn: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoggedIn by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(viewModel) {
        viewModel.loggedIn.collect { currentOnLoggedIn() }
    }

    LoginScreen(
        email = viewModel.email,
        password = viewModel.password,
        uiState = uiState,
        onBack = onBack,
        onLogin = viewModel::login,
        onForgotPassword = { onForgotPassword(viewModel.email.text.toString().trim()) },
        onCreateAccount = onCreateAccount
    )
}

@Composable
fun LoginScreen(
    email: TextFieldState,
    password: TextFieldState,
    uiState: LoginUiState,
    onBack: (() -> Unit)?,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }
    val canLogin = email.text.isNotBlank() && password.text.isNotEmpty()

    AuthScaffold(onBack = onBack) {
        AuthPage(
            bottomBar = {
                AuthBottomActions(
                    buttonText = "登录",
                    onClick = onLogin,
                    enabled = canLogin,
                    loading = uiState.isSubmitting,
                    link = { LinkText(prefix = "还没有账号？", link = "创建账号", onClick = onCreateAccount) }
                )
            }
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            BrandLogo(size = 76.dp)
            Spacer(modifier = Modifier.height(22.dp))
            AuthHeadline(title = "登录 Shared Music", subtitle = AnnotatedString("使用你的邮箱和密码登录"))
            Spacer(modifier = Modifier.height(32.dp))
            GroupedCard {
                FormTextField(
                    label = "邮箱",
                    state = email,
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    onKeyboardAction = { passwordFocus.requestFocus() },
                    contentType = ContentType.Username + ContentType.EmailAddress
                )
                GroupedDivider()
                FormSecureField(
                    label = "密码",
                    state = password,
                    revealed = revealed,
                    placeholder = "输入密码",
                    imeAction = ImeAction.Done,
                    onKeyboardAction = { if (canLogin) onLogin() },
                    contentType = ContentType.Password,
                    focusRequester = passwordFocus,
                    trailing = { PasswordVisibilityToggle(revealed = revealed, onToggle = { revealed = !revealed }) }
                )
            }
            uiState.error?.let { GroupedFooter(text = it, isError = true) }
            LinkText(
                link = "忘记密码？",
                onClick = onForgotPassword,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 34.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun LoginPreview(darkTheme: Boolean, error: String? = null) {
    MusicApplicationTheme(darkTheme = darkTheme) {
        LoginScreen(
            email = rememberTextFieldState("music@example.com"),
            password = rememberTextFieldState("Music2026"),
            uiState = LoginUiState(error = error),
            onBack = {},
            onLogin = {},
            onForgotPassword = {},
            onCreateAccount = {}
        )
    }
}

@Preview(name = "登录页", widthDp = 393, heightDp = 852)
@Composable
private fun LoginLightPreview() = LoginPreview(darkTheme = false)

@Preview(name = "登录页 · 深色 · 错误", widthDp = 393, heightDp = 852)
@Composable
private fun LoginDarkPreview() = LoginPreview(darkTheme = true, error = "密码错误")
