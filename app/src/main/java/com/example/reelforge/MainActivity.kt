package com.example.reelforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = darkColorScheme(
                primary = Color(0xFF8B5CF6),
                secondary = Color(0xFF2DD4BF),
                background = Color(0xFF080B16),
                surface = Color(0xFF131A2B),
                onPrimary = Color.White,
                onSecondary = Color(0xFF07131A),
                onBackground = Color.White,
                onSurface = Color.White
            )
            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    DownloadScreen()
                }
            }
        }
    }
}
