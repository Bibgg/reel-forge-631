package com.example.reelforge

import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import kotlin.math.cos
import kotlin.math.sin

private val AppBgTop = Color(0xFF11113B)
private val AppBgBottom = Color(0xFF080B19)
private val AppCard = Color(0xE61A2140)
private val AppMuted = Color(0xFFBAC5E2)
private val FestivalPink = Color(0xFFFF477E)
private val FestivalBlue = Color(0xFF4CC9F0)
private val FestivalGreen = Color(0xFF42E695)
private val FestivalGold = Color(0xFFFFD166)
private val FestivalPurple = Color(0xFF9B72FF)

@Composable
fun DownloadScreen() {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<YoutubeSearchItem>>(emptyList()) }
    var selected by remember { mutableStateOf<YoutubeSearchItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("अपना पसंदीदा गाना, फिल्म या वीडियो खोजें।") }
    var showDownloadInfo by remember { mutableStateOf(false) }

    fun searchVideos(searchTerm: String = query) {
        if (searchTerm.isBlank() || loading) return
        query = searchTerm
        selected = null
        loading = true
        message = "YouTube पर खोज रहे हैं…"
        scope.launch {
            try {
                results = YoutubeSearchEngine.searchVideos(searchTerm)
                message = if (results.isEmpty()) "कोई वीडियो नहीं मिला। दूसरा नाम आज़माएँ।"
                    else "${results.size} वीडियो मिले • देखने के लिए किसी वीडियो को चुनें"
            } catch (error: Exception) {
                results = emptyList()
                message = error.message ?: "अभी वीडियो खोजे नहीं जा सके। इंटरनेट कनेक्शन जाँचें।"
            } finally {
                loading = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBgTop, Color(0xFF17113C), AppBgBottom)))
    ) {
        DiwaliFireworks(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .background(Brush.linearGradient(listOf(FestivalPink, FestivalPurple, FestivalBlue)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ALL", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(" DOWNLOADER", color = FestivalGold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Text("SEARCH • WATCH • SAVE", color = FestivalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
                Surface(
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FestivalGold.copy(alpha = 0.65f))
                ) {
                    Text("FESTIVE", color = FestivalGold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            if (selected == null) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Your entertainment", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("एक जगह खोजें और ऑनलाइन वीडियो देखें", color = AppMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE6222850)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.13f))
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Search songs, movies, videos…", color = AppMuted) },
                            leadingIcon = { Text("⌕", color = FestivalBlue, fontSize = 26.sp) },
                            trailingIcon = {
                                TextButton(enabled = query.isNotBlank() && !loading, onClick = { searchVideos() }) {
                                    Text("SEARCH", color = FestivalGold, fontWeight = FontWeight.ExtraBold)
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { searchVideos() }),
                            shape = RoundedCornerShape(17.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FestivalBlue,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = FestivalGold,
                                focusedContainerColor = Color(0xFF111A39),
                                unfocusedContainerColor = Color(0xFF111A39)
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickSearchChip("🎵 Music", FestivalPink, Modifier.weight(1f)) { searchVideos("Nepali music") }
                            QuickSearchChip("🎬 Movies", FestivalBlue, Modifier.weight(1f)) { searchVideos("popular movie trailers") }
                            QuickSearchChip("✨ Trending", FestivalGreen, Modifier.weight(1f)) { searchVideos("trending videos") }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = FestivalGold,
                        trackColor = Color.White.copy(alpha = 0.14f)
                    )
                }
                Text(message, color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))

                if (results.isEmpty() && !loading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = AppCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FestivalPurple.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 27.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎆", fontSize = 38.sp)
                            Text("Find something amazing", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("ऊपर Search में वीडियो का नाम लिखें और वीडियो यहाँ देखें.",
                                color = AppMuted, fontSize = 12.sp, lineHeight = 19.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                        contentPadding = PaddingValues(bottom = 22.dp)
                    ) {
                        items(results) { video ->
                            YouTubeResultRow(video = video, onClick = { selected = video; message = "" })
                        }
                    }
                }
            } else {
                val video = selected!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selected = null }) {
                        Text("← BACK TO VIDEOS", color = FestivalGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("NOW PLAYING", color = FestivalGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(5.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AppCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FestivalBlue.copy(alpha = 0.4f))
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        YouTubePlayer(video.url)
                        Text(video.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                        Text(video.uploader, color = AppMuted, fontSize = 13.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = FestivalGreen.copy(alpha = 0.16f), shape = RoundedCornerShape(30.dp)) {
                                Text("● ONLINE PLAYER", color = FestivalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { showDownloadInfo = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FestivalPink)
                            ) { Text("⬇ DOWNLOAD", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(message, color = AppMuted, fontSize = 12.sp)
            }
        }
    }

    if (showDownloadInfo) {
        AlertDialog(
            onDismissRequest = { showDownloadInfo = false },
            containerColor = Color(0xFF1A2140),
            title = { Text("Download options", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "YouTube के हर वीडियो को third-party app में डाउनलोड करने की अनुमति नहीं होती। इस बटन से केवल वहाँ डाउनलोड उपलब्ध होगा जहाँ वीडियो का मालिक/स्रोत इसकी अनुमति देता है। YouTube की आधिकारिक offline सुविधा YouTube ऐप में उपलब्ध हो सकती है।",
                    color = AppMuted
                )
            },
            confirmButton = {
                TextButton(onClick = { showDownloadInfo = false }) {
                    Text("समझ गया", color = FestivalGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun QuickSearchChip(label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = tint.copy(alpha = 0.15f),
        shape = RoundedCornerShape(13.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.48f))
    ) {
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 10.dp), maxLines = 1)
    }
}

@Composable
private fun YouTubeResultRow(video: YoutubeSearchItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = AppCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.width(135.dp).height(88.dp).clip(RoundedCornerShape(13.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(video.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 18.sp)
                Text(video.uploader, color = AppMuted, fontSize = 11.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("▶ ", color = FestivalGreen, fontSize = 10.sp)
                    Text(
                        if (video.durationSeconds > 0) "${video.durationSeconds / 60}:${(video.durationSeconds % 60).toString().padStart(2, '0')}"
                        else "YouTube video",
                        color = FestivalGold, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
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
        modifier = Modifier.fillMaxWidth().height(225.dp).clip(RoundedCornerShape(16.dp)),
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

@Composable
private fun DiwaliFireworks(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "diwali-fireworks")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "firework-phase"
    )
    Canvas(modifier = modifier) {
        val bursts = listOf(
            Triple(Offset(size.width * 0.12f, size.height * 0.12f), 0f, FestivalPink),
            Triple(Offset(size.width * 0.88f, size.height * 0.18f), 0.34f, FestivalGreen),
            Triple(Offset(size.width * 0.78f, size.height * 0.52f), 0.68f, FestivalBlue)
        )
        bursts.forEach { (center, offset, baseColor) ->
            val p = (phase + offset) % 1f
            val radius = 15f + 55f * p
            val alpha = (1f - p) * 0.42f
            for (ray in 0 until 18) {
                val angle = (Math.PI * 2.0 * ray / 18.0).toFloat()
                val dx = cos(angle)
                val dy = sin(angle)
                val start = center + Offset(dx * radius * 0.28f, dy * radius * 0.28f)
                val end = center + Offset(dx * radius, dy * radius)
                val color = when (ray % 4) {
                    0 -> baseColor
                    1 -> FestivalGold
                    2 -> FestivalPurple
                    else -> Color.White
                }.copy(alpha = alpha)
                drawLine(color, start, end, strokeWidth = 2.2f, cap = StrokeCap.Round)
                drawCircle(color, radius = 2.1f, center = end)
            }
            drawCircle(
                color = baseColor.copy(alpha = alpha * 0.45f),
                radius = radius * 0.48f,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }
        val stars = listOf(
            Offset(size.width * 0.48f, size.height * 0.10f),
            Offset(size.width * 0.92f, size.height * 0.38f),
            Offset(size.width * 0.08f, size.height * 0.62f),
            Offset(size.width * 0.56f, size.height * 0.72f)
        )
        stars.forEachIndexed { index, point ->
            val twinkle = if ((phase * 4f + index) % 1f > 0.5f) 0.8f else 0.25f
            drawCircle(FestivalGold.copy(alpha = twinkle), radius = 2.2f, center = point)
        }
    }
}
