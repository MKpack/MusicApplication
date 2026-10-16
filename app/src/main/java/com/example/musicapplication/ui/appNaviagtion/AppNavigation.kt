package com.example.musicapplication.ui.appNaviagtion

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.session.AuthState
import com.example.musicapplication.ui.auth.flow.AuthFlow
import com.example.musicapplication.ui.auth.flow.AuthFlowRoute
import com.example.musicapplication.ui.auth.login.LoginRoute
import com.example.musicapplication.ui.auth.welcome.WelcomeScreen
import com.example.musicapplication.ui.mainPage.MainPage
import com.example.musicapplication.ui.mainPage.MainPageViewModel
import com.example.musicapplication.ui.session.SessionViewModel

// 登录页、注册 / 找回密码流程的路由，邮箱是可选参数
private const val LOGIN_ROUTE = "${RouterConfig.LOGIN}?${RouterConfig.EMAIL_ARG}={${RouterConfig.EMAIL_ARG}}"
private const val AUTH_FLOW_ROUTE =
    "${RouterConfig.AUTH_FLOW}/{${RouterConfig.AUTH_FLOW_ARG}}?${RouterConfig.EMAIL_ARG}={${RouterConfig.EMAIL_ARG}}"

private fun loginRoute(email: String? = null) = withEmail(RouterConfig.LOGIN, email)

private fun authFlowRoute(flow: AuthFlow, email: String? = null) =
    withEmail("${RouterConfig.AUTH_FLOW}/${flow.routeArg}", email)

private fun withEmail(route: String, email: String?) =
    if (email.isNullOrBlank()) route else "$route?${RouterConfig.EMAIL_ARG}=${Uri.encode(email)}"

private val emailArgument = navArgument(RouterConfig.EMAIL_ARG) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

// 页面之间左右推入的转场（类似 iOS 的 push）：新页面从右边完整滑入，下面的页面往左挪 1/4 并淡出
private const val PUSH_MILLIS = 350
private fun <T> pushSpec() = tween<T>(PUSH_MILLIS, easing = FastOutSlowInEasing)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    slideIntoContainer(SlideDirection.Start, pushSpec())

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    slideOutOfContainer(SlideDirection.Start, pushSpec(), targetOffset = { it / 4 }) + fadeOut(pushSpec())

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideIntoContainer(SlideDirection.End, pushSpec(), initialOffset = { it / 4 }) + fadeIn(pushSpec())

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutOfContainer(SlideDirection.End, pushSpec())

@Composable
fun AppNavigation(
    context: Context,
    externalAudioUri: Uri?,
    onExternalAudioConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val authState by sessionViewModel.authState.collectAsState()
    // 登录状态在 SessionManager 创建时已经同步从本地读好，第一帧就能决定去主页还是欢迎页，
    // 不再需要单独的启动页等待；打开 App 时看到的是系统启动页（样式在 themes.xml）
    val startDestination = remember {
        if (sessionViewModel.authState.value == AuthState.Authenticated) RouterConfig.MAINPAGE else RouterConfig.AUTH_GRAPH
    }

    LaunchedEffect(authState) {
        if (authState == AuthState.SessionExpired) {
            // 登录过期的老用户直接去登录页，不用再看欢迎页
            navController.navigate(loginRoute()) {
                popUpTo(RouterConfig.MAINPAGE) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            sessionViewModel.consumeSessionExpired()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        navigation(
            route = RouterConfig.AUTH_GRAPH,
            startDestination = RouterConfig.WELCOME
        ) {
            composable(
                route = RouterConfig.WELCOME,
                exitTransition = { pushExit() },
                popEnterTransition = { popEnter() }
            ) { entry ->
                WelcomeScreen(
                    onCreateAccount = { entry.ifResumed { navController.navigate(authFlowRoute(AuthFlow.REGISTER)) } },
                    onLogin = { entry.ifResumed { navController.navigate(loginRoute()) } }
                )
            }
            composable(
                route = LOGIN_ROUTE,
                arguments = listOf(emailArgument),
                enterTransition = { pushEnter() },
                // 登录成功进入主页时直接淡出，不做左右推
                exitTransition = {
                    if (targetState.destination.route == RouterConfig.MAINPAGE) fadeOut(pushSpec()) else pushExit()
                },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() }
            ) { entry ->
                // 登录过期时登录页是第一页，下面没有可以返回的页面，就不显示返回按钮
                val canGoBack = remember(entry) { navController.previousBackStackEntry != null }
                LoginRoute(
                    onBack = if (canGoBack) ({ entry.ifResumed { navController.popBackStack() } }) else null,
                    onLoggedIn = {
                        navController.navigate(RouterConfig.MAINPAGE) {
                            // 欢迎页、登录页、注册流程都在同一个导航图里，整个图一起清掉，登录后按返回键不会再回来
                            popUpTo(RouterConfig.AUTH_GRAPH) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onForgotPassword = { email ->
                        entry.ifResumed { navController.navigate(authFlowRoute(AuthFlow.RESET, email.takeIf { it.contains('@') })) }
                    },
                    onCreateAccount = { entry.ifResumed { navController.navigate(authFlowRoute(AuthFlow.REGISTER)) } }
                )
            }
            composable(
                route = AUTH_FLOW_ROUTE,
                arguments = listOf(
                    navArgument(RouterConfig.AUTH_FLOW_ARG) { type = NavType.StringType },
                    emailArgument
                ),
                enterTransition = { pushEnter() },
                exitTransition = { pushExit() },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() }
            ) { entry ->
                AuthFlowRoute(
                    onExit = { entry.ifResumed { navController.popBackStack() } },
                    onLogin = { entry.ifResumed { navController.backToLogin() } },
                    onFinished = { email -> entry.ifResumed { navController.openLoginWithEmail(email) } }
                )
            }
        }
        composable(RouterConfig.MAINPAGE) { navBackStackEntry ->
            val mainPageViewModel: MainPageViewModel = hiltViewModel(navBackStackEntry)
            MainPage(
                mainPageViewModel = mainPageViewModel,
                context = context,
                externalAudioUri = externalAudioUri,
                onExternalAudioConsumed = onExternalAudioConsumed,
                onClickLogout = {
                    sessionViewModel.logout()
                    // 退出登录回到欢迎页
                    navController.navigate(RouterConfig.AUTH_GRAPH) {
                        popUpTo(RouterConfig.MAINPAGE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

/** 页面正在转场（还没完全进入、或者正在离开）时不响应点击，避免连点导致重复跳转、多退一页 */
private inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

/** 注册流程第一步的「去登录」：登录页就在下面就直接返回，否则（从欢迎页进来的）把流程换成登录页 */
private fun NavController.backToLogin() {
    if (!popBackStack(LOGIN_ROUTE, inclusive = false)) {
        navigate(loginRoute()) {
            popUpTo(AUTH_FLOW_ROUTE) { inclusive = true }
        }
    }
}

/**
 * 注册成功 / 重置密码后回到登录页，并把邮箱填好。
 * 返回栈里已经有登录页（从登录页进来的）就把它一起换掉，返回栈始终是「欢迎页 → 登录页」。
 */
private fun NavController.openLoginWithEmail(email: String) {
    val loginInBackStack = runCatching { getBackStackEntry(LOGIN_ROUTE) }.isSuccess
    navigate(loginRoute(email)) {
        popUpTo(if (loginInBackStack) LOGIN_ROUTE else AUTH_FLOW_ROUTE) { inclusive = true }
    }
}
