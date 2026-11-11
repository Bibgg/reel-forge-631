package com.example.reelforge

import android.widget.Toast
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("वीडियो डाउनलोडर", style = MaterialTheme.typography.headlineMedium)
        Text(
            "वीडियो लिंक पेस्ट करें। ऐप उपलब्ध स्ट्रीम जाँचेगा और डाउनलोड विकल्प दिखाएगा।",
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
                                Toast.makeText(context, "डाउनलोड शुरू हुआ। Downloads फ़ोल्डर देखें।", Toast.LENGTH_LONG).show()
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

        Text("Extractor से उपलब्ध सेवाएँ", style = MaterialTheme.typography.titleMedium)
        Text("YouTube • SoundCloud • PeerTube • Bandcamp • media.ccc.de")
        Text("Instagram • TikTok • Facebook • X/Twitter: लिंक पहचाने जाते हैं, लेकिन NewPipeExtractor इन्हें सपोर्ट नहीं करता। इनके लिए अलग अधिकृत integration चाहिए।")
        Text(
            "केवल अपने वीडियो या ऐसे वीडियो डाउनलोड करें जिन्हें सेव करने की अनुमति हो। निजी सामग्री या प्लेटफॉर्म की सुरक्षा-पाबंदियों को bypass नहीं किया जाता।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
