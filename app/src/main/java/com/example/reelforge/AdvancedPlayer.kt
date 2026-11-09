package com.example.reelforge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun ReferenceStylePlayer(file: File, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            playWhenReady = true
        }
    }
    var playing by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var fitMode by remember { mutableStateOf("Fit") }
    var quality by remember { mutableStateOf("Auto") }
    var menu by remember { mutableStateOf("") }

    LaunchedEffect(player) {
        while (true) {
            if (!dragging) position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0 } ?: 0L
            playing = player.isPlaying
            delay(400)
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF05070B))
                .clickable { if (!locked) controlsVisible = !controlsVisible },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.resizeMode = if (fitMode == "Fill") AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize()
            )

            if (controlsVisible && !locked) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0x55000000))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", color = Color.White, fontSize = 34.sp,
                            modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.nameWithoutExtension, color = Color.White,
                                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("स्थानीय वीडियो", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Text("Help", color = Color.White,
                            modifier = Modifier.clickable { menu = "help" }.padding(8.dp))
                        Text("⚙", color = Color.White, fontSize = 23.sp,
                            modifier = Modifier.clickable { menu = "settings" }.padding(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("▣  Tap to Lock", color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.clickable { locked = true }.padding(8.dp))
                        Spacer(Modifier.weight(1f))
                        Text("↶\n10", color = Color.White, fontSize = 17.sp, lineHeight = 17.sp,
                            modifier = Modifier.clickable { player.seekBack() }.padding(12.dp))
                        Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 38.sp,
                            modifier = Modifier.clickable {
                                if (player.isPlaying) player.pause() else player.play()
                                playing = player.isPlaying
                            }.padding(horizontal = 18.dp))
                        Text("↷\n10", color = Color.White, fontSize = 17.sp, lineHeight = 17.sp,
                            modifier = Modifier.clickable { player.seekForward() }.padding(12.dp))
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatTime(position), color = Color.White, fontSize = 12.sp)
                            Slider(
                                value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f,
                                onValueChange = { dragging = true; position = it.toLong() },
                                onValueChangeFinished = { player.seekTo(position); dragging = false },
                                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fitMode, color = Color.White, modifier = Modifier.clickable {
                                fitMode = if (fitMode == "Fit") "Fill" else "Fit"
                            }.padding(6.dp))
                            Text("▤ Language", color = Color.White,
                                modifier = Modifier.clickable { menu = "language" }.padding(6.dp))
                            Text("${speed}x", color = Color.White,
                                modifier = Modifier.clickable { menu = "speed" }.padding(6.dp))
                            Text(quality, color = Color.White,
                                modifier = Modifier.clickable { menu = "quality" }.padding(6.dp))
                        }
                    }
                }
            }

            if (locked) {
                Text("🔒  Locked · Tap to Unlock", color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterStart).clickable { locked = false }.padding(18.dp))
                Text("🔓", color = Color.White, fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.TopEnd).clickable { locked = false }.padding(20.dp))
            }

            when (menu) {
                "speed" -> PlayerMenu("Playback speed",
                    listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), {
                        speed = it.removeSuffix("x").toFloat()
                        player.setPlaybackSpeed(speed)
                        menu = ""
                    }) { menu = "" }
                "quality" -> PlayerMenu("Video quality", listOf("Auto", "480P", "720P", "1080P"), {
                    quality = it
                    val builder = player.trackSelectionParameters.buildUpon()
                    when (it) {
                        "480P" -> builder.setMaxVideoSize(854, 480)
                        "720P" -> builder.setMaxVideoSize(1280, 720)
                        "1080P" -> builder.setMaxVideoSize(1920, 1080)
                        else -> builder.clearVideoSizeConstraints()
                    }
                    player.trackSelectionParameters = builder.build()
                    menu = ""
                }) { menu = "" }
                "language" -> {
                    val languages = remember(player.currentTracks) {
                        buildList {
                            add("Default")
                            player.currentTracks.groups.forEach { group ->
                                if (group.type == C.TRACK_TYPE_AUDIO) {
                                    for (i in 0 until group.length) {
                                        val lang = group.getTrackFormat(i).language
                                        if (!lang.isNullOrBlank() && !contains(lang)) add(lang)
                                    }
                                }
                            }
                        }
                    }
                    PlayerMenu("Audio language", languages, {
                        val lang = if (it == "Default") null else it
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon().setPreferredAudioLanguage(lang).build()
                        menu = ""
                    }) { menu = "" }
                }
                "settings", "help" -> PlayerMenu(
                    if (menu == "help") "Player help" else "Settings",
                    if (menu == "help") listOf("Tap screen to show controls", "Use lock to avoid touches")
                    else listOf("Playback speed", "Quality", "Language"),
                    { menu = "" }
                ) { menu = "" }
            }
        }
    }
}

@Composable
private fun PlayerMenu(title: String, options: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(min = 190.dp, max = 280.dp)
            .background(Color(0xFF20242B)).padding(14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp))
            options.forEach { option ->
                Text(option, color = Color.White, modifier = Modifier.fillMaxWidth()
                    .clickable { onSelect(option) }.padding(vertical = 10.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
