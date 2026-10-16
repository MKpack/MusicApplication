package com.example.musicapplication.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicapplication.ui.theme.LocalMusicThemeColors

/*
 * iOS 分组样式的表单：圆角卡片里一行一个输入框，左边是固定宽度的标签，行之间用缩进的细分隔线。
 * 输入框用 TextFieldState 版本的 BasicTextField，自带撤销、粘贴，也能接系统的自动填充（密码管理器）。
 */

private val CardShape = RoundedCornerShape(20.dp)
private val RowHeight = 54.dp
private val LabelWidth = 92.dp
private val RowStartPadding = 18.dp

/** 分组卡片：浅色是白卡片，深色是 #1C1C1E，左右各留 16dp */
@Composable
fun GroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(CardShape)
            .background(LocalMusicThemeColors.current.neutral.card),
        content = content
    )
}

/** 卡片里两行之间的分隔线，左边和标签对齐 */
@Composable
fun GroupedDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = RowStartPadding),
        thickness = Dp.Hairline,
        color = LocalMusicThemeColors.current.neutral.separator
    )
}

/** 卡片下方的说明文字（和标签左对齐），[isError] 为 true 时显示成红色的错误提示 */
@Composable
fun GroupedFooter(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val neutral = LocalMusicThemeColors.current.neutral
    Text(
        text = text,
        color = if (isError) neutral.danger else neutral.labelSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp + RowStartPadding, end = 32.dp, top = 8.dp)
    )
}

/** 普通输入行：邮箱等 */
@Composable
fun FormTextField(
    label: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    trailing: (@Composable () -> Unit)? = null,
) {
    FormRow(label = label, focusRequester = focusRequester, modifier = modifier, trailing = trailing) {
        BasicTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .autofill(contentType),
            textStyle = fieldTextStyle(),
            cursorBrush = SolidColor(LocalMusicThemeColors.current.primary),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = TextFieldLineLimits.SingleLine,
            decorator = placeholderDecorator(state, placeholder)
        )
    }
}

/** 密码输入行。显示/隐藏由外面控制，这样「密码」和「确认密码」可以共用一个开关 */
@Composable
fun FormSecureField(
    label: String,
    state: TextFieldState,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    imeAction: ImeAction = ImeAction.Done,
    onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType = ContentType.Password,
    focusRequester: FocusRequester = remember { FocusRequester() },
    trailing: (@Composable () -> Unit)? = null,
) {
    FormRow(label = label, focusRequester = focusRequester, modifier = modifier, trailing = trailing) {
        BasicSecureTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .autofill(contentType),
            textStyle = fieldTextStyle(),
            cursorBrush = SolidColor(LocalMusicThemeColors.current.primary),
            // 显示明文时也保持密码键盘：不联想、不记录输入，切换时键盘也不会闪
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = imeAction
            ),
            onKeyboardAction = onKeyboardAction,
            textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
            decorator = placeholderDecorator(state, placeholder)
        )
    }
}

/** 行尾的绿色 ✓：邮箱格式正确、两次密码一致时出现 */
@Composable
fun ValidMark(
    visible: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(initialScale = 0.4f) + fadeIn(),
        exit = scaleOut(targetScale = 0.4f) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(22.dp)
                .background(LocalMusicThemeColors.current.neutral.success, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/** 行尾的小眼睛：切换密码明文显示 */
@Composable
fun PasswordVisibilityToggle(
    revealed: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (revealed) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            contentDescription = if (revealed) "隐藏密码" else "显示密码",
            tint = LocalMusicThemeColors.current.neutral.labelTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * 一行的骨架：点标签或空白处也能聚焦输入框，和 iOS 一样整行都可以点。
 * 用 pointerInput 而不是 clickable：clickable 会把输入框合并进整行的无障碍节点，读屏时找不到输入框。
 */
@Composable
private fun FormRow(
    label: String,
    focusRequester: FocusRequester,
    modifier: Modifier,
    trailing: (@Composable () -> Unit)?,
    field: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .pointerInput(focusRequester) {
                detectTapGestures { focusRequester.requestFocus() }
            }
            .padding(start = RowStartPadding, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LocalMusicThemeColors.current.neutral.label,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(LabelWidth)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            field()
        }
        trailing?.invoke()
    }
}

@Composable
private fun fieldTextStyle() = TextStyle(
    color = LocalMusicThemeColors.current.neutral.label,
    fontSize = 16.sp
)

/** 输入框为空时显示灰色的占位文字 */
@Composable
private fun placeholderDecorator(state: TextFieldState, placeholder: String): TextFieldDecorator {
    val color = LocalMusicThemeColors.current.neutral.labelTertiary
    return TextFieldDecorator { innerTextField ->
        Box(contentAlignment = Alignment.CenterStart) {
            if (state.text.isEmpty() && placeholder.isNotEmpty()) {
                Text(text = placeholder, color = color, fontSize = 16.sp, maxLines = 1)
            }
            innerTextField()
        }
    }
}

/** 告诉系统自动填充（密码管理器）这个输入框填什么 */
private fun Modifier.autofill(contentType: ContentType?): Modifier =
    if (contentType == null) this else semantics { this.contentType = contentType }
