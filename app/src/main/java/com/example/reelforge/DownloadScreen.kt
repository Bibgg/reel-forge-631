package com.example.reelforge

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.videodownloader.YoutubeSearchEngine
import com.example.videodownloader.YoutubeSearchItem
import kotlinx.coroutines.launch

private val AppBg = Color(0xFF080B16)
private val AppCard = Color(0xFF151D30)
private val AppAccent = Color(0xFF8B5CF6)
private val AppMuted = Color(0xFF9AA7BD)

@Composable
fun DownloadScreen() {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<YoutubeSearchItem>>(emptyList()) }
    var selected by remember { mutableStateOf<YoutubeSearchItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("YouTube पर गाने, वीडियो और चैनल खोजें।") }

    Surface(Modifier.fillMaxSize(), color = AppBg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ALL", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text(" DOWNLOADER", color = AppAccent, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Text("YouTube • Search and Watch", color = AppMuted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search YouTube videos", color = AppMuted) },
                trailingIcon = {
                    TextButton(
                        enabled = query.isNotBlank() && !loading,
                        onClick = {
                            selected = null
                            loading = true
                            message = "YouTube पर खोज रहे हैं…"
                            scope.launch {
                                try {
                                    results = YoutubeSearchEngine.searchVideos(query)
                                    message = if (results.isEmpty()) "कोई वीडियो नहीं मिला।" else "${results.size} वीडियो मिले।"
                                } catch (e: Exception) {
                                    results = emptyList()
                                    message = e.message ?: "YouTube search अभी उपलब्ध नहीं है।"
                                } finally { loading = false }
                            }
                        }
                    ) { Text("SEARCH", color = AppAccent, fontWeight = FontWeight.Bold) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent, unfocusedBorderColor = Color(0xFF39445D),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    cursorColor = AppAccent
                )
            )
            Spacer(Modifier.height(10.dp))

            selected?.let { video ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selected = null }) { Text("← Search results", color = AppAccent) }
                    Spacer(Modifier.weight(1f))
                }
                Text(video.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(video.uploader, color = AppMuted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                YouTubePlayer(video.url)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { message = "इस वीडियो के लिए डाउनलोड तभी उपलब्ध होगा जब स्रोत/अधिकार अनुमति दें। YouTube का आधिकारिक offline विकल्प YouTube ऐप में देखें।" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) { Text("⬇  DOWNLOAD (जहाँ अनुमति हो)") }
                Text(message, color = AppMuted, fontSize = 12.sp)
            } ?: run {
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppAccent)
                }
                Text(message, color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(results) { video ->
                        YouTubeResultRow(video = video, onClick = { selected = video; message = "" })
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeResultRow(video: YoutubeSearchItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.width(132.dp).height(82.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(video.uploader, color = AppMuted, fontSize = 12.sp, maxLines = 1)
                Text(
                    if (video.durationSeconds > 0) "▶ ${video.durationSeconds / 60}:${(video.durationSeconds % 60).toString().padStart(2, '0')}" else "YouTube video",
                    color = AppMuted, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun YouTubePlayer(videoUrl: String) {
    val videoId = remember(videoUrl) {
        Uri.parse(videoUrl).getQueryParameter("v")
            ?: Uri.parse(videoUrl).lastPathSegment.orEmpty()
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    "<html><body style='margin:0;background:#000'><iframe width='100%' height='100%' " +
                        "src='https://www.youtube-nocookie.com/embed/$videoId' frameborder='0' " +
                        "allow='accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture; web-share' " +
                        "allowfullscreen></iframe></body></html>",
                    "text/html", "UTF-8", null
                )
            }
        },
        update = { }
    )
}
