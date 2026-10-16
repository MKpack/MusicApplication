package com.example.musicapplication.ui.auth.flow

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapplication.ui.auth.components.AuthBottomActions
import com.example.musicapplication.ui.auth.components.AuthHeadline
import com.example.musicapplication.ui.auth.components.AuthHero
import com.example.musicapplication.ui.auth.components.AuthPage
import com.example.musicapplication.ui.auth.components.AuthScaffold
import com.example.musicapplication.ui.auth.components.FloatingCovers
import com.example.musicapplication.ui.auth.components.OtpField
import com.example.musicapplication.ui.auth.components.SuccessBadge
import com.example.musicapplication.ui.auth.validation.AuthValidators
import com.example.musicapplication.ui.component.FormSecureField
import com.example.musicapplication.ui.component.FormTextField
import com.example.musicapplication.ui.component.GroupedCard
import com.example.musicapplication.ui.component.GroupedDivider
import com.example.musicapplication.ui.component.GroupedFooter
import com.example.musicapplication.ui.component.LinkText
import com.example.musicapplication.ui.component.PasswordVisibilityToggle
import com.example.musicapplication.ui.component.ValidMark
import com.example.musicapplication.ui.theme.LocalMusicThemeColors
import com.example.musicapplication.ui.theme.MusicApplicationTheme

/**
 * 注册 / 找回密码的分步页面。
 * 系统返回键和左上角返回按钮都回到上一步；在第一步返回时退出流程（回到登录页或欢迎页）。
 *
 * @param onExit 在第一步按返回
 * @param onLogin 第一步底部的「去登录 / 返回登录」
 * @param onFinished 完成页的按钮，带上刚才的邮箱回到登录页
 */
@Composable
fun AuthFlowRoute(
    onExit: () -> Unit,
    onLogin: () -> Unit,
    onFinished: (email: String) -> Unit,
    viewModel: AuthFlowViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 第一步不拦截，交给导航返回上一页
    BackHandler(enabled = uiState.step != AuthStep.EMAIL) {
        if (uiState.step == AuthStep.DONE) onFinished(uiState.sentTo) else viewModel.back()
    }

    AuthFlowScreen(
        flow = viewModel.flow,
        uiState = uiState,
        email = viewModel.email,
        code = viewModel.code,
        password = viewModel.password,
        confirmPassword = viewModel.confirmPassword,
        onBack = { if (!viewModel.back()) onExit() },
        onSendCode = viewModel::sendCode,
        onResendCode = viewModel::resendCode,
        onCodeFilled = viewModel::onCodeFilled,
        onEditEmail = viewModel::editEmail,
        onSubmit = viewModel::submit,
        onLogin = onLogin,
        onFinished = { onFinished(uiState.sentTo) }
    )
}

@Composable
fun AuthFlowScreen(
    flow: AuthFlow,
    uiState: AuthFlowUiState,
    email: TextFieldState,
    code: TextFieldState,
    password: TextFieldState,
    confirmPassword: TextFieldState,
    onBack: () -> Unit,
    onSendCode: () -> Unit,
    onResendCode: () -> Unit,
    onCodeFilled: () -> Unit,
    onEditEmail: () -> Unit,
    onSubmit: () -> Unit,
    onLogin: () -> Unit,
    onFinished: () -> Unit,
) {
    val background = LocalMusicThemeColors.current.neutral.groupedBackground
    AuthScaffold(
        onBack = if (uiState.step == AuthStep.DONE) null else onBack,
        step = uiState.step.indicator
    ) {
        // 顶栏不动，下面整页左右滑：前进时新页从右边盖上来，后退时当前页往右滑走
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val transform = if (forward) {
                    slideIntoContainer(SlideDirection.Start, stepSpec()) togetherWith
                        slideOutOfContainer(SlideDirection.Start, stepSpec(), targetOffset = { it / 4 }) + fadeOut(stepSpec())
                } else {
                    slideIntoContainer(SlideDirection.End, stepSpec(), initialOffset = { it / 4 }) + fadeIn(stepSpec()) togetherWith
                        slideOutOfContainer(SlideDirection.End, stepSpec())
                }
                // 前进时新页面在上层，后退时正在离开的页面在上层
                transform.targetContentZIndex = if (forward) 1f else -1f
                transform
            },
            label = "authStep"
        ) { step ->
            // 每页铺满底色，滑动时上面一页能完整盖住下面一页
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
            ) {
                when (step) {
                    AuthStep.EMAIL -> EmailStep(flow, uiState, email, onSendCode, onLogin)
                    AuthStep.CODE -> CodeStep(uiState, code, onCodeFilled, onResendCode, onEditEmail)
                    AuthStep.PASSWORD -> PasswordStep(flow, uiState, password, confirmPassword, onSubmit)
                    AuthStep.DONE -> DoneStep(flow, onFinished)
                }
            }
        }
    }
}

private fun <T> stepSpec() = tween<T>(350, easing = FastOutSlowInEasing)

@Composable
private fun EmailStep(
    flow: AuthFlow,
    uiState: AuthFlowUiState,
    email: TextFieldState,
    onSendCode: () -> Unit,
    onLogin: () -> Unit,
) {
    val copy = flow.copy
    val emailValid = AuthValidators.isEmailValid(email.text.toString())
    val focusRequester = remember { FocusRequester() }
    // 第一次进来邮箱是空的：直接弹出键盘
    LaunchedEffect(Unit) {
        if (email.text.isEmpty()) focusRequester.requestFocus()
    }

    AuthPage(
        bottomBar = {
            AuthBottomActions(
                buttonText = copy.sendCodeButton,
                onClick = onSendCode,
                enabled = emailValid,
                loading = uiState.isSubmitting,
                link = { LinkText(prefix = copy.backToLoginPrompt, link = copy.backToLoginLink, onClick = onLogin) }
            )
        }
    ) {
        AuthHero(icon = copy.emailIcon, title = copy.emailTitle, subtitle = AnnotatedString(copy.emailSubtitle))
        Spacer(modifier = Modifier.height(22.dp))
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
                onKeyboardAction = { if (emailValid) onSendCode() },
                contentType = ContentType.EmailAddress +
                    if (flow == AuthFlow.REGISTER) ContentType.NewUsername else ContentType.Username,
                focusRequester = focusRequester,
                trailing = { ValidMark(visible = emailValid, contentDescription = "邮箱格式正确") }
            )
        }
        val error = uiState.error
        GroupedFooter(text = error ?: "验证码 10 分钟内有效。", isError = error != null)
    }
}

@Composable
private fun CodeStep(
    uiState: AuthFlowUiState,
    code: TextFieldState,
    onCodeFilled: () -> Unit,
    onResendCode: () -> Unit,
    onEditEmail: () -> Unit,
) {
    val colors = LocalMusicThemeColors.current
    val focusRequester = remember { FocusRequester() }
    // 进入这一步自动弹出数字键盘
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AuthPage {
        AuthHero(
            icon = Icons.Rounded.MarkEmailRead,
            title = "输入验证码",
            subtitle = buildAnnotatedString {
                append("已发送至 ")
                withStyle(SpanStyle(color = colors.neutral.label, fontWeight = FontWeight.SemiBold)) {
                    append(uiState.sentTo)
                }
                append("  ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "editEmail",
                        styles = TextLinkStyles(SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold))
                    ) { onEditEmail() }
                ) {
                    append("修改")
                }
            }
        )
        Spacer(modifier = Modifier.height(30.dp))
        OtpField(
            state = code,
            onFilled = onCodeFilled,
            // 重新开始输入后横线恢复正常颜色，错误文字留到下一步
            isError = uiState.error != null && code.text.isEmpty(),
            focusRequester = focusRequester
        )
        Spacer(modifier = Modifier.height(28.dp))
        uiState.error?.let { error ->
            Text(
                text = error,
                color = colors.neutral.danger,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
            )
        }
        ResendCode(
            seconds = uiState.resendSeconds,
            sending = uiState.isSubmitting,
            onResend = onResendCode
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.neutral.labelTertiary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = "输满 5 位自动进入下一步", color = colors.neutral.labelTertiary, fontSize = 13.sp)
        }
    }
}

/** 倒计时中显示「56 秒后可以重新发送」，结束后变成「没有收到？重新发送」 */
@Composable
private fun ResendCode(seconds: Int, sending: Boolean, onResend: () -> Unit) {
    val neutral = LocalMusicThemeColors.current.neutral
    if (seconds > 0) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = neutral.label, fontWeight = FontWeight.SemiBold)) { append("$seconds") }
                append(" 秒后可以重新发送")
            },
            color = neutral.labelSecondary,
            fontSize = 15.sp,
            // 和链接的点击区域一样高，倒计时结束切换成链接时下面的提示不会跳动
            modifier = Modifier.padding(vertical = 10.dp)
        )
    } else {
        LinkText(
            prefix = "没有收到？",
            link = if (sending) "正在发送…" else "重新发送",
            onClick = onResend,
            enabled = !sending
        )
    }
}

@Composable
private fun PasswordStep(
    flow: AuthFlow,
    uiState: AuthFlowUiState,
    password: TextFieldState,
    confirmPassword: TextFieldState,
    onSubmit: () -> Unit,
) {
    val copy = flow.copy
    val colors = LocalMusicThemeColors.current
    var revealed by rememberSaveable { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { passwordFocus.requestFocus() }

    val first = password.text.toString()
    val second = confirmPassword.text.toString()
    val matched = first.isNotEmpty() && first == second
    // 确认密码还在输入（是第一个密码的开头）时先不提示，输错一位才提示
    val mismatched = second.isNotEmpty() && !first.startsWith(second)

    AuthPage(
        bottomBar = {
            AuthBottomActions(
                buttonText = copy.submitButton,
                onClick = onSubmit,
                enabled = matched,
                loading = uiState.isSubmitting
            )
        }
    ) {
        AuthHero(
            icon = Icons.Rounded.Lock,
            title = copy.passwordTitle,
            subtitle = passwordSubtitle(flow, uiState.sentTo, colors.neutral.label)
        )
        Spacer(modifier = Modifier.height(22.dp))
        GroupedCard {
            FormSecureField(
                label = copy.passwordLabel,
                state = password,
                revealed = revealed,
                placeholder = "输入密码",
                imeAction = ImeAction.Next,
                onKeyboardAction = { confirmFocus.requestFocus() },
                contentType = ContentType.NewPassword,
                focusRequester = passwordFocus,
                trailing = { PasswordVisibilityToggle(revealed = revealed, onToggle = { revealed = !revealed }) }
            )
            GroupedDivider()
            FormSecureField(
                label = copy.confirmLabel,
                state = confirmPassword,
                revealed = revealed,
                placeholder = "再次输入密码",
                imeAction = ImeAction.Done,
                onKeyboardAction = { if (matched) onSubmit() },
                contentType = ContentType.NewPassword,
                focusRequester = confirmFocus,
                trailing = { ValidMark(visible = matched, contentDescription = "两次输入一致") }
            )
        }
        when {
            uiState.error != null -> GroupedFooter(text = uiState.error, isError = true)
            mismatched -> GroupedFooter(text = "两次输入的密码不一致", isError = true)
        }
    }
}

private fun passwordSubtitle(flow: AuthFlow, email: String, emphasis: Color): AnnotatedString = when (flow) {
    AuthFlow.REGISTER -> buildAnnotatedString {
        append("以后用 ")
        withStyle(SpanStyle(color = emphasis, fontWeight = FontWeight.SemiBold)) { append(email) }
        append("\n和这个密码登录")
    }
    AuthFlow.RESET -> AnnotatedString("设置完成后，\n请使用新密码重新登录")
}

@Composable
private fun DoneStep(flow: AuthFlow, onFinished: () -> Unit) {
    val copy = flow.copy
    AuthPage(
        bottomBar = { AuthBottomActions(buttonText = copy.doneButton, onClick = onFinished) }
    ) {
        // 徽章中心离顶部 160dp，最上面的封面不会被滚动区域裁掉；
        // 区域只占 290dp 高，标题紧跟在徽章下面（下方多出来的 30dp 里没有东西）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Top, unbounded = true)
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                if (flow == AuthFlow.REGISTER) FloatingCovers()
                SuccessBadge(icon = copy.doneIcon)
            }
        }
        AuthHeadline(title = copy.doneTitle, subtitle = AnnotatedString(copy.doneSubtitle))
    }
}

@Composable
private fun AuthFlowPreview(flow: AuthFlow, step: AuthStep, darkTheme: Boolean = false) {
    MusicApplicationTheme(darkTheme = darkTheme) {
        AuthFlowScreen(
            flow = flow,
            uiState = AuthFlowUiState(step = step, sentTo = "music@example.com", resendSeconds = 56),
            email = rememberTextFieldState("music@example.com"),
            code = rememberTextFieldState("582"),
            password = rememberTextFieldState("Music2026"),
            confirmPassword = rememberTextFieldState("Music2026"),
            onBack = {},
            onSendCode = {},
            onResendCode = {},
            onCodeFilled = {},
            onEditEmail = {},
            onSubmit = {},
            onLogin = {},
            onFinished = {}
        )
    }
}

@Preview(name = "注册 · 邮箱", widthDp = 393, heightDp = 852)
@Composable
private fun RegisterEmailPreview() = AuthFlowPreview(AuthFlow.REGISTER, AuthStep.EMAIL)

@Preview(name = "注册 · 验证码", widthDp = 393, heightDp = 852)
@Composable
private fun RegisterCodePreview() = AuthFlowPreview(AuthFlow.REGISTER, AuthStep.CODE)

@Preview(name = "注册 · 密码", widthDp = 393, heightDp = 852)
@Composable
private fun RegisterPasswordPreview() = AuthFlowPreview(AuthFlow.REGISTER, AuthStep.PASSWORD)

@Preview(name = "注册 · 完成", widthDp = 393, heightDp = 852)
@Composable
private fun RegisterDonePreview() = AuthFlowPreview(AuthFlow.REGISTER, AuthStep.DONE)

@Preview(name = "找回密码 · 邮箱 · 深色", widthDp = 393, heightDp = 852)
@Composable
private fun ResetEmailDarkPreview() = AuthFlowPreview(AuthFlow.RESET, AuthStep.EMAIL, darkTheme = true)

@Preview(name = "找回密码 · 完成", widthDp = 393, heightDp = 852)
@Composable
private fun ResetDonePreview() = AuthFlowPreview(AuthFlow.RESET, AuthStep.DONE)
