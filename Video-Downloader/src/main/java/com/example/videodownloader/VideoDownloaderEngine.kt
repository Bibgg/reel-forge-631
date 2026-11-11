package com.example.videodownloader

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest
import okhttp3.OkHttpClient
import okhttp3.RequestBody
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

/**
 * NewPipe Extractor for its native supported services, yt-dlp for broad multi-site
 * downloads, and Android DownloadManager for progressive NewPipe streams.
 */
object VideoDownloaderEngine {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36"

    @Volatile private var initialized = false
    private val lock = Any()

    private fun initializeExtractor() {
        if (initialized) return
        synchronized(lock) {
            if (!initialized) {
                NewPipe.init(ExtractorHttpDownloader())
                initialized = true
            }
        }
    }

    suspend fun inspect(rawUrl: String): ExtractionResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "कृपया पूरा https:// लिंक डालिए।"
        }

        val platform = detectPlatform(url)
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT).orEmpty()

        if (extension in setOf("mp4", "m4v", "webm", "mov", "mkv", "mp3", "m4a", "aac", "ogg")) {
            return@withContext ExtractionResult(
                "Direct media file", platform ?: SourcePlatform.DIRECT,
                listOf(MediaOption("Direct download • .$extension", url, extension,
                    when (extension) {
                        "mp3" -> "audio/mpeg"
                        "m4a", "aac" -> "audio/mp4"
                        "webm" -> "video/webm"
                        else -> "video/mp4"
                    }))
            )
        }

        val source = platform ?: SourcePlatform.UNKNOWN
        if (source in setOf(SourcePlatform.INSTAGRAM, SourcePlatform.TIKTOK,
                SourcePlatform.FACEBOOK, SourcePlatform.X)) {
            throw UnsupportedOperationException(
                "${source.displayName} लिंक पहचाना गया, लेकिन NewPipeExtractor इसे सपोर्ट नहीं करता। " +
                    "इस प्लेटफॉर्म के लिए आधिकारिक/अनुमत integration अभी बाकी है; login या सुरक्षा-पाबंदी bypass नहीं किया जाएगा।"
            )
        }

        initializeExtractor()
        val service = try {
            NewPipe.getServiceByUrl(url)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "यह साइट NewPipeExtractor की supported list में नहीं है। Supported: YouTube, SoundCloud, PeerTube, Bandcamp और media.ccc.de.", e
            )
        }

        val extractor = service.getStreamExtractor(url)
        extractor.fetchPage()
        val options = extractor.getVideoStreams()
            .filter { it.getDeliveryMethod() == DeliveryMethod.PROGRESSIVE_HTTP }
            .filter { it.getContent().isNotBlank() }
            .map { stream ->
                val format = stream.getFormat()
                val suffix = format?.getSuffix()?.takeIf { it.isNotBlank() } ?: "mp4"
                val formatName = format?.getName()?.takeIf { it.isNotBlank() } ?: suffix.uppercase()
                val resolution = stream.getResolution().takeIf { it.isNotBlank() }
                MediaOption(
                    listOfNotNull(formatName, resolution).joinToString(" • "),
                    stream.getContent(), suffix, format?.getMimeType() ?: "video/mp4"
                )
            }
            .distinctBy { it.url }

        if (options.isEmpty()) {
            throw IllegalStateException(
                "वीडियो स्ट्रीम नहीं मिली। हो सकता है यह private, live, restricted या अभी unsupported format हो।"
            )
        }
        ExtractionResult(extractor.getName().ifBlank { "Video" }, source, options)
    }


    /**
     * Broad site support through yt-dlp. Output is first written to app storage,
     * then published into the public Movies/VideoDownloader folder so Gallery can index it.
     */
    suspend fun downloadWithYtDlp(
        context: Context,
        rawUrl: String,
        onProgress: (Float, Long, String) -> Unit
    ): DownloadReceipt = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        require(url.startsWith("https://", true) || url.startsWith("http://", true)) {
            "कृपया पूरा https:// लिंक डालिए।"
        }

        YtDlp.init(context.applicationContext)
        val outputDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "ReelForgeDownloads"
        ).apply { mkdirs() }

        val uniquePrefix = "ReelForge_" + System.currentTimeMillis() + "_"
        val outputTemplate = File(outputDir, uniquePrefix + "%(id)s.%(ext)s").absolutePath
        val request = YtDlpRequest(url)
            .setOutputTemplate(outputTemplate)
            .addOption("--no-playlist")
            .addOption("--no-mtime")
            .addOption("-f", "best[height<=1080][ext=mp4]/best[height<=1080]/best")

        val future = YtDlp.executeAsync(request) { progress, eta, line ->
            onProgress(progress, eta, line)
        }
        val response = future.get()
        if (!response.isSuccess) {
            throw IllegalStateException("yt-dlp डाउनलोड विफल हुआ (exit code ${response.exitCode})")
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
                "VideoDownloader"
            ).apply { mkdirs() }
            val destination = File(legacyDir, source.name)
            source.copyTo(destination, overwrite = true)
            android.media.MediaScannerConnection.scanFile(
                context, arrayOf(destination.absolutePath), arrayOf(mimeType), null
            )
            source.delete()
            return "$folder/VideoDownloader"
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
                "$folder/VideoDownloader"
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
            return "$folder/VideoDownloader"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun enqueue(context: Context, title: String, option: MediaOption): Long {
        val safeTitle = title.replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim().take(70).ifBlank { "ReelForge-Video" }
        val safeExtension = option.extension.lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }.ifBlank { "mp4" }
        val fileName = "$safeTitle.$safeExtension"

        val request = DownloadManager.Request(Uri.parse(option.url))
            .setTitle(fileName)
            .setDescription("ReelForge video download")
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
            .replace(Regex("[\\\\/:*?\\"<>|\\\\p{Cntrl}]"), "_")
            .trim().trim('.')
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

    private class ExtractorHttpDownloader : Downloader() {
        private val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true).build()

        override fun execute(request: Request): Response {
            val method = request.httpMethod()
            val data = request.dataToSend()
            val body: RequestBody? = when (method.uppercase(Locale.ROOT)) {
                "GET", "HEAD" -> null
                else -> (data ?: ByteArray(0)).toRequestBody()
            }
            val builder = okhttp3.Request.Builder()
                .url(request.url()).method(method, body).header("User-Agent", USER_AGENT)
            request.headers().forEach { (name, values) ->
                builder.removeHeader(name)
                values.forEach { value -> builder.addHeader(name, value) }
            }
            client.newCall(builder.build()).execute().use { response ->
                return Response(
                    response.code, response.message, response.headers.toMultimap(),
                    response.body?.string().orEmpty(), response.request.url.toString()
                )
            }
        }
    }
}
