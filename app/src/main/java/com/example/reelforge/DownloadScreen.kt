package com.example.reelforge

import android.app.DownloadManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.VideoDownloaderEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Night = Color(0xFF080D1C)
private val Panel = Color(0xFF151F38)
private val Cyan = Color(0xFF4CC9F0)
private val Lime = Color(0xFF52E6A5)
private val Gold = Color(0xFFFFD166)
private val Pink = Color(0xFFFF5C8A)
private val Muted = Color(0xFFB8C6DF)

@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("लिंक पेस्ट करें और डाउनलोड शुरू करें।") }
    var details by remember { mutableStateOf("वीडियो • ऑडियो • फोटो • PDF • ZIP • APK • दस्तावेज़") }
    var showInfo by remember { mutableStateOf(false) }

    fun startDownload() {
        val url = link.trim()
        if (busy) return
        if (!(url.startsWith("https://", true) || url.startsWith("http://", true))) {
            status = "कृपया पूरा http:// या https:// लिंक डालें।"
            return
        }
        busy = true
        progress = 0f
        status = "लिंक जाँचा जा रहा है…"
        details = "स्रोत और उपलब्ध फ़ाइल प्रकार पहचाने जा रहे हैं"
        scope.launch {
            try {
                val inspected = try {
                    VideoDownloaderEngine.inspect(url)
                } catch (_: Exception) {
                    null
                }

                if (inspected?.platform == com.example.videodownloader.SourcePlatform.YOUTUBE) {
                    throw IllegalStateException(
                        "YouTube लिंक पहचाना गया। इस ऐप में third-party YouTube डाउनलोड उपलब्ध नहीं है; YouTube की आधिकारिक Offline सुविधा इस्तेमाल करें।"
                    )
                }

                if (!inspected?.options.isNullOrEmpty()) {
                    val option = inspected!!.options.first()
                    status = "फ़ाइल Download Manager को भेजी जा रही है…"
                    val id = withContext(Dispatchers.IO) {
                        VideoDownloaderEngine.enqueueDirectUrl(context, option.url)
                    }
                    details = "डाउनलोड ID: $id"
                    status = "डाउनलोड शुरू हो गया। Android notification में प्रगति देखें।"
                    Toast.makeText(context, "डाउनलोड शुरू हो गया", Toast.LENGTH_SHORT).show()
                } else {
                    status = "वेबसाइट का समर्थित मीडिया स्रोत खोज रहे हैं…"
                    val receipt = VideoDownloaderEngine.downloadWithPythonWorker(context, url) { value, _, line ->
                        progress = value.coerceIn(0f, 1f)
                        if (line.isNotBlank()) status = line.take(150)
                    }
                    progress = 1f
                    details = "फ़ाइल: ${receipt.fileName}\nस्थान: ${receipt.savedLocation}"
                    status = "डाउनलोड पूरा हुआ!"
                    Toast.makeText(context, "डाउनलोड पूरा हुआ", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                status = error.message ?: "डाउनलोड नहीं हो सका। दूसरा लिंक आज़माएँ।"
                details = "कुछ वेबसाइटें डाउनलोड की अनुमति नहीं देतीं या समर्थित नहीं होतीं।"
            } finally {
                busy = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF111A39), Night, Color(0xFF10152A)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Pink.copy(alpha = 0.18f)
                ) {
                    Text("⬇", color = Pink, fontSize = 30.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ALL DOWNLOADER", color = Color.White, fontSize = 23.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                    Text("ONE APP • MANY FILE TYPES", color = Cyan, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                TextButton(onClick = { showInfo = true }) {
                    Text("INFO", color = Gold, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("सब कुछ एक जगह डाउनलोड करें", color = Color.White,
                    fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 31.sp)
                Text("किसी समर्थित वेबसाइट का लिंक या सीधा फ़ाइल URL पेस्ट करें।",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(alpha = 0.35f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("DOWNLOAD LINK", color = Gold, fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        placeholder = { Text("https://example.com/file.mp4", color = Muted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = Muted.copy(alpha = 0.45f),
                            cursorColor = Gold
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { startDownload() })
                    )
                    Button(
                        onClick = { startDownload() },
                        enabled = link.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Pink)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Text("⬇  ", fontSize = 18.sp)
                        }
                        Text(if (busy) "PLEASE WAIT…" else "START DOWNLOAD",
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SUPPORTED FILE TYPES", color = Color.White,
                            fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("MULTI-FILE", color = Lime, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    val types = listOf(
                        "🎬 Video" to Cyan, "🎵 Audio" to Pink,
                        "🖼 Images" to Gold, "📄 PDF / Docs" to Lime,
                        "📦 ZIP / RAR" to Color(0xFFB69CFF), "📱 APK / Files" to Cyan
                    )
                    types.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (label, tint) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = tint.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.3f))
                                ) {
                                    Text(label, color = Color.White, fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (busy) "DOWNLOAD IN PROGRESS" else "DOWNLOAD STATUS",
                        color = Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(status, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (busy) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Lime,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                    }
                    Text(details, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }

            Text(
                "नोट: डाउनलोड केवल सार्वजनिक/अनुमत और समर्थित स्रोतों से होगा। DRM, लॉगिन या निजी सामग्री की सुरक्षा को बायपास नहीं किया जाता।",
                color = Muted.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 17.sp
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            containerColor = Panel,
            title = { Text("ALL DOWNLOADER", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "यह ऐप सीधे वीडियो, ऑडियो, फोटो, PDF, दस्तावेज़, archive और APK जैसे फ़ाइल लिंक डाउनलोड कर सकता है। कुछ सार्वजनिक वेबसाइटों के मीडिया के लिए yt-dlp extractor इस्तेमाल होता है। हर वेबसाइट समर्थित नहीं होती। YouTube के लिए आधिकारिक Offline विकल्प इस्तेमाल करें।",
                    color = Muted
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("ठीक है", color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
