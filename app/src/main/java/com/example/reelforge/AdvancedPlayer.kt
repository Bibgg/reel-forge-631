package com.example.reelforge

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

@Composable
fun ReferenceStylePlayer(file: File, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val originalOrientation = remember { activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
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
    var autoRotate by remember { mutableStateOf(true) }
    var rotationLabel by remember { mutableStateOf("Auto") }
    var zoom by remember { mutableFloatStateOf(1f) }
    var gestureHint by remember { mutableStateOf("") }

    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

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

    fun rotateTo(label: String) {
        rotationLabel = label
        autoRotate = label == "Auto"
        activity?.requestedOrientation = when (label) {
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "Reverse landscape" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "Reverse portrait" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "Lock current" -> if (context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
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
                    view.resizeMode = when (fitMode) {
                        "Fill" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        "Stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
            )

            // Double tap left/right to seek; single tap toggles controls.
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(locked) {
                        detectTapGestures(
                            onTap = { if (!locked) controlsVisible = !controlsVisible },
                            onDoubleTap = { offset ->
                                if (!locked) {
                                    if (offset.x < size.width / 2f) player.seekBack() else player.seekForward()
                                    gestureHint = if (offset.x < size.width / 2f) "⏪ 10 seconds" else "10 seconds ⏩"
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            zoom = (zoom * zoomChange).coerceIn(1f, 3f)
                        }
                    }
                    .pointerInput(locked) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                if (!locked) {
                                    totalDrag += dragAmount
                                    val x = change.position.x
                                    if (x < size.width * 0.42f) {
                                        val attrs = activity?.window?.attributes
                                        if (attrs != null) {
                                            val current = if (attrs.screenBrightness < 0f) 0.5f else attrs.screenBrightness
                                            attrs.screenBrightness = (current - dragAmount / size.height).coerceIn(0.03f, 1f)
                                            activity.window.attributes = attrs
                                            gestureHint = "☀ Brightness"
                                        }
                                    } else if (x > size.width * 0.58f) {
                                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val next = (current - (dragAmount / size.height * max * 2f).toInt()).coerceIn(0, max)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                        gestureHint = "🔊 Volume"
                                    }
                                    change.consume()
                                }
                            },
                            onDragEnd = { }
                        )
                    }
            )

            if (gestureHint.isNotBlank()) {
                Text(gestureHint, color = Color.White, fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center).background(Color(0x99000000), RoundedCornerShape(12.dp)).padding(14.dp))
                LaunchedEffect(gestureHint) {
                    delay(900)
                    gestureHint = ""
                }
            }

            if (controlsVisible && !locked) {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0x55000000))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", color = Color.White, fontSize = 34.sp,
                            modifier = Modifier.clickable { onClose() }.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(file.nameWithoutExtension, color = Color.White, fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("ReelForge Player", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Text("↻", color = Color.White, fontSize = 25.sp,
                            modifier = Modifier.clickable { menu = "rotation" }.padding(8.dp))
                        Text("⚙", color = Color.White, fontSize = 23.sp,
                            modifier = Modifier.clickable { menu = "settings" }.padding(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(if (locked) "🔒 Unlock" else "🔓 Lock", color = Color.White, fontSize = 13.sp,
                            modifier = Modifier.clickable { locked = !locked }.padding(8.dp))
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(fitMode, color = Color.White, modifier = Modifier.clickable {
                                fitMode = when (fitMode) { "Fit" -> "Fill"; "Fill" -> "Stretch"; else -> "Fit" }
                            }.padding(6.dp))
                            Text("Zoom ${(zoom * 100).toInt()}%", color = Color.White,
                                modifier = Modifier.clickable { zoom = if (zoom >= 2f) 1f else zoom + 0.25f }.padding(6.dp))
                            Text("Language", color = Color.White, modifier = Modifier.clickable { menu = "language" }.padding(6.dp))
                            Text("${speed}x", color = Color.White, modifier = Modifier.clickable { menu = "speed" }.padding(6.dp))
                            Text(quality, color = Color.White, modifier = Modifier.clickable { menu = "quality" }.padding(6.dp))
                        }
                    }
                }
            }

            if (locked) {
                Text("🔒 Controls locked · Tap unlock", color = Color.White, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.TopCenter).clickable { locked = false }
                        .background(Color(0x99000000), RoundedCornerShape(20.dp)).padding(12.dp))
            }

            when (menu) {
                "rotation" -> PlayerMenu("Screen rotation", listOf("Auto", "Portrait", "Landscape",
                    "Reverse landscape", "Reverse portrait", "Lock current"), {
                    rotateTo(it); menu = ""
                }) { menu = "" }
                "speed" -> PlayerMenu("Playback speed", listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"), {
                    speed = it.removeSuffix("x").toFloat()
                    player.setPlaybackSpeed(speed); menu = ""
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
                    player.trackSelectionParameters = builder.build(); menu = ""
                }) { menu = "" }
                "language" -> {
                    val languages = remember(player.currentTracks) {
                        buildList<String> {
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
                "settings" -> PlayerMenu("Player settings",
                    listOf("Auto rotation: ${if (autoRotate) "On" else "Off"}", "Fit / Fill / Stretch", "Pinch to zoom"),
                    {
                        if (it.startsWith("Auto rotation")) rotateTo(if (autoRotate) "Lock current" else "Auto")
                        menu = ""
                    }) { menu = "" }
            }
        }
    }
}

@Composable
private fun PlayerMenu(title: String, options: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0x77000000)).clickable { onDismiss() },
        contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(min = 210.dp, max = 300.dp)
            .background(Color(0xFF20242B), RoundedCornerShape(16.dp)).padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp))
            options.forEach { option ->
                Text(option, color = Color.White, modifier = Modifier.fillMaxWidth()
                    .clickable { onSelect(option) }.padding(vertical = 11.dp))
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
