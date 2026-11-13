package com.example.reelforge

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.videodownloader.VideoDownloaderEngine
import com.example.videodownloader.YoutubeVideoInfo
import kotlinx.coroutines.launch

private val Night = Color(0xFF080B16)
private val Panel = Color(0xFF131A2B)
private val Muted = Color(0xFF9AA7BD)
private val Violet = Color(0xFF8B5CF6)
private val Mint = Color(0xFF2DD4BF)

@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var youtubeInfo by remember { mutableStateOf<YoutubeVideoInfo?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = Violet.copy(alpha = 0.18f)) {
                    Text("⬇", modifier = Modifier.padding(13.dp), color = Color.White, fontSize = 25.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ALL DOWNLOADER", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("ONE LINK. ALL YOUR DOWNLOADS.", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("क्या डाउनलोड करना है?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("YouTube वीडियो लिंक या किसी फाइल का direct URL डालें।", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; youtubeInfo = null; error = ""; status = ""; progress = 0f },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://youtube.com/watch?v=...", color = Muted) },
                        label = { Text("लिंक पेस्ट करें", color = Muted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF39445D),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Mint
                        )
                    )
                    Button(
                        onClick = {
                            error = ""; status = ""; youtubeInfo = null; busy = true
                            scope.launch {
                                try {
                                    youtubeInfo = VideoDownloaderEngine.fetchYoutubeInfo(link.trim())
                                    status = "वीडियो मिल गया।"
                                } catch (e: Exception) {
                                    error = e.message ?: "वीडियो की जानकारी नहीं मिल सकी।"
                                } finally { busy = false }
                            }
                        },
                        enabled = link.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "वीडियो खोज रहे हैं..." else "▶  YOUTUBE VIDEO दिखाएँ", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val clean = link.trim()
                            if (!clean.startsWith("https://", true) && !clean.startsWith("http://", true)) {
                                error = "कृपया पूरा http:// या https:// लिंक डालें।"
                            } else if (VideoDownloaderEngine.youtubeVideoId(clean) != null) {
                                error = "YouTube पेज URL direct media file नहीं है। नीचे दिए विकल्प से YouTube में उपलब्ध आधिकारिक Download सुविधा खोलें।"
                            } else {
                                try {
                                    val id = VideoDownloaderEngine.enqueueDirectUrl(context, clean)
                                    status = "डाउनलोड कतार में जोड़ दिया गया। Android notification में प्रगति देखें। (ID: $id)"
                                    error = ""
                                } catch (e: Exception) { error = e.message ?: "डाउनलोड शुरू नहीं हो सका।" }
                            }
                        },
                        enabled = link.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B42))
                    ) { Text("⬇  DIRECT FILE / APK DOWNLOAD", color = Color.White, fontWeight = FontWeight.Bold) }

                    youtubeInfo?.let { info ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B42)),
                            shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(info.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Creator: ${info.author}", color = Muted, fontSize = 12.sp)
                                YouTubeEmbed(videoId = info.videoId)
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.watchUrl)))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Violet)
                                ) { Text("YouTube में आधिकारिक Download विकल्प देखें") }
                                Text("ध्यान दें: YouTube वीडियो का URL स्वयं डाउनलोड फाइल नहीं होता। ऐप केवल वैध direct media links डाउनलोड करता है; YouTube का offline विकल्प YouTube ऐप में उपलब्धता/अधिकार के अनुसार दिखेगा।",
                                    color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (error.isNotBlank()) Text(error, color = Color(0xFFFF8585), fontSize = 12.sp)
                    if (status.isNotBlank()) Text(status, color = Mint, fontSize = 12.sp)
                }
            }

            Text("DOWNLOAD ANYTHING", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val groups = listOf(
                Triple("🎬", "Video & Audio", "MP4 • MP3 • WEBM • direct media links"),
                Triple("📦", "Apps & APK", "Direct .apk files • app packages"),
                Triple("📄", "Documents", "PDF • DOCX • XLSX • PPTX • TXT"),
                Triple("🗂️", "Archives & Files", "ZIP • RAR • 7Z • images • other files")
            )
            groups.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.first, fontSize = 25.sp); Spacer(Modifier.width(13.dp))
                        Column {
                            Text(item.second, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.third, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF102B32))) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("SAVE LOCATIONS", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Videos: Movies/AllDownloader", color = Color.White, fontSize = 12.sp)
                    Text("Audio: Music/AllDownloader", color = Color.White, fontSize = 12.sp)
                    Text("APK, PDF, ZIP और बाकी फाइलें: Download/AllDownloader", color = Color.White, fontSize = 12.sp)
                }
            }
            Text("यह संस्करण अपना URL parser और metadata client इस्तेमाल करता है; NewPipe library नहीं। DRM या निजी सामग्री की सुरक्षा पाबंदियाँ bypass नहीं की जाती।",
                color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun YouTubeEmbed(videoId: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(210.dp),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com",
                    "<!doctype html><html><body style='margin:0;background:#000'><iframe " +
                        "width='100%' height='100%' src='https://www.youtube-nocookie.com/embed/$videoId' " +
                        "title='YouTube video player' frameborder='0' allow='accelerometer; autoplay; " +
                        "clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share' " +
                        "allowfullscreen></iframe></body></html>",
                    "text/html", "UTF-8", null
                )
            }
        },
        update = { }
    )
}
