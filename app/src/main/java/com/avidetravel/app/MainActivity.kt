package com.avidetravel.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.avidetravel.app.ui.AvideApp
import com.avidetravel.app.ui.theme.AvideTravelTheme

class MainActivity : ComponentActivity() {
    private var pendingGoogleAuthCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleAuthIntent(intent)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            AvideTravelTheme {
                AvideApp(
                    googleAuthCode = pendingGoogleAuthCode,
                    onGoogleAuthConsumed = { pendingGoogleAuthCode = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "avidetravel" && data.host == "auth") {
            pendingGoogleAuthCode = data.getQueryParameter("code")
        }
    }
}
