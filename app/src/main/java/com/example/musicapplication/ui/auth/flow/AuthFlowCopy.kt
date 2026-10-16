package com.example.musicapplication.ui.auth.flow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

/** 注册和找回密码每一步的文案、图标，两个流程只有这些不一样 */
internal class AuthFlowCopy(
    val emailIcon: ImageVector,
    val emailTitle: String,
    val emailSubtitle: String,
    val sendCodeButton: String,
    val backToLoginPrompt: String,
    val backToLoginLink: String,
    val passwordTitle: String,
    val passwordLabel: String,
    val confirmLabel: String,
    val submitButton: String,
    val doneIcon: ImageVector,
    val doneTitle: String,
    val doneSubtitle: String,
    val doneButton: String,
)

internal val AuthFlow.copy: AuthFlowCopy
    get() = when (this) {
        AuthFlow.REGISTER -> RegisterCopy
        AuthFlow.RESET -> ResetCopy
    }

private val RegisterCopy = AuthFlowCopy(
    emailIcon = Icons.Rounded.Mail,
    emailTitle = "创建账号",
    emailSubtitle = "输入你的邮箱，我们会发送\n一个 5 位验证码",
    sendCodeButton = "获取验证码",
    backToLoginPrompt = "已有账号？",
    backToLoginLink = "去登录",
    passwordTitle = "设置密码",
    passwordLabel = "密码",
    confirmLabel = "确认密码",
    submitButton = "完成注册",
    doneIcon = Icons.Rounded.Check,
    doneTitle = "注册成功",
    doneSubtitle = "欢迎加入 Shared Music\n用刚才的邮箱和密码登录吧",
    doneButton = "去登录",
)

private val ResetCopy = AuthFlowCopy(
    emailIcon = Icons.Rounded.LockReset,
    emailTitle = "找回密码",
    emailSubtitle = "输入注册时使用的邮箱，\n我们会发送一个 5 位验证码",
    sendCodeButton = "发送验证码",
    backToLoginPrompt = "想起来了？",
    backToLoginLink = "返回登录",
    passwordTitle = "设置新密码",
    passwordLabel = "新密码",
    confirmLabel = "确认新密码",
    submitButton = "重置密码",
    doneIcon = Icons.Rounded.VerifiedUser,
    doneTitle = "密码已重置",
    doneSubtitle = "旧密码已失效\n请使用新密码登录 Shared Music",
    doneButton = "返回登录",
)
