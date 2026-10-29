package com.example.musicapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.musicapplication.ui.appNaviagtion.AppNavigation
import com.example.musicapplication.ui.theme.MusicApplicationTheme
import com.example.musicapplication.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    }

    private val mContext: Context = this
    private var externalAudioUri by mutableStateOf<Uri?>(null)
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpSplashScreen()
        enableEdgeToEdge()

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        requestNotificationPermissionIfNeeded()

        externalAudioUri = parseExternalAudioUri(intent)

        setContent {

            val themePreset by themeViewModel.themePreset.collectAsStateWithLifecycle()
            val backgroundImage by themeViewModel.backgroundImage.collectAsStateWithLifecycle()

            MusicApplicationTheme(
                themePreset = themePreset,
                backgroundImage = backgroundImage
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

    /**
     * 系统启动页（Android 12+，样式在 themes.xml 里）：主题色从 DataStore 读出来之前一直显示，
     * 避免先用默认主题色画一帧再变色，最多等 1 秒。
     * 退场用系统默认的淡出：自己接管退场动画时，交接的那一下会先露出一帧页面，再被启动页盖回来，看起来像闪了一下。
     */
    private fun setUpSplashScreen() {
        val content: View = findViewById(android.R.id.content)
        val start = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = themeViewModel.isLoaded.value || SystemClock.uptimeMillis() - start > 1000
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATIONS_REQUEST_CODE
            )
        }
    }
}
