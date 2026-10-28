package br.com.lottan.nativeapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lottan.nativeapp.ui.*

class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<android.net.Uri?>(null)
    @Suppress("DEPRECATION")
    private fun acceptIntent(value: android.content.Intent?) {
        val uri = when (value?.action) {
            android.content.Intent.ACTION_VIEW -> value.data
            android.content.Intent.ACTION_SEND -> value.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri?.scheme == "content") incoming = uri
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); setIntent(intent); acceptIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) acceptIntent(intent)
        enableEdgeToEdge()
        setContent {
            val vm: LibraryViewModel = viewModel()
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val dark = prefs.theme in listOf("dark", "graphite") || (prefs.theme == "system" && isSystemInDarkTheme())
            SideEffect {
                enableEdgeToEdge(
                    if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                window.decorView.setBackgroundColor(VisualTokens.palette(dark).background.toArgb())
            }
            // Animations run only when the app option is on and Android has not disabled animations system-wide.
            val systemAnimations = android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            LottanTheme(dark, prefs.theme == "graphite", prefs.textSize / 18f, prefs.highContrast, prefs.animations && systemAnimations) { LottanApp(vm, incoming) { incoming = null } }
        }
    }
}
