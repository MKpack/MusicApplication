package com.example.musicapplication.config

object RouterConfig {

    // 未登录时的页面都在这个嵌套导航图里：欢迎页 → 登录 / 注册 / 找回密码。登录成功后整个图一起出栈
    const val AUTH_GRAPH = "auth"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    // 注册、找回密码共用的分步流程，参数 flow 区分是哪一个（register / reset）
    const val AUTH_FLOW = "authFlow"
    const val AUTH_FLOW_ARG = "flow"
    // 预先填好的邮箱：注册成功后回到登录页、从登录页进入找回密码时带上
    const val EMAIL_ARG = "email"
    const val MAINPAGE = "mainPage"

    const val HOME = "home"
    const val PROFILE = "profile"
    const val SEARCH = "search"
}
