package com.example.videodownloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DirectFileInfo(
    val finalUrl: String,
    val fileName: String,
    val extension: String,
    val mimeType: String
)

/**
 * First-party direct HTTP file detection. It follows normal redirects and checks
 * headers/content type; it does not defeat authentication, DRM, paywalls or access controls.
 */
object OwnLinkAnalyzer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun inspect(rawUrl: String): DirectFileInfo? {
        val url = rawUrl.trim()
        val head = Request.Builder().url(url)
            .head()
            .header("User-Agent", "ALL-DOWNLOADER/1.0 (Android)")
            .build()
        val response = try {
            client.newCall(head).execute()
        } catch (_: Exception) {
            return null
        }

        response.use { result ->
            if (!result.isSuccessful) return null
            return buildInfo(
                result.request.url.toString(),
                result.header("Content-Type"),
                result.header("Content-Disposition"),
                result.header("Content-Length")
            )
        }
    }

    private fun buildInfo(
        finalUrl: String,
        rawMime: String?,
        disposition: String?,
        contentLength: String?
    ): DirectFileInfo? {
        val mime = rawMime?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
            ?: "application/octet-stream"
        if (mime in setOf("text/html", "application/xhtml+xml", "application/json")) return null

        val dispositionName = disposition?.let { header ->
            Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
                .find(header)?.groupValues?.getOrNull(1)?.let {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                } ?: Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE)
                    .find(header)?.groupValues?.getOrNull(1)
        }
        val urlName = finalUrl.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val name = (dispositionName ?: urlName).ifBlank { "download" }
            .filter { it.isLetterOrDigit() || it in "._- ()" }
            .take(150).ifBlank { "download" }
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            .ifBlank { extensionForMime(mime) }
        val knownFileMime = mime.startsWith("video/") || mime.startsWith("audio/") ||
            mime.startsWith("image/") || mime in setOf(
                "application/pdf", "application/zip",
                "application/vnd.android.package-archive",
                "application/octet-stream", "application/x-7z-compressed"
            )
        val hasFileName = name.contains('.') || !dispositionName.isNullOrBlank()
        val hasLength = (contentLength?.toLongOrNull() ?: 0L) > 0L
        if (!knownFileMime && !hasFileName && !hasLength) return null
        return DirectFileInfo(finalUrl, name, ext, mime)
    }

    private fun extensionForMime(mime: String): String = when {
        mime == "application/pdf" -> "pdf"
        mime == "application/zip" -> "zip"
        mime == "application/vnd.android.package-archive" -> "apk"
        mime.startsWith("audio/") -> "audio"
        mime.startsWith("video/") -> "video"
        mime.startsWith("image/") -> "image"
        else -> "bin"
    }
}
