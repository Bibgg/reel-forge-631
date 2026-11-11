package com.example.videodownloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

/**
 * NewPipe Extractor integration plus Android DownloadManager.
 * Upstream currently supports YouTube, SoundCloud, media.ccc.de, PeerTube and Bandcamp.
 * Instagram, TikTok, Facebook and X are detected but not scraped or bypassed here.
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
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .addRequestHeader("User-Agent", USER_AGENT)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
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
