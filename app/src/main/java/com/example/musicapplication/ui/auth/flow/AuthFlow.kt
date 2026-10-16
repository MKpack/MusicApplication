package com.example.musicapplication.ui.auth.flow

/** 注册和找回密码走同一套分步页面，只是文案、图标和最后调用的接口不同 */
enum class AuthFlow(val routeArg: String) {
    REGISTER("register"),
    RESET("reset");

    companion object {
        fun fromRouteArg(arg: String?): AuthFlow = entries.firstOrNull { it.routeArg == arg } ?: REGISTER
    }
}

/**
 * 流程里的步骤：输入邮箱 → 输入验证码 → 设置密码 → 完成。
 * [indicator] 是顶部进度条显示的第几步，完成页不显示进度条（0）。
 */
enum class AuthStep(val indicator: Int) {
    EMAIL(1),
    CODE(2),
    PASSWORD(3),
    DONE(0),
}
