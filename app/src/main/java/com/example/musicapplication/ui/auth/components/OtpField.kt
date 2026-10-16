package com.example.musicapplication.ui.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.auth.validation.AuthValidators
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/**
 * 验证码输入：[length] 条横线，每条上面显示一位数字，正在输入的那一格是主题色并有闪烁的光标。
 * 真正接收输入的是藏在横线下面的透明输入框：只接受数字、最多 [length] 位，
 * 粘贴「验证码：12345」这样的整段文字时只留下数字，一次填满。输满后调用 [onFilled]。
 */
@Composable
fun OtpField(
    state: TextFieldState,
    onFilled: () -> Unit,
    modifier: Modifier = Modifier,
    length: Int = AuthValidators.CODE_LENGTH,
    isError: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val currentOnFilled by rememberUpdatedState(onFilled)
    val transformation = remember(length) { DigitsOnlyTransformation(length) }

    LaunchedEffect(state, length) {
        snapshotFlow { state.text.length }.collect { if (it == length) currentOnFilled() }
    }

    // 输入框本身是透明的，选中文字时的高亮和拖动手柄也一起藏起来，长按仍然可以粘贴
    CompositionLocalProvider(LocalTextSelectionColors provides HiddenSelectionColors) {
        BasicTextField(
            state = state,
            modifier = modifier
                .focusRequester(focusRequester)
                .semantics { contentType = ContentType.SmsOtpCode },
            inputTransformation = transformation,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = interactionSource,
            decorator = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.matchParentSize()) { innerTextField() }
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        val text = state.text
                        repeat(length) { index ->
                            OtpSlot(
                                digit = text.getOrNull(index),
                                active = focused && index == text.length,
                                isError = isError
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun OtpSlot(digit: Char?, active: Boolean, isError: Boolean) {
    val colors = LocalMusicThemeColors.current
    val lineColor by animateColorAsState(
        targetValue = when {
            isError -> colors.neutral.danger
            active -> colors.primary
            digit != null -> colors.neutral.label
            else -> colors.neutral.labelTertiary
        },
        label = "otpLine"
    )
    Column(
        modifier = Modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (digit != null) {
                Text(
                    text = digit.toString(),
                    color = colors.neutral.label,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (active) {
                BlinkingCaret()
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(lineColor, CircleShape)
        )
    }
}

@Composable
private fun BlinkingCaret() {
    val alpha by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 500
                0f at 501
                0f at 1000
            }
        ),
        label = "caretAlpha"
    )
    Box(
        modifier = Modifier
            .size(width = 2.dp, height = 34.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(LocalMusicThemeColors.current.primary, RoundedCornerShape(1.dp))
    )
}

/** 只保留 0-9（全角数字、空格、其他文字都去掉），最多 [maxLength] 位 */
private class DigitsOnlyTransformation(private val maxLength: Int) : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        val text = asCharSequence()
        val digits = text.filter { it in '0'..'9' }.take(maxLength)
        if (digits.length != text.length) replace(0, length, digits)
    }
}

private val HiddenSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = Color.Transparent
)
