package com.nocta.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nocta.player.ui.NoctaRoot

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            NoctaRoot(
                vm = viewModel,
                onRequestPermission = { ensurePermissions() }
            )
        }

        ensurePermissions()
    }

    private fun ensurePermissions() {
        val required = requiredPermissions()
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            viewModel.onPermissionsChanged()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requiredPermissions(): List<String> {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.READ_MEDIA_AUDIO
            list += Manifest.permission.POST_NOTIFICATIONS
        } else {
            list += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return list
    }
}
