package com.example

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ui.GamepadViewModel
import com.example.ui.PadPhoneApp
import com.example.ui.theme.PadPhoneTheme

class MainActivity : ComponentActivity() {

    private val gamepadViewModel: GamepadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applySystemBarsForCurrentOrientation()

        setContent {
            PadPhoneTheme {
                PadPhoneApp(
                    viewModel = gamepadViewModel,
                    onLandscapeLockChanged = { locked ->
                        requestedOrientation = if (locked) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                        updateImmersiveBars(immersive = locked || isLandscape())
                    },
                )
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateImmersiveBars(
            immersive = gamepadViewModel.uiState.value.landscapeLocked ||
                newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE,
        )
    }

    private fun isLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun applySystemBarsForCurrentOrientation() {
        updateImmersiveBars(immersive = isLandscape())
    }

    private fun updateImmersiveBars(immersive: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
