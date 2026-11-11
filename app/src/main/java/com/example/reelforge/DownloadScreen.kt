package com.example.reelforge

import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.videodownloader.ExtractionResult
import com.example.videodownloader.VideoDownloaderEngine
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ExtractionResult?>(null) }
    var error by remember { mutableStateOf("") }
    var ytdlpLoading by remember { mutableStateOf(false) }
    var ytdlpProgress by remember { mutableFloatStateOf(0f) }
    var ytdlpMessage by remember { mutableStateOf("") }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("वीडियो डाउनलोडर", style = MaterialTheme.typography.headlineMedium)
        Text(
            "वीडियो लिंक पेस्ट करें। डाउनलोड पूरा होने पर वीडियो मोबाइल के Movies/VideoDownloader फ़ोल्डर और Gallery में दिखेगी।",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = link,
            onValueChange = { link = it; error = ""; result = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("वीडियो लिंक") },
            placeholder = { Text("https://...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Button(
            onClick = {
                loading = true
                error = ""
                result = null
                scope.launch {
                    try {
                        result = VideoDownloaderEngine.inspect(link)
                    } catch (e: Exception) {
                        error = e.message ?: "लिंक जाँचा नहीं जा सका।"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = link.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.width(10.dp))
            }
            Text(if (loading) "जाँच हो रही है..." else "लिंक जाँचें")
        }


        Button(
            onClick = {
                ytdlpLoading = true
                ytdlpProgress = 0f
                ytdlpMessage = "डाउनलोड शुरू हो रहा है..."
                scope.launch {
                    try {
                        val receipt = VideoDownloaderEngine.downloadWithYtDlp(context, link) { percent, eta, line ->
                            mainHandler.post {
                                ytdlpProgress = percent.coerceIn(0f, 100f)
                                if (line.isNotBlank()) ytdlpMessage = line
                            }
                        }
                        ytdlpProgress = 100f
                        ytdlpMessage = "सफल: ${receipt.fileName} — ${receipt.savedLocation}"
                    } catch (e: Exception) {
                        ytdlpMessage = e.cause?.message ?: e.message ?: "डाउनलोड पूरा नहीं हुआ।"
                    } finally {
                        ytdlpLoading = false
                    }
                }
            },
            enabled = link.isNotBlank() && !ytdlpLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ytdlpLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.width(10.dp))
            }
            Text(if (ytdlpLoading) "yt-dlp डाउनलोड चल रहा है..." else "yt-dlp से डाउनलोड करें")
        }
        if (ytdlpLoading || ytdlpMessage.isNotBlank()) {
            LinearProgressIndicator(
                progress = { (ytdlpProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(ytdlpMessage, style = MaterialTheme.typography.bodySmall)
        }

        if (error.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp))
            }
        }

        result?.let { media ->
            Text(media.title, style = MaterialTheme.typography.titleLarge)
            Text("स्रोत: ${media.platform.displayName}")
            Text("डाउनलोड विकल्प: ${media.options.size}")
            media.options.forEach { option ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.label, style = MaterialTheme.typography.titleMedium)
                            Text(".${option.extension}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            try {
                                VideoDownloaderEngine.enqueue(context, media.title, option)
                                Toast.makeText(context, "डाउनलोड शुरू हुआ। Gallery में Movies/VideoDownloader देखें।", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "डाउनलोड शुरू नहीं हुआ।", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Text("डाउनलोड")
                        }
                    }
                }
            }
        }

        Text("NewPipe Extractor से उपलब्ध सेवाएँ", style = MaterialTheme.typography.titleMedium)
        Text("yt-dlp engine: 1000+ sites का support; सभी साइटों पर हर लिंक चलने की गारंटी नहीं है।")
        Text("YouTube • SoundCloud • PeerTube • Bandcamp • media.ccc.de")
        Text("Instagram • TikTok • Facebook • X/Twitter: yt-dlp engine से डाउनलोड की कोशिश की जाती है। कुछ लिंक पर platform की login, cookies या network restrictions के कारण डाउनलोड असफल हो सकता है।")
        Text(
            "केवल अपने वीडियो या ऐसे वीडियो डाउनलोड करें जिन्हें सेव करने की अनुमति हो। निजी सामग्री या प्लेटफॉर्म की सुरक्षा-पाबंदियों को bypass नहीं किया जाता।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
