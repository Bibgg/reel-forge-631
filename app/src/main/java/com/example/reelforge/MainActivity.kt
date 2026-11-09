package com.example.reelforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReelForgeApp()
                }
            }
        }
    }
}

@Composable
fun ReelForgeApp() {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var videos by remember { mutableStateOf(VideoStore.list(context)) }
    var playing by remember { mutableStateOf<File?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("⬆") },
                    label = { Text("अपलोड") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("▶") },
                    label = { Text("वीडियो") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Text("☰") },
                    label = { Text("फ़ाइलें") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> UploadScreen(onUploaded = {
                    videos = VideoStore.list(context)
                    tab = 1
                })
                1 -> VideosScreen(videos, onPlay = { playing = it })
                else -> FilesScreen(videos, onPlay = { playing = it })
            }
        }
    }

    playing?.let { file ->
        PlayerDialog(file = file, onClose = { playing = null })
    }
}
