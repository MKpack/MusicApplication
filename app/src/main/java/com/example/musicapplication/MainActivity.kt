package com.example.musicapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapplication.ui.appNaviagtion.AppNavigation
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import com.example.musicapplication.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mContext: Context = this
    private var externalAudioUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        externalAudioUri = parseExternalAudioUri(intent)

        setContent {

            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themePreset by themeViewModel.themePreset.collectAsStateWithLifecycle()

            MusicApplicationTheme(
                themePreset = themePreset
            ) {
                AppNavigation(
                    context = mContext,
                    externalAudioUri = externalAudioUri,
                    onExternalAudioConsumed = {
                        externalAudioUri = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalAudioUri = parseExternalAudioUri(intent)
    }

    private fun parseExternalAudioUri(intent: Intent): Uri? {
        if (intent.action != Intent.ACTION_VIEW) return null

        val uri = intent.data ?: return null
        val type = intent.type

        if (type != null && !type.startsWith("audio/")) {
            return null
        }

        return uri
    }
}
