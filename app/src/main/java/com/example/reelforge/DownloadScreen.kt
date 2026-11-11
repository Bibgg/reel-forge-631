package com.example.reelforge

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.VideoDownloaderEngine
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
    fun isAppStoreLink(value: String): Boolean {
        val host = Uri.parse(value).host?.lowercase().orEmpty()
        return host == "play.google.com" || host.endsWith(".play.google.com")
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("क्या डाउनलोड करना है?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("वीडियो, म्यूज़िक, ऐप फाइल, PDF, ZIP, फोटो और दूसरी फाइलों का लिंक यहाँ डालें।", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it; error = ""; status = ""; progress = 0f },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/file.apk", color = Muted) },
                        label = { Text("लिंक पेस्ट करें", color = Muted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = Color(0xFF39445D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Mint
                        )
                    )
                    Button(
                        onClick = {
                            val clean = link.trim()
                            if (!clean.startsWith("https://", true) && !clean.startsWith("http://", true)) {
                                error = "कृपया पूरा http:// या https:// लिंक डालें।"
                            } else if (isAppStoreLink(clean)) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
                                    status = "यह ऐप-स्टोर लिंक है। इसे आधिकारिक Play Store में खोला गया है।"
                                } catch (_: Exception) {
                                    error = "Play Store लिंक नहीं खुल सका।"
                                }
                            } else {
                                try {
                                    val id = VideoDownloaderEngine.enqueueDirectUrl(context, clean)
                                    status = "डाउनलोड कतार में जोड़ दिया गया। Android notification में प्रगति देखें। (ID: $id)"
                                    error = ""
                                } catch (e: Exception) {
                                    error = e.message ?: "डाउनलोड शुरू नहीं हो सका।"
                                }
                            }
                        },
                        enabled = link.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet)
                    ) {
                        Text("⚡  DIRECT FILE / APK DOWNLOAD", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    Button(
                        onClick = {
                            busy = true
                            progress = 0f
                            status = "मीडिया लिंक जाँचा जा रहा है..."
                            error = ""
                            scope.launch {
                                try {
                                    val receipt = VideoDownloaderEngine.downloadWithYtDlp(context, link.trim()) { percent, _, line ->
                                        progress = percent.coerceIn(0f, 100f)
                                        if (line.isNotBlank()) status = line
                                    }
                                    progress = 100f
                                    status = "पूरा हुआ: ${receipt.fileName} • ${receipt.savedLocation}"
                                } catch (e: Exception) {
                                    error = e.cause?.message ?: e.message ?: "यह मीडिया लिंक डाउनलोड नहीं हो सका।"
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = link.isNotBlank() && !busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202B42))
                    ) {
                        if (busy) CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "MEDIA DOWNLOAD RUNNING..." else "▶  VIDEO / AUDIO SMART EXTRACT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (busy || progress > 0f) {
                        LinearProgressIndicator(
                            progress = { (progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = Mint,
                            trackColor = Color(0xFF35415A)
                        )
                    }
                    if (error.isNotBlank()) Text(error, color = Color(0xFFFF8585), fontSize = 12.sp)
                    if (status.isNotBlank()) Text(status, color = Mint, fontSize = 12.sp)
                }
            }

            Text("DOWNLOAD ANYTHING", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            val groups = listOf(
                Triple("🎬", "Video & Audio", "MP4 • MP3 • WEBM • supported media links"),
                Triple("📦", "Apps & APK", "Direct .apk files • app packages"),
                Triple("📄", "Documents", "PDF • DOCX • XLSX • PPTX • TXT"),
                Triple("🗂️", "Archives & Files", "ZIP • RAR • 7Z • images • other files")
            )
            groups.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.first, fontSize = 25.sp)
                        Spacer(Modifier.width(13.dp))
                        Column {
                            Text(item.second, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.third, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102B32))
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("SAVE LOCATIONS", color = Mint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Videos: Movies/AllDownloader", color = Color.White, fontSize = 12.sp)
                    Text("Audio: Music/AllDownloader", color = Color.White, fontSize = 12.sp)
                    Text("APK, PDF, ZIP और बाकी फाइलें: Download/AllDownloader", color = Color.White, fontSize = 12.sp)
                }
            }

            Text(
                "नोट: सामान्य फाइल और APK के लिए सीधा डाउनलोड लिंक चाहिए। Play Store लिंक आधिकारिक स्टोर में खुलता है। DRM, निजी या सुरक्षा-प्रतिबंधित सामग्री को bypass नहीं किया जाता।",
                color = Muted,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
