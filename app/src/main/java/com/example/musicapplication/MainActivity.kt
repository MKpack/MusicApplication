package com.example.musicapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.internal.rememberComposableLambdaN
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicapplication.config.RouterConfig
import com.example.musicapplication.ui.login.LoginEntry
import com.example.musicapplication.ui.login.LoginViewModel
import com.example.musicapplication.ui.mainPage.MainPage
import com.example.musicapplication.ui.mainPage.MainPageViewModel
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mContext: Context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicApplicationTheme {
                AppNavigation(this)
            }
        }
    }
}


@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable(RouterConfig.LOGIN) { navBackStackEntry ->
            val loginViewModel: LoginViewModel = hiltViewModel(navBackStackEntry)
            LoginEntry(loginViewModel, context, navController)
        }
        composable(RouterConfig.MAINPAGE) { navBackStackEntry ->
            val mainPageViewModel: MainPageViewModel = hiltViewModel(navBackStackEntry)
            MainPage(mainPageViewModel)
        }
    }
}