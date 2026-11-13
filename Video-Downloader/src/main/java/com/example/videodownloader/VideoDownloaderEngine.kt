package com.example.videodownloader

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SourcePlatform(val displayName: String) {
    YOUTUBE("YouTube"), INSTAGRAM("Instagram"), TIKTOK("TikTok"),
    FACEBOOK("Facebook"), X("X / Twitter"), SOUNDCLOUD("SoundCloud"),
    PEERTUBE("PeerTube"), BANDCAMP("Bandcamp"), MEDIA_CCC("media.ccc.de"),
    DIRECT("Direct media link"), UNKNOWN("Unknown site")
}

data class MediaOption(
    val label: String,
    val url: String,
    val extension: String,
    val mimeType: String
)

data class ExtractionResult(
    val title: String,
    val platform: SourcePlatform,
    val options: List<MediaOption>
)

data class DownloadReceipt(val fileName: String, val savedLocation: String)

data class YoutubeVideoInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val watchUrl: String
)

/** First-party URL analysis and metadata, Android DownloadManager for direct files. */
object VideoDownloaderEngine {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * First-party YouTube URL parser + public oEmbed metadata client.
     * No NewPipe code, extractor, or NewPipe dependency is used.
     */
    fun youtubeVideoId(rawUrl: String): String? {
        return try {
            val uri = Uri.parse(rawUrl.trim())
            val host = uri.host?.lowercase(Locale.ROOT) ?: return null
            val validHost = host == "youtu.be" || host == "youtube.com" ||
                host.endsWith(".youtube.com")
            if (!validHost) return null
            val id = when {
                host == "youtu.be" -> uri.lastPathSegment
                uri.path == "/watch" -> uri.getQueryParameter("v")
                uri.pathSegments.firstOrNull() in setOf("shorts", "embed", "live") ->
                    uri.pathSegments.getOrNull(1)
                else -> null
            } ?: return null
            id.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
        } catch (_: Exception) { null }
    }

    suspend fun fetchYoutubeInfo(rawUrl: String): YoutubeVideoInfo =
        withContext(Dispatchers.IO) {
            val id = youtubeVideoId(rawUrl)
                ?: throw IllegalArgumentException("यह सही YouTube वीडियो लिंक नहीं है।")
            val watchUrl = "https://www.youtube.com/watch?v=$id"
            val endpoint = "https://www.youtube.com/oembed?url=" +
                Uri.encode(watchUrl) + "&format=json"
            val request = okhttp3.Request.Builder().url(endpoint)
                .header("User-Agent", USER_AGENT).get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("YouTube से वीडियो की जानकारी नहीं मिली (HTTP ${response.code})।")
                }
                val json = JSONObject(response.body?.string().orEmpty())
                YoutubeVideoInfo(
                    videoId = id,
                    title = json.optString("title", "YouTube video"),
                    author = json.optString("author_name", "YouTube creator"),
                    thumbnailUrl = json.optString("thumbnail_url",
                        "https://i.ytimg.com/vi/$id/hqdefault.jpg"),
                    watchUrl = watchUrl
                )
            }
        }

    suspend fun inspect(rawUrl: String): ExtractionResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "कृपया पूरा https:// लिंक डालिए।"
        }
        val platform = detectPlatform(url)
        val directFile = OwnLinkAnalyzer.inspect(url)
        if (directFile != null) {
            return@withContext ExtractionResult(
                directFile.fileName.substringBeforeLast('.', "Direct file"),
                platform ?: SourcePlatform.DIRECT,
                listOf(MediaOption("Direct download • ${directFile.mimeType}",
                    directFile.finalUrl, directFile.extension, directFile.mimeType))
            )
        }
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        if (extension in setOf("mp4", "m4v", "webm", "mov", "mkv", "mp3", "m4a", "aac", "ogg")) {
            return@withContext ExtractionResult("Direct media file",
                platform ?: SourcePlatform.DIRECT,
                listOf(MediaOption("Direct download • .$extension", url, extension,
                    if (extension == "mp3") "audio/mpeg" else "video/mp4")))
        }
        if (platform == SourcePlatform.YOUTUBE) {
            val info = fetchYoutubeInfo(url)
            return@withContext ExtractionResult(info.title, SourcePlatform.YOUTUBE, emptyList())
        }
        throw UnsupportedOperationException(
            "इस लिंक के लिए अपना extractor अभी उपलब्ध नहीं है। Direct media file URL डालें।"
        )
    }

    /**
     * Broad site support through yt-dlp. Output is first written to app storage,
     * then published into the public Movies/AllDownloader folder so Gallery can index it.
     */
    /**
     * Embedded Python/yt-dlp worker. The Android library hosts the runtime internally;
     * this is not an interactive shell and does not expose arbitrary terminal commands.
     */
    suspend fun downloadWithPythonWorker(
        context: Context,
        rawUrl: String,
        onProgress: (Float, Long, String) -> Unit
    ): DownloadReceipt = downloadWithYtDlp(context, rawUrl, onProgress)

    suspend fun downloadWithYtDlp(
        context: Context,
        rawUrl: String,
        onProgress: (Float, Long, String) -> Unit
    ): DownloadReceipt = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "कृपया पूरा https:// लिंक डालिए।"
        }

        require(youtubeVideoId(url) == null) { "YouTube page URLs are not direct media files; use YouTube official offline options." }

        YtDlp.init(context.applicationContext)
        val outputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "ReelForgeDownloads"
        ).apply { mkdirs() }

        val uniquePrefix = "ReelForge_" + System.currentTimeMillis() + "_"
        val outputTemplate = File(outputDir, uniquePrefix + "%(id)s.%(ext)s").absolutePath
        // Retry transient network/fragment failures, and use progressively broader
        // format selectors. This improves resilience but cannot override source restrictions.
        val formatFallbacks = listOf(
            "best[height<=1080][ext=mp4]/best[height<=1080]/best",
            "best[height<=1080]/best",
            "best"
        )
        var lastExitCode = -1
        var succeeded = false
        for ((index, format) in formatFallbacks.withIndex()) {
            val request = YtDlpRequest(url)
                .setOutputTemplate(outputTemplate)
                .addOption("--no-playlist")
                .addOption("--no-mtime")
                .addOption("--retries", "5")
                .addOption("--fragment-retries", "5")
                .addOption("--extractor-retries", "3")
                .addOption("--file-access-retries", "3")
                .addOption("--retry-sleep", "2")
                .addOption("--socket-timeout", "30")
                .addOption("--concurrent-fragments", "4")
                .addOption("-f", format)

            onProgress(0f, 0L, "डाउनलोड तरीका ${index + 1}/${formatFallbacks.size} आज़माया जा रहा है…")
            val response = YtDlp.executeAsync(request) { progress, eta, line ->
                onProgress(progress, eta, line)
            }.get()
            if (response.isSuccess) {
                succeeded = true
                break
            }
            lastExitCode = response.exitCode
            if (index < formatFallbacks.lastIndex) {
                onProgress(0f, 0L, "एक तरीका काम नहीं आया; दूसरे उपलब्ध format से कोशिश कर रहे हैं…")
                Thread.sleep(1200L)
            }
        }
        if (!succeeded) {
            throw IllegalStateException(
                "सभी सुरक्षित डाउनलोड कोशिशें असफल रहीं (exit code $lastExitCode)। लिंक expired, login-required, unsupported या source-restricted हो सकता है।"
            )
        }

        val downloaded = outputDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(uniquePrefix) }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("डाउनलोड पूरा हुआ, लेकिन वीडियो फ़ाइल नहीं मिली।")

        val location = copyIntoPublicMedia(context, downloaded)
        DownloadReceipt(downloaded.name, location)
    }

    private fun copyIntoPublicMedia(context: Context, source: File): String {
        val extension = source.extension.lowercase(Locale.ROOT)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "video/mp4"
        val isAudio = mimeType.startsWith("audio/")
        val folder = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val legacyDir = File(
                Environment.getExternalStoragePublicDirectory(folder),
                "AllDownloader"
            ).apply { mkdirs() }
            val destination = File(legacyDir, source.name)
            source.copyTo(destination, overwrite = true)
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(destination.absolutePath), arrayOf(mimeType), null
            )
            source.delete()
            return "$folder/AllDownloader"
        }

        val collection = if (isAudio) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "$folder/AllDownloader"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("मोबाइल मीडिया फ़ोल्डर में जगह नहीं बन पाई।")

        try {
            val output = resolver.openOutputStream(uri, "w")
                ?: throw IllegalStateException("मोबाइल मीडिया फ़ोल्डर में फ़ाइल नहीं लिखी जा सकी।")
            output.use { destination ->
                source.inputStream().use { input -> input.copyTo(destination) }
            }
            val ready = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, ready, null, null)
            source.delete()
            return "$folder/AllDownloader"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun enqueue(context: Context, title: String, option: MediaOption): Long {
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim().take(70).ifBlank { "AllDownloader-Video" }
        val safeExtension = option.extension.lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }.ifBlank { "mp4" }
        val fileName = "$safeTitle.$safeExtension"

        val request = DownloadManager.Request(Uri.parse(option.url))
            .setTitle(fileName)
            .setDescription("ALL DOWNLOADER media download")
            .setMimeType(option.mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "VideoDownloader/$fileName"
            )
            .addRequestHeader("User-Agent", USER_AGENT)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    /**
     * Downloads a direct HTTP(S) file URL of any ordinary file type.
     * Videos/audio go to Gallery-visible media folders; documents, archives,
     * images, APKs and other files go to Downloads/AllDownloader.
     */
    fun enqueueDirectUrl(context: Context, rawUrl: String): Long {
        val parsed = Uri.parse(rawUrl.trim())
        val scheme = parsed.scheme?.lowercase(Locale.ROOT)
        require(scheme == "https" || scheme == "http") {
            "सुरक्षित http:// या https:// फाइल लिंक डालिए।"
        }
        val host = parsed.host
        require(!host.isNullOrBlank()) { "लिंक सही नहीं है।" }
        val pathName = parsed.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
        val fileName = sanitizeFileName(pathName ?: "download_${System.currentTimeMillis()}")
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "apk" -> "application/vnd.android.package-archive"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "rar" -> "application/vnd.rar"
                "7z" -> "application/x-7z-compressed"
                else -> "application/octet-stream"
            }
        val folder = when {
            mime.startsWith("video/") -> Environment.DIRECTORY_MOVIES
            mime.startsWith("audio/") -> Environment.DIRECTORY_MUSIC
            else -> Environment.DIRECTORY_DOWNLOADS
        }
        val relativeFolder = if (folder == Environment.DIRECTORY_DOWNLOADS) {
            "AllDownloader"
        } else {
            "AllDownloader"
        }
        val request = DownloadManager.Request(parsed)
            .setTitle(fileName)
            .setDescription("ALL DOWNLOADER • $host")
            .setMimeType(mime)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(folder, "$relativeFolder/$fileName")
            .addRequestHeader("User-Agent", USER_AGENT)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    private fun sanitizeFileName(value: String): String {
        val cleaned = value.substringBefore('?').substringBefore('#')
            .filter { it.isLetterOrDigit() || it in "._- ()" }
            .trim().trim('.')
            .replace(' ', '_')
        return cleaned.take(150).ifBlank { "download_${System.currentTimeMillis()}" }
    }

    fun detectPlatform(rawUrl: String): SourcePlatform? {
        val host = Uri.parse(rawUrl).host?.lowercase(Locale.ROOT) ?: return null
        fun matches(domain: String) = host == domain || host.endsWith(".$domain")
        return when {
            matches("youtube.com") || matches("youtu.be") -> SourcePlatform.YOUTUBE
            matches("instagram.com") -> SourcePlatform.INSTAGRAM
            matches("tiktok.com") -> SourcePlatform.TIKTOK
            matches("facebook.com") || matches("fb.watch") -> SourcePlatform.FACEBOOK
            matches("x.com") || matches("twitter.com") -> SourcePlatform.X
            matches("soundcloud.com") -> SourcePlatform.SOUNDCLOUD
            matches("peertube.tv") -> SourcePlatform.PEERTUBE
            matches("bandcamp.com") -> SourcePlatform.BANDCAMP
            matches("media.ccc.de") -> SourcePlatform.MEDIA_CCC
            else -> null
        }
    }

}
