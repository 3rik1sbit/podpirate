package com.podpirate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.podpirate.ui.PodPirateNavHost
import com.podpirate.ui.theme.PodPirateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodPirateTheme {
                PodPirateNavHost()
            }
        }
    }
}
