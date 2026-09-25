package com.avidetravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.avidetravel.app.ui.AvideApp
import com.avidetravel.app.ui.theme.AvideTravelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvideTravelTheme {
                AvideApp()
            }
        }
    }
}
