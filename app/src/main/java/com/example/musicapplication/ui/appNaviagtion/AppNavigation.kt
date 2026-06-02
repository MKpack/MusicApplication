package com.example.musicapplication.ui.appNaviagtion

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.data.session.AuthState
import com.example.musicapplication.ui.session.SessionViewModel
import com.example.musicapplication.ui.login.LoginEntry
import com.example.musicapplication.ui.login.LoginViewModel
import com.example.musicapplication.ui.mainPage.MainPage
import com.example.musicapplication.ui.mainPage.MainPageViewModel
import com.example.musicapplication.ui.splash.SplashScreen


@Composable
fun AppNavigation(
    context: Context,
    externalAudioUri: Uri?,
    onExternalAudioConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val authState by sessionViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState == AuthState.SessionExpired) {
            navController.navigate(RouterConfig.LOGIN) {
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
        startDestination = RouterConfig.SPLASH
    ) {
        composable(RouterConfig.SPLASH) {
            SplashScreen(
                navController = navController
            )
        }
        composable(RouterConfig.LOGIN + "?redirect={redirect}") { navBackStackEntry ->
            val loginViewModel: LoginViewModel = hiltViewModel(navBackStackEntry)
            LoginEntry(loginViewModel, context, navController)
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
                    navController.navigate(RouterConfig.LOGIN) {
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
